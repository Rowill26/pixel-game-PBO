"""Cut the open middle of a full-scene picture so the far layer shows through it (2.5D).

python tools/bg_window.py SCENE FAR SKY OUT --rect x0,y0,x1,y1 [--rect ...] [--tol 45] [--min 300]

Inside each --rect (scene pixels), every pixel that matches the far layer (or the sky layer where the
far layer is magenta) becomes the magenta key; small leftover islands are cleared too. Outside the
rectangles nothing is touched, so trees, altars and gates at the sides stay exactly as painted. Pick
rectangles that do not cross a trunk or a building, so no straight seam can show when the far layer
moves. Things that are in the far picture as well (vines, a distant temple) stay visible through it.
"""
import argparse

import numpy as np
from PIL import Image
from scipy import ndimage

ap = argparse.ArgumentParser()
ap.add_argument("scene")
ap.add_argument("far")
ap.add_argument("sky")
ap.add_argument("out")
ap.add_argument("--rect", action="append", required=True)
ap.add_argument("--tol", type=float, default=45)
ap.add_argument("--min", type=int, default=300, help="islands smaller than this (pixels) are cleared")
a = ap.parse_args()

u = np.asarray(Image.open(a.scene).convert("RGB")).astype(int)
h, w = u.shape[:2]
f = np.asarray(Image.open(a.far).convert("RGB").resize((w, h))).astype(int)
s = np.asarray(Image.open(a.sky).convert("RGB").resize((w, h))).astype(int)
fmag = (f[..., 0] > 150) & (f[..., 2] > 150) & (f[..., 1] < 120)
ref = np.where(fmag[..., None], s, f)
d = np.sqrt(((u - ref) ** 2).sum(2))
win = np.zeros((h, w), bool)
for r in a.rect:
    x0, y0, x1, y1 = map(int, r.split(","))
    win[y0:y1, x0:x1] = True
bg = win & (d < a.tol)
bg = ndimage.binary_closing(bg, iterations=2) & win
keep = win & ~bg
lab, n = ndimage.label(keep)
if n:
    sizes = ndimage.sum(keep, lab, range(1, n + 1))
    bg |= np.isin(lab, [i + 1 for i, z in enumerate(sizes) if z < a.min])
out = u.astype(np.uint8).copy()
out[bg] = (255, 0, 255)
Image.fromarray(out).save(a.out)
print(f"{a.out}: {100 * bg.mean():.0f}% opened")
