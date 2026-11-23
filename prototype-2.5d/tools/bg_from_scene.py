"""Cut the main (buildings + ground) layer out of a full-scene picture, using the separately generated
sky and far layers of the same scene to find what is background.

python tools/bg_from_scene.py SCENE FAR SKY OUT --base ROW [--tol 32]

Above row --base (the line the buildings stand on), a scene pixel that matches the far layer (where
the far layer has scenery) or the sky layer (where it does not) becomes the magenta key; a flood from
the top keeps the cut to background reachable from the sky, so buildings stay whole. Everything below
--base is kept. The far and sky layers are drawn behind OUT, so the picture looks like the scene again,
but now they can move separately (parallax).
"""
import argparse
from collections import deque

import numpy as np
from PIL import Image

ap = argparse.ArgumentParser()
ap.add_argument("scene")
ap.add_argument("far")
ap.add_argument("sky")
ap.add_argument("out")
ap.add_argument("--base", type=int, required=True)
ap.add_argument("--tol", type=float, default=32)
ap.add_argument("--seed", action="append", default=[],
                help="x,y (scene pixels) to also start the flood from, e.g. a window between pillars")
ap.add_argument("--anywhere", action="store_true",
                help="every matching pixel above --base is background, connected or not")
a = ap.parse_args()

u = np.asarray(Image.open(a.scene).convert("RGB")).astype(int)
f = np.asarray(Image.open(a.far).convert("RGB").resize((u.shape[1], u.shape[0]))).astype(int)
s = np.asarray(Image.open(a.sky).convert("RGB").resize((u.shape[1], u.shape[0]))).astype(int)
fmag = (f[..., 0] > 150) & (f[..., 2] > 150) & (f[..., 1] < 120)
ref = np.where(fmag[..., None], s, f)
bg = np.sqrt(((u - ref) ** 2).sum(2)) < a.tol
h, w = bg.shape
bg[a.base:] = False
seen = np.zeros_like(bg)
q = deque((0, x) for x in range(w) if bg[0, x])
for sd in a.seed:
    sx, sy = map(int, sd.split(","))
    if bg[sy, sx]:
        q.append((sy, sx))
for y, x in q:
    seen[y, x] = True
while q:
    y, x = q.popleft()
    for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        ny, nx = y + dy, x + dx
        if 0 <= ny < h and 0 <= nx < w and bg[ny, nx] and not seen[ny, nx]:
            seen[ny, nx] = True
            q.append((ny, nx))
if a.anywhere:
    seen = bg.copy()
out = u.astype(np.uint8).copy()
out[seen] = (255, 0, 255)
Image.fromarray(out).save(a.out)
print(f"{a.out}: {100 * seen.mean():.0f}% became background")
