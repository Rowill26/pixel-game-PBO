"""Clear the sky haze the AI painted over the magenta key in a mid (buildings) layer, and cut the strip
of ground the AI drew under the buildings.

python tools/bg_clearmist.py SRC OUT [--cut-below ROW] [--bright 140] [--grey 40]

- every pixel from row --cut-below down becomes the key (the ground layer shows there instead, so
  there is no hard seam between the two layers)
- haze: starting from the key and the top edge, flood through pixels that are bright and nearly grey
  (or lean to magenta) and turn them into key. Darker scenery (thatch, wood, brick, leaves) stops the
  flood, so the buildings stay whole.
"""
import argparse
from collections import deque

import numpy as np
from PIL import Image

ap = argparse.ArgumentParser()
ap.add_argument("src")
ap.add_argument("out")
ap.add_argument("--cut-below", type=int, default=-1)
ap.add_argument("--bright", type=float, default=140)
ap.add_argument("--grey", type=float, default=40)
a = ap.parse_args()

img = np.asarray(Image.open(a.src).convert("RGB")).copy()
h, w = img.shape[:2]
r, g, b = (img[..., i].astype(int) for i in range(3))
mx = np.maximum(np.maximum(r, g), b)
mn = np.minimum(np.minimum(r, g), b)
key = (r > 150) & (b > 150) & (g < 120)
haze = (((r + g + b) / 3 > a.bright) & (mx - mn < a.grey)) | ((r - g > 15) & (b - g > 15))
seen = key.copy()
q = deque(zip(*np.nonzero(key)))
q.extend((0, x) for x in range(w) if haze[0, x])
while q:
    y, x = q.popleft()
    for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        ny, nx = y + dy, x + dx
        if 0 <= ny < h and 0 <= nx < w and not seen[ny, nx] and haze[ny, nx]:
            seen[ny, nx] = True
            q.append((ny, nx))
img[seen] = (255, 0, 255)
if a.cut_below >= 0:
    img[a.cut_below:] = (255, 0, 255)
Image.fromarray(img).save(a.out)
print(f"{a.out}: cleared {100 * seen.mean():.0f}% as haze/key")
