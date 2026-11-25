"""Turn an AI pixel-art half-body portrait into a 128x192 dialog portrait.

python tools/portrait.py SRC OUT [--ramps ...] [--scale N] [--sat-weight W] [--preview P]

Uses the same steps as normalize_ai_pixel.py (find the AI's pixel grid, take the centre of every cell,
remove the plain background, map to the game palette), then enlarges the bust by a whole number
(nearest neighbour, so pixels stay square) to fill the 192 px height, sits it on the bottom edge
(the body is cut there) and, if it is wider than 128, keeps the right part (the face looks right,
toward the dialog box).
"""
import argparse
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
import normalize_ai_pixel as N  # noqa: E402

W, H = 128, 192

ap = argparse.ArgumentParser()
ap.add_argument("src")
ap.add_argument("out")
ap.add_argument("--ramps", default="")
ap.add_argument("--scale", type=int, default=0, help="whole-number enlargement; 0 = fill the height")
ap.add_argument("--height", type=int, default=0,
                help="first shrink the bust to this many pixels tall (majority colour), so every portrait "
                     "shares the same pixel size; 85 with --scale 2 matches the first portrait (Ratna)")
ap.add_argument("--sat-weight", type=float, default=0.0)
ap.add_argument("--bg-tol", type=float, default=18)
ap.add_argument("--min-hole", type=int, default=99999)
ap.add_argument("--shift-x", type=int, default=0)
ap.add_argument("--rebuild-left", type=int, default=0,
                help="the AI cut an object (a staff) in half at the left edge: mirror the first N columns "
                     "of the bust to the left to restore its full width")
ap.add_argument("--preview")
a = ap.parse_args()

rgb = np.asarray(Image.open(a.src).convert("RGB"))
pitch, ox, oy = N.find_pitch(rgb.astype(float))
native = N.sample_grid(rgb, pitch, ox, oy)
def remove_background(img, tol):
    """A bust is cut by the bottom edge and may touch the sides (a staff), so the background colour is
    taken from the TOP row only, and the flood starts from the top, left and right edges."""
    from collections import deque
    h, w = img.shape[:2]
    bg = np.median(img[0].astype(float), axis=0)
    near = np.sqrt(((img.astype(float) - bg) ** 2).sum(2)) <= tol
    keep = np.ones((h, w), bool)
    q = deque([(0, x) for x in range(w)] + [(y, 0) for y in range(h)] + [(y, w - 1) for y in range(h)])
    while q:
        y, x = q.popleft()
        if not (0 <= y < h and 0 <= x < w) or not keep[y, x] or not near[y, x]:
            continue
        keep[y, x] = False
        q.extend(((y + 1, x), (y - 1, x), (y, x + 1), (y, x - 1)))
    return keep


mask = remove_background(native, a.bg_tol)
ramps = {r.strip() for r in a.ramps.split(",") if r.strip()}
bust = N.crop_to_content(N.to_palette(native, mask, ramps, a.sat_weight))
bh, bw = bust.shape[:2]
if a.height and a.height != bh:
    nw = max(1, round(bw * a.height / bh))
    bust = N.resize_majority(bust, nw, a.height)
    bh, bw = bust.shape[:2]
if a.rebuild_left:
    n = a.rebuild_left
    strip = bust[:, :n][:, ::-1]
    # only rows where the object reaches the very first column (the cut edge)
    rows = bust[:, 0, 3] > 0
    add = np.zeros((bh, n, 4), np.uint8)
    add[rows] = strip[rows]
    bust = np.concatenate([add, bust], 1)
    bh, bw = bust.shape[:2]
k = a.scale or max(1, H // bh)
big = np.asarray(Image.fromarray(bust, "RGBA").resize((bw * k, bh * k), Image.NEAREST))
canvas = np.zeros((H, W, 4), np.uint8)
gh, gw = big.shape[:2]
# bottom aligned; horizontally: keep the right part when too wide, else centre
x0 = (W - gw) // 2 if gw <= W else W - gw
x0 += a.shift_x
y0 = H - gh
sy0, dy0 = max(0, -y0), max(0, y0)
sx0, dx0 = max(0, -x0), max(0, x0)
hgt, wid = min(gh - sy0, H - dy0), min(gw - sx0, W - dx0)
canvas[dy0:dy0 + hgt, dx0:dx0 + wid] = big[sy0:sy0 + hgt, sx0:sx0 + wid]
Image.fromarray(canvas, "RGBA").save(a.out)
print(f"grid {pitch:.2f}px -> bust {bw}x{bh} x{k} = {gw}x{gh}, canvas {W}x{H}")
if a.preview:
    src = Image.open(a.src).convert("RGB")
    src.thumbnail((384, 384))
    pv = Image.new("RGB", (src.width + W * 2 + 20, max(src.height, H * 2)), (30, 27, 39))
    pv.paste(src, (0, 0))
    big2 = Image.fromarray(canvas, "RGBA").resize((W * 2, H * 2), Image.NEAREST)
    pv.paste(big2, (src.width + 20, 0), big2)
    pv.save(a.preview)
