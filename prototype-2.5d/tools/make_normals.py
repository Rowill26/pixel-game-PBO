"""Automatic normal maps for pixel-art sprites (placeholder quality; Laigter does it better).

The height of each pixel = distance to the sprite edge (a soft "pillow") + a little brightness
detail. Normals are taken from the height slope and encoded as RGB = normal * 0.5 + 0.5,
with +Y pointing UP on screen (the convention used by the LibGDX lighting shader).

Usage:  python tools/make_normals.py path/to/sprite.png [strength]
        -> writes path/to/sprite_n.png
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image


def normal_map(img, strength=2.0, max_dist=5):
    a = np.asarray(img.convert("RGBA"), dtype=np.float32)
    mask = a[..., 3] > 0
    dist = np.zeros(mask.shape, np.float32)
    cur = mask.copy()
    for _ in range(max_dist):
        nxt = cur.copy()
        nxt[1:, :] &= cur[:-1, :]
        nxt[:-1, :] &= cur[1:, :]
        nxt[:, 1:] &= cur[:, :-1]
        nxt[:, :-1] &= cur[:, 1:]
        cur = nxt
        dist += cur
    lum = (a[..., 0] * 0.299 + a[..., 1] * 0.587 + a[..., 2] * 0.114) / 255.0
    h = dist / max_dist + lum * 0.35
    h = np.where(mask, h, 0.0)
    p = np.pad(h, 1, mode="edge")
    h = (p[:-2, 1:-1] + p[2:, 1:-1] + p[1:-1, :-2] + p[1:-1, 2:] + 2 * h) / 6.0
    gx = np.zeros_like(h)
    gy = np.zeros_like(h)
    gx[:, 1:-1] = h[:, 2:] - h[:, :-2]
    gy[1:-1, :] = h[2:, :] - h[:-2, :]
    nx = -gx * strength
    ny = gy * strength                     # image rows go down; screen +Y goes up
    nz = np.ones_like(h)
    ln = np.sqrt(nx * nx + ny * ny + nz * nz)
    n = np.stack([nx / ln, ny / ln, nz / ln], axis=-1)
    rgb = np.clip((n * 0.5 + 0.5) * 255, 0, 255).astype(np.uint8)
    alpha = (mask * 255).astype(np.uint8)[..., None]
    return Image.fromarray(np.concatenate([rgb, alpha], axis=-1), "RGBA")


if __name__ == "__main__":
    src = Path(sys.argv[1])
    strength = float(sys.argv[2]) if len(sys.argv) > 2 else 2.0
    out = src.with_name(src.stem + "_n.png")
    normal_map(Image.open(src), strength).save(out)
    print("wrote", out)
