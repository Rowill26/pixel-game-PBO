"""Split a full-scene battle picture into a main layer and a far layer along natural edges.

python tools/bg_mist.py SCENE OUT_MAIN OUT_FAR --rect x0,y0,x1,y1 [--pct 45] [--min 400]

Inside --rect only the PALE, misty background (brighter than the --pct percentile of the rect and not
very saturated) is opened in the main layer; dark vines, roots, branches and buildings stay in front,
so the edge between the layers runs along their own outlines instead of a straight line.
The far layer is the scene itself, with every kept (front) pixel inside the rect replaced by the
nearest misty pixel, so no copy of a vine sits behind the real one and nothing doubles when the
layers move apart. Outside the rect the far layer is the magenta key (the main layer covers it).
"""
import argparse

import numpy as np
from PIL import Image
from scipy import ndimage

ap = argparse.ArgumentParser()
ap.add_argument("scene")
ap.add_argument("out_main")
ap.add_argument("out_far")
ap.add_argument("--rect", required=True)
ap.add_argument("--pct", type=float, default=45)
ap.add_argument("--sat", type=float, default=0.45)
ap.add_argument("--min", type=int, default=400)
a = ap.parse_args()

u = np.asarray(Image.open(a.scene).convert("RGB")).astype(float)
h, w = u.shape[:2]
x0, y0, x1, y1 = map(int, a.rect.split(","))
win = np.zeros((h, w), bool)
win[y0:y1, x0:x1] = True
mx, mn = u.max(2), u.min(2)
lum = u.mean(2)
sat = (mx - mn) / np.maximum(mx, 1)
thr = np.percentile(lum[win], a.pct)
mist = win & (lum > thr) & (sat < a.sat)
mist = ndimage.binary_opening(mist, iterations=1)
# small dark specks inside the mist (leaves far away) belong to the mist
keep = win & ~mist
lab, n = ndimage.label(keep)
if n:
    sizes = ndimage.sum(keep, lab, range(1, n + 1))
    mist |= np.isin(lab, [i + 1 for i, z in enumerate(sizes) if z < a.min])
# only keep mist that is connected to the big open area (not lone pale spots on trunks)
lab, n = ndimage.label(mist)
if n:
    sizes = ndimage.sum(mist, lab, range(1, n + 1))
    mist = np.isin(lab, [i + 1 for i, z in enumerate(sizes) if z >= 2000])

main = u.astype(np.uint8).copy()
main[mist] = (255, 0, 255)
Image.fromarray(main).save(a.out_main)

# far: misty colour everywhere inside the rect (front pixels get the nearest mist pixel), key outside
idx = ndimage.distance_transform_edt(~mist, return_distances=False, return_indices=True)
far = u[idx[0], idx[1]].astype(np.uint8)
grow = ndimage.binary_dilation(win, iterations=24)
far[~grow] = (255, 0, 255)
Image.fromarray(far).save(a.out_far)
print(f"opened {100 * mist.mean():.0f}% (threshold {thr:.0f})")
