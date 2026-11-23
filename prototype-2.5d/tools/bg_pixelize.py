"""Turn an AI background painting into a pixel-art arena layer.

python tools/bg_pixelize.py SRC OUT --size 720x380 [--ramps Langit,Putih] [--fit cover|width]
                            [--anchor top|center|bottom] [--key ff00ff] [--dither 20] [--preview P]

- scales the image (area average) so it covers the target size and crops it at the anchor
- --key: pixels close to this colour (the magenta chroma key) become transparent
- maps every pixel to the game palette (palette2.py), optionally only some ramps, with an ordered
  (Bayer 4x4) dither so soft gradients become flat bands with a thin mixed strip, like the sprites
- writes OUT and, for opaque layers, nothing else; run make_normals.py afterwards if the layer is lit
"""
import argparse
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
from palette2 import RAMPS  # noqa: E402

BAYER = np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]], float) / 16 - 0.5


def palette(ramps):
    cols = []
    for name, _, hexes in RAMPS:
        if ramps and name not in ramps:
            continue
        cols += [[int(h[i:i + 2], 16) for i in (0, 2, 4)] for h in hexes]
    return np.array(cols, float)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("src")
    ap.add_argument("out")
    ap.add_argument("--size", default="720x380")
    ap.add_argument("--ramps", default="")
    ap.add_argument("--fit", default="cover", choices=["cover", "width"])
    ap.add_argument("--anchor", default="center", choices=["top", "center", "bottom"])
    ap.add_argument("--top", type=int, default=None, help="put the scaled image's top edge at this y")
    ap.add_argument("--key", default="")
    ap.add_argument("--key-tol", type=float, default=110)
    ap.add_argument("--key-spread", type=float, default=0,
                    help="also clear lavender/pink pixels touching the key whose red and blue exceed green by this")
    ap.add_argument("--dither", type=float, default=20)
    ap.add_argument("--preview")
    a = ap.parse_args()

    W, H = map(int, a.size.lower().split("x"))
    src = Image.open(a.src).convert("RGB")
    sw, sh = src.size
    k = W / sw if a.fit == "width" else max(W / sw, H / sh)
    nw, nh = max(W, round(sw * k)), max(1, round(sh * k))
    img = np.asarray(src.resize((nw, nh), Image.BOX), float)
    x0 = (nw - W) // 2
    img = img[:, x0:x0 + W]
    alpha = np.full((H, W), 255, np.uint8)
    if a.top is not None:
        # place the scaled image with its top edge at y = top (may be negative); outside = transparent
        canvas = np.zeros((H, W, 3))
        s0, d0 = max(0, -a.top), max(0, a.top)
        n = min(nh - s0, H - d0)
        canvas[d0:d0 + n] = img[s0:s0 + n]
        alpha[:] = 0
        alpha[d0:d0 + n] = 255
        img = canvas
    else:
        y0 = {"top": 0, "center": (nh - H) // 2, "bottom": nh - H}[a.anchor] if nh >= H else 0
        img = img[y0:y0 + H]
        if img.shape[0] < H:                    # strip shorter than the layer: sit on the bottom
            pad = np.zeros((H - img.shape[0], W, 3))
            alpha[:H - img.shape[0]] = 0
            img = np.concatenate([pad, img], 0)
    if a.key:
        key = np.array([int(a.key[i:i + 2], 16) for i in (0, 2, 4)], float)
        dist = np.sqrt(((img - key) ** 2).sum(2))
        # magenta-ish: strong red + blue, weak green also counts (AI keys are never exact)
        magentaish = (img[..., 0] > 150) & (img[..., 2] > 150) & (img[..., 1] < 120)
        keyed = (dist < a.key_tol) | magentaish
        if a.key_spread > 0:
            # AI keys bleed: mist or moonlight on top of the magenta turns it lavender/pink. Grow the
            # transparent area into neighbouring pixels whose colour still leans to magenta (red and
            # blue both above green), so no halo is left around the scenery.
            from scipy import ndimage
            r, g, b = img[..., 0], img[..., 1], img[..., 2]
            lean = (r - g > a.key_spread) & (b - g > a.key_spread)
            lab, _ = ndimage.label(keyed | lean)
            keep = np.unique(lab[keyed])
            keyed = np.isin(lab, keep[keep > 0])
        alpha[keyed] = 0

    pal = palette({r.strip() for r in a.ramps.split(",") if r.strip()})
    th = np.tile(BAYER, (H // 4 + 1, W // 4 + 1))[:H, :W][..., None] * a.dither
    flat = (img + th).reshape(-1, 3)
    idx = np.empty(len(flat), int)
    for s in range(0, len(flat), 65536):         # chunks keep memory small
        d = ((flat[s:s + 65536, None, :] - pal[None]) ** 2).sum(2)
        idx[s:s + 65536] = d.argmin(1)
    rgb = pal[idx].reshape(H, W, 3).astype(np.uint8)
    out = np.dstack([rgb, alpha])
    out[alpha == 0] = 0
    Image.fromarray(out, "RGBA").save(a.out)
    used = len(np.unique(rgb[alpha > 0].reshape(-1, 3), axis=0)) if (alpha > 0).any() else 0
    print(f"{a.out}: {W}x{H}, {used} colours, {100 * (alpha == 0).mean():.0f}% transparent")
    if a.preview:
        big = Image.fromarray(out, "RGBA").resize((W * 2, H * 2), Image.NEAREST)
        bg = Image.new("RGBA", big.size, (255, 0, 255, 255))
        bg.alpha_composite(big)
        bg.save(a.preview)


if __name__ == "__main__":
    main()
