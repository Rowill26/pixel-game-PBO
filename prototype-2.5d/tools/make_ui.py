"""UI assets for the battle HUD, drawn in code from the game palette.

python tools/make_ui.py

- ui/panel.png, ui/panel_active.png: 12x12 nine-patch frames (4 px borders): dark fill, antique gold
  double line, a small diamond "ukiran" stud on each corner
- ui/cursor.png (5x7 pointing right), ui/arrow.png (7x5 pointing down)
- ui/ikon_<sprite>.png: 16x16 turn-order faces cut from each sprite's first idle frame
"""
import glob
import os

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "assets")
OUT = os.path.join(ASSETS, "ui")
os.makedirs(OUT, exist_ok=True)


def hx(h, a=255):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (a,)


FILL = hx("1e1b27", 236)
EDGE_DARK = hx("121019")
GOLD_D = hx("8a6d33")
GOLD = hx("b8954a")
GOLD_L = hx("dcc07a")


def panel(active):
    n = 12
    im = Image.new("RGBA", (n, n), FILL)
    px = im.load()
    line = GOLD_L if active else GOLD
    for i in range(n):
        for (x, y) in ((i, 0), (i, n - 1), (0, i), (n - 1, i)):
            px[x, y] = EDGE_DARK
        for (x, y) in ((i, 1), (i, n - 2), (1, i), (n - 2, i)):
            if 1 <= i <= n - 2:
                px[x, y] = line
        for (x, y) in ((i, 3), (i, n - 4), (3, i), (n - 4, i)):
            if 3 <= i <= n - 4:
                px[x, y] = GOLD_D if not active else GOLD
    # corner studs: a small diamond sitting on the double line
    for cx, cy in ((1, 1), (n - 2, 1), (1, n - 2), (n - 2, n - 2)):
        for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            x, y = cx + dx, cy + dy
            if 0 <= x < n and 0 <= y < n:
                px[x, y] = GOLD_L
    return im


panel(False).save(os.path.join(OUT, "panel.png"))
panel(True).save(os.path.join(OUT, "panel_active.png"))

cur = Image.new("RGBA", (5, 7), (0, 0, 0, 0))
for y in range(7):
    w = 4 - abs(y - 3)
    for x in range(w + 1):
        cur.putpixel((x, y), GOLD_L if x < w else EDGE_DARK)
cur.save(os.path.join(OUT, "cursor.png"))
arr = Image.new("RGBA", (7, 5), (0, 0, 0, 0))
for y in range(5):
    for x in range(y, 7 - y):
        arr.putpixel((x, y), GOLD_L if y < 4 else EDGE_DARK)
arr.save(os.path.join(OUT, "arrow.png"))

# turn-order faces: crop around the top of the figure (the head), 14x14, framed into 16x16
sprites = os.path.join(ASSETS, "sprites")
files = {os.path.basename(f)[:-len("_idle_0.png")]: f for f in glob.glob(os.path.join(sprites, "*_idle_0.png"))}
for f in glob.glob(os.path.join(sprites, "*.png")):          # sprites with one still frame (leak.png)
    base = os.path.basename(f)[:-4]
    if "_" not in base and base not in files:
        files[base] = f
for name, f in sorted(files.items()):
    a = np.array(Image.open(f).convert("RGBA"))
    solid = a[..., 3] > 0
    if name == "juru_mantra":
        solid[:, 54:] = False                     # leave the staff out, find the head
    ys, xs = np.nonzero(solid)
    top = ys.min() + (4 if name == "leak" else 0)  # leak: skip the hair above the face
    # head = the columns of the top 8 rows of the figure
    band = (ys >= top) & (ys < top + 8)
    cx = int(round(xs[band].mean()))
    size = 14 if a.shape[0] <= 80 else 22          # big sprites (ritual form) get a wider crop
    y0 = max(0, top - 1)
    crop = Image.fromarray(a).crop((cx - size // 2, y0, cx - size // 2 + size, y0 + size))
    crop = crop.resize((14, 14), Image.NEAREST) if size != 14 else crop
    icon = Image.new("RGBA", (16, 16), FILL[:3] + (255,))
    icon.alpha_composite(crop, (1, 1))
    px = icon.load()
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            px[x, y] = GOLD
    icon.save(os.path.join(OUT, f"ikon_{name}.png"))
    print("icon", name)
print("ui written to", OUT)
