"""Clear the middle of a mid layer (mist, bushes, moonlight the AI painted between the side scenery)
and shrink the layer toward the bottom corners.

python tools/bg_sides.py SRC OUT --left 0.30 --right 0.70 --scale 0.8 [--glow 120]

- between --left and --right (fractions of the width) every pixel that leans blue or lavender is set
  to the magenta key; warm or grey scenery there (stone, brick, wood) stays, so nothing is cut with a
  straight edge
- the central fifth of the picture is always cleared
- with --glow, bright lavender pixels anywhere (moonlight / mist on the key) become key as well
- the picture is placed bottom-centre on a magenta canvas 1/scale times bigger, so after
  bg_pixelize.py --anchor bottom the scenery is smaller and sits in the corners
"""
import argparse

import numpy as np
from PIL import Image

ap = argparse.ArgumentParser()
ap.add_argument("src")
ap.add_argument("out")
ap.add_argument("--left", type=float, default=0.3)
ap.add_argument("--right", type=float, default=0.7)
ap.add_argument("--scale", type=float, default=0.85)
ap.add_argument("--glow", type=float, default=0)
a = ap.parse_args()

src = np.asarray(Image.open(a.src).convert("RGB")).copy()
h, w = src.shape[:2]
r, g, b = (src[..., i].astype(int) for i in range(3))
xs = np.arange(w)[None, :].repeat(h, 0)
middle = (xs > w * a.left) & (xs < w * a.right)
bluish = (b > r + 6) | ((b > g + 10) & (r > g))
key = (middle & bluish) | ((xs > w * 0.4) & (xs < w * 0.6))
if a.glow:
    key |= (b > g + 12) & (r > g) & ((r + g + b) / 3 > a.glow)
    # whitish moonlight haze: very bright and almost grey
    mx = np.maximum(np.maximum(r, g), b)
    mn = np.minimum(np.minimum(r, g), b)
    key |= ((r + g + b) / 3 > 190) & (mx - mn < 28)
    # the moon glow behind the top-left corner is a little darker and bluish grey
    ys0 = np.arange(h)[:, None].repeat(w, 1)
    key |= (ys0 < h * 0.4) & ((r + g + b) / 3 > 150) & (b > r + 15) & (b > g + 12)
# the strip of path/ground the AI paints along the bottom edge between the two sides
ys = np.arange(h)[:, None].repeat(w, 1)
warm = (r > g) & (g >= b - 4)
key |= (ys > h * 0.93) & (xs > w * 0.12) & (xs < w * 0.88) & warm
src[key] = (255, 0, 255)

W, H = int(round(w / a.scale)), int(round(h / a.scale))
out = np.zeros((H, W, 3), np.uint8)
out[:] = (255, 0, 255)
# split at the middle and push each half to its own corner
half = w // 2
out[H - h:, :half] = src[:, :half]
out[H - h:, W - (w - half):] = src[:, half:]
Image.fromarray(out).save(a.out)
print(f"{a.out}: {W}x{H}, cleared {100 * key.mean():.0f}% of the source")
