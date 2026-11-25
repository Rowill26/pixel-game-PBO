"""Pixel kit (from the v3 mockups): palette, sprite builder, prop drawing helpers.

Also still renders the mockups when run directly.
2.5D placeholder mockups (v3) for Calon Arang: Bayang-Bayang Girah.

Pixel layers are drawn at 640x360 using only the 64-colour palette, upscaled 2x with
nearest-neighbour, then given "camera" effects that a LibGDX shader would add at runtime:
per-layer depth blur, lighting, bloom, fog, colour grading, vignette. UI is drawn last, unblurred.
"""
import math
import random
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

sys.path.insert(0, str(Path(__file__).parent))
from palette2 import PALETTE  # noqa: E402

OUT = None
W, H, S = 640, 360, 2


def hx(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


C = {label.replace(" ", ""): hx(h) for label, h, _, _ in PALETTE}
PAL_SET = set(C.values())

MAT = {
    "skin": ("Kulit2", "Kulit3", "Kulit4"), "skinA": ("Kulit2", "Kulit3", "Kulit4"),
    "skinD": ("Kulit1", "Kulit2", "Kulit3"), "hair": ("Bayangan1", "Bayangan2", "Bayangan3"),
    "white": ("Putih1", "Putih2", "Putih3"), "whiteD": ("Batu4", "Putih1", "Putih2"),
    "red": ("Merah2", "Merah3", "Merah4"), "redD": ("Merah1", "Merah2", "Merah3"),
    "gold": ("Emas2", "Emas3", "Emas4"), "green": ("Daun3", "Daun4", "Daun5"),
    "dark": ("Bayangan2", "Bayangan3", "Langit2"), "wood": ("Tanah2", "Tanah3", "Tanah4"),
    "steel": ("Batu3", "Batu4", "Putih3"), "pale": ("Batu3", "Batu4", "Batu5"),
    "ghost": ("Girah1", "Girah2", "Girah3"), "pcloth": ("Bayangan2", "Girah1", "Girah2"),
    "mauve": ("Senja2", "Senja3", "Senja4"), "mane": ("Putih1", "Putih2", "Putih3"),
    "stone": ("Batu2", "Batu3", "Batu4"),
}
OUTLINE = {"skin": "Kulit1", "skinA": "Kulit1", "skinD": "Bayangan1", "white": "Batu2", "whiteD": "Batu2",
           "mane": "Batu2", "red": "Merah1", "redD": "Bayangan1", "gold": "Emas1", "green": "Daun2",
           "pale": "Batu2", "mauve": "Senja1", "steel": "Batu2", "wood": "Tanah1"}
EMIS = {"eye": "Bayangan1", "glowP": "Girah4", "glowF": "Api3", "glowW": "Putih3", "brow": "Bayangan1",
        "mouth": "Merah2", "nose": "Kulit2", "eyeW": "Putih2", "nail": "Putih3", "polB": "Bayangan1",
        "polW": "Putih2", "paint": "Merah3", "flowerW": "Putih3", "flowerC": "Emas4", "eyeR": "Merah4",
        "lid": "Bayangan1", "strandD": "Batu4", "strandL": "Putih3", "stripeR": "Merah3", "stripeW": "Putih2"}
FOG = {"fogA": ("Girah3", 2), "fogC": ("Girah2", 4), "smoke": ("Batu4", 3)}


class Spr:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.lab = [[None] * w for _ in range(h)]

    def put(self, x, y, m):
        x, y = int(x), int(y)
        if 0 <= x < self.w and 0 <= y < self.h:
            self.lab[y][x] = m

    def rect(self, x0, y0, x1, y1, m):
        for y in range(int(y0), int(y1) + 1):
            for x in range(int(x0), int(x1) + 1):
                self.put(x, y, m)

    def ellipse(self, cx, cy, rx, ry, m):
        for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
            for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
                if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0:
                    self.put(x, y, m)

    def _mask(self, fn, m):
        mk = Image.new("L", (self.w, self.h), 0)
        fn(ImageDraw.Draw(mk))
        a = np.array(mk)
        for y, x in zip(*np.nonzero(a)):
            self.lab[y][x] = m

    def poly(self, pts, m):
        self._mask(lambda d: d.polygon(pts, fill=255), m)

    def line(self, x0, y0, x1, y1, m, w=1):
        self._mask(lambda d: d.line([(x0, y0), (x1, y1)], fill=255, width=w), m)

    def render(self, mirror=False, shade=1):
        lab = [row[::-1] for row in self.lab] if mirror else self.lab
        im = Image.new("RGBA", (self.w, self.h), (0, 0, 0, 0))
        px = im.load()

        def at(x, y):
            return lab[y][x] if 0 <= x < self.w and 0 <= y < self.h else None

        solid = [[False] * self.w for _ in range(self.h)]
        for y in range(self.h):
            for x in range(self.w):
                m = lab[y][x]
                if m is None:
                    continue
                if m in FOG:
                    col, step = FOG[m]
                    if (step == 2 and (x + y) % 2 == 0) or (step == 3 and (x + 2 * y) % 3 == 0) \
                            or (step == 4 and x % 2 == 0 and y % 2 == 0):
                        px[x, y] = C[col] + (255,)
                    continue
                solid[y][x] = True
                if m in EMIS:
                    px[x, y] = C[EMIS[m]] + (255,)
                    continue
                dk, bs, lt = MAT[m]
                dark = any(at(x + k, y) != m for k in range(1, shade + 1)) or at(x, y + shade) != m
                light = at(x - 1, y) != m or at(x, y - 1) != m
                px[x, y] = C[dk if dark else lt if light else bs] + (255,)
        for y in range(self.h):
            for x in range(self.w):
                if solid[y][x]:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < self.w and 0 <= ny < self.h and solid[ny][nx]:
                        px[x, y] = C[OUTLINE.get(lab[ny][nx], "Bayangan1")] + (255,)
                        break
        return im


# =========================================================== characters (drawn facing right)
def human(top, bottom, wide=0, hair="short", long_robe=False):
    s = Spr(64, 64)
    if hair == "long":
        s.poly([(25, 20), (33, 18), (34, 44), (23, 44)], "hair")
    if hair == "bun":
        s.ellipse(26, 18, 3.5, 3.5, "hair")
    s.rect(23 - wide, 33, 25 - wide, 44, "skinA")
    if not long_robe:
        s.rect(27, 55, 29, 61, "skinA"); s.rect(34, 55, 36, 61, "skinA")
        s.rect(26, 61, 29, 61, "skinD"); s.rect(34, 61, 37, 61, "skinD")
        s.poly([(25 - wide, 44), (39 + wide, 44), (40 + wide, 56), (24 - wide, 56)], bottom)
    else:
        s.poly([(25 - wide, 44), (39 + wide, 44), (42 + wide, 61), (22 - wide, 61)], bottom)
    s.poly([(25 - wide, 32), (39 + wide, 32), (38 + wide, 45), (26 - wide, 45)], top)
    s.rect(31, 29, 34, 32, "skin")
    s.ellipse(33, 23.5, 6, 6.5, "skin")
    s.ellipse(32, 19.5, 6.5, 4, "hair")
    s.rect(26, 19, 29, 25, "hair")
    s.rect(35, 23, 35, 24, "eye"); s.rect(38, 23, 38, 24, "eye")
    s.put(35, 22, "brow"); s.put(38, 22, "brow"); s.put(37, 27, "mouth")
    s.rect(37 + wide, 33, 39 + wide, 42, "skinA")
    s.rect(37 + wide, 42, 40 + wide, 44, "skinA")
    return s


def pendekar():
    s = human("skin", "red", wide=2)
    s.line(25, 32, 38, 44, "redD", 3)
    s.rect(26, 32, 37, 32, "gold")
    s.rect(23, 44, 41, 45, "gold")
    s.rect(26, 16, 39, 18, "red"); s.rect(22, 17, 25, 20, "red")
    s.rect(39, 39, 42, 40, "gold")
    s.line(42, 44, 50, 31, "steel", 1); s.line(43, 44, 51, 31, "steel", 1)
    s.rect(40, 43, 43, 46, "gold")
    return s


def juru_mantra():
    s = Spr(64, 64)
    s.rect(44, 10, 45, 61, "wood")
    base = human("white", "white", wide=1, long_robe=True)
    for y in range(64):
        for x in range(64):
            if base.lab[y][x]:
                s.lab[y][x] = base.lab[y][x]
    s.line(26, 33, 37, 50, "gold", 2)
    s.ellipse(32, 18, 7, 4, "white")
    s.rect(41, 40, 44, 43, "skinA")
    s.ellipse(44.5, 9, 2, 2, "glowW")
    return s


def peramu():
    s = human("green", "green", wide=3, hair="bun")
    s.rect(22, 44, 42, 45, "gold")
    s.ellipse(43, 50, 4, 4, "wood")
    s.ellipse(21, 49, 2.5, 3.5, "gold")
    s.put(43, 48, "glowW")
    return s


def pengintai():
    s = human("dark", "dark", wide=-1)
    s.rect(33, 25, 40, 28, "dark")
    s.rect(26, 18, 39, 19, "dark")
    s.rect(22, 19, 26, 21, "dark")
    s.line(40, 43, 48, 38, "steel", 1)
    return s


def ratna():
    s = human("white", "mauve", wide=0, hair="bun", long_robe=True)
    s.line(25, 33, 38, 52, "mauve", 3)
    s.put(26, 15, "glowF")
    s.rect(33, 41, 38, 43, "skinA")
    return s


def bharadah():
    s = Spr(64, 64)
    s.rect(44, 26, 45, 61, "wood")
    base = human("white", "white", wide=1, long_robe=True)
    for y in range(64):
        for x in range(64):
            if base.lab[y][x]:
                s.lab[y][x] = base.lab[y][x]
    s.ellipse(32, 19, 6.5, 4.5, "whiteD")
    s.ellipse(27, 17, 3, 3, "whiteD")
    s.rect(26, 19, 29, 25, "whiteD")
    s.poly([(33, 26), (40, 26), (38, 36), (35, 36)], "whiteD")
    s.put(37, 27, "whiteD")
    s.rect(41, 40, 44, 43, "skinA")
    return s


def leak():
    s = Spr(64, 64)
    s.poly([(27, 15), (41, 14), (44, 44), (24, 46)], "hair")
    s.poly([(28, 30), (40, 30), (44, 50), (24, 50)], "pale")
    for x in range(24, 45, 4):
        s.poly([(x, 50), (x + 2, 55), (x + 4, 50)], "pale")
    s.poly([(26, 30), (30, 30), (29, 46), (25, 46)], "hair")
    s.ellipse(35, 22, 5, 6, "pale")
    s.poly([(29, 15), (41, 15), (41, 19), (34, 18), (30, 21)], "hair")
    s.put(36, 22, "glowP"); s.put(39, 22, "glowP")
    s.put(38, 26, "mouth")
    s.line(39, 33, 51, 37, "pale", 2)
    for i in range(3):
        s.line(51, 36 + i, 55, 34 + 2 * i, "nail")
    for y in range(52, 62):
        half = max(1, 10 - (y - 52))
        for x in range(34 - half, 34 + half):
            if s.lab[y][x] is None:
                s.put(x, y, "fogA" if y < 57 else "fogC")
    return s


def pengikut():
    s = Spr(64, 64)
    s.rect(26, 54, 28, 61, "skinD"); s.rect(33, 54, 35, 61, "skinD")
    s.poly([(22, 34), (38, 29), (43, 56), (21, 58)], "pcloth")
    s.ellipse(36, 26, 7, 7, "pcloth")
    s.ellipse(39, 27, 3, 4, "skinD")
    s.put(40, 26, "eyeR")
    s.rect(40, 40, 45, 42, "skinD")
    s.rect(44, 42, 48, 46, "gold")
    for y in range(18, 42):
        s.put(46 + (y // 3) % 2, y, "smoke")
    return s


def calon_arang_ritual():
    s = Spr(128, 128)
    s.poly([(54, 22), (80, 22), (110, 126), (22, 126)], "mane")
    for x in range(28, 106, 4):
        top = 34 if 50 < x < 86 else 50
        s.line(x, top, x + (x - 66) // 6, 124, "strandD")
    for x in range(30, 104, 9):
        s.line(x + 2, 60, x + 2 + (x - 66) // 6, 118, "strandL")
    s.line(56, 60, 28, 34, "skinA", 6)
    s.line(80, 60, 108, 40, "skinA", 6)
    for t in range(0, 30, 6):
        s.line(56 - t * 0.93, 60 - t * 0.87, 56 - (t + 3) * 0.93, 60 - (t + 3) * 0.87, "stripeR", 6)
        s.line(80 + t * 0.93, 60 - t * 0.67, 80 + (t + 3) * 0.93, 60 - (t + 3) * 0.67, "stripeR", 6)
    for i in range(5):
        s.line(26 + i, 33, 18 + i * 2, 18, "nail")
        s.line(108 + i, 40, 112 + i * 3, 25, "nail")
    s.poly([(54, 52), (80, 52), (84, 80), (50, 80)], "redD")
    for y in range(80, 90, 3):
        for x in range(48, 88, 3):
            s.rect(x, y, x + 2, y + 2, "polB" if ((x // 3 + y // 3) % 2) else "polW")
    s.ellipse(67, 40, 10, 12, "skin")
    s.rect(60, 37, 63, 39, "eyeW"); s.rect(70, 37, 73, 39, "eyeW")
    s.put(62, 38, "glowF"); s.put(72, 38, "glowF")
    s.line(59, 35, 64, 34, "brow", 2); s.line(70, 34, 75, 35, "brow", 2)
    s.line(60, 41, 61, 45, "paint", 1); s.line(73, 41, 72, 45, "paint", 1)
    s.line(63, 48, 71, 48, "mouth", 2)
    s.poly([(53, 30), (81, 30), (84, 14), (77, 22), (72, 8), (67, 20), (62, 8), (57, 22), (50, 14)], "gold")
    for fx, fy in ((56, 26), (67, 22), (78, 26)):
        s.ellipse(fx, fy, 2.5, 2.5, "flowerW"); s.put(fx, fy, "flowerC")
    s.rect(58, 112, 62, 124, "stripeW"); s.rect(72, 112, 76, 124, "stripeW")
    for y in range(112, 125, 4):
        s.rect(58, y, 62, y + 1, "stripeR"); s.rect(72, y, 76, y + 1, "stripeR")
    for (cx, cy, r) in ((104, 70, 12), (18, 82, 12), (112, 104, 9)):
        for y in range(cy - r, cy + r):
            for x in range(cx - r, cx + r):
                if (r - 4) ** 2 <= (x - cx) ** 2 + (y - cy) ** 2 <= r * r and s.lab[y][x] is None:
                    s.put(x, y, "fogA")
    return s


def npc(top, bottom, hair="short", carry=False, seed=0):
    s = Spr(48, 48)
    if hair == "bun":
        s.ellipse(19, 13, 2.5, 2.5, "hair")
    s.rect(18, 25, 19, 33, "skinA")
    s.rect(21, 41, 22, 46, "skinA"); s.rect(25, 41, 26, 46, "skinA")
    s.poly([(19, 33), (29, 33), (30, 42), (18, 42)], bottom)
    s.poly([(19, 24), (29, 24), (28, 34), (20, 34)], top)
    s.ellipse(24.5, 17.5, 4.5, 5, "skin")
    s.ellipse(24, 14.5, 5, 3, "hair")
    s.rect(26, 17, 26, 18, "eye"); s.rect(28, 17, 28, 18, "eye")
    s.rect(28, 25, 29, 32, "skinA")
    if carry:
        s.ellipse(24, 9, 5, 2, "gold")
    return s


# =========================================================== drawing helpers
def col(name):
    return C[name] + (255,)


def dither(img, x0, y0, x1, y1, name, density=2, phase=0):
    px = img.load()
    c = col(name)
    for y in range(max(0, int(y0)), min(img.height, int(y1))):
        for x in range(max(0, int(x0)), min(img.width, int(x1))):
            if (density == 2 and (x + y + phase) % 2 == 0) or \
                    (density == 4 and x % 2 == 0 and (y + phase) % 2 == 0) or \
                    (density == 8 and x % 4 == 0 and (y + phase) % 2 == 0):
                px[x, y] = c


def brick(d, x0, y0, x1, y1, rnd, moss=True):
    d.rectangle([x0, y0, x1, y1], fill=col("Bata3"))
    for y in range(int(y0), int(y1) + 1, 3):
        d.line([(x0, y), (x1, y)], fill=col("Bata1"))
        off = 0 if (y // 3) % 2 else 3
        for x in range(int(x0) + off, int(x1), 6):
            d.line([(x, y), (x, min(y + 2, y1))], fill=col("Bata2"))
            r = rnd.random()
            if r < 0.18:
                d.line([(x + 1, y + 1), (min(x + 5, x1), y + 1)], fill=col("Bata4"))
            elif r < 0.3:
                d.line([(x + 1, y + 1), (min(x + 5, x1), y + 2)], fill=col("Bata2"))
    d.line([(x0, y0), (x1, y0)], fill=col("Bata5"))
    d.line([(x1, y0), (x1, y1)], fill=col("Bata1"))
    if moss:
        for x in range(int(x0), int(x1)):
            h = rnd.randint(0, 3)
            if h:
                d.line([(x, y1 - h), (x, y1)], fill=col("Daun3" if h > 1 else "Daun4"))


def candi_bentar(d, cx, base, h, w, rnd, gap=None):
    gap = gap or w // 3
    for side in (-1, 1):
        x_in = cx + side * gap // 2
        x_out = cx + side * (gap // 2 + w)
        xa, xb = sorted((x_in, x_out))
        tiers = [(0, h * 0.55), (h * 0.55, h * 0.72), (h * 0.72, h * 0.84), (h * 0.84, h * 0.93)]
        for i, (t0, t1) in enumerate(tiers):
            shrink = i * w * 0.12
            if side < 0:
                ax, bx = xa + shrink, xb
            else:
                ax, bx = xa, xb - shrink
            brick(d, ax, base - t1, bx, base - t0, rnd, moss=(i == 0))
            d.line([(ax - 1, base - t1), (bx + 1, base - t1)], fill=col("Bata5"))
        fx = (xa + xb) / 2 + side * w * 0.12
        d.polygon([(fx - 2, base - h * 0.93), (fx + 2, base - h * 0.93), (fx, base - h)], fill=col("Bata4"))
        d.rectangle([xa + 2, base - h * 0.4, xb - 2, base - h * 0.36], fill=col("Bata1"))


def thatch_roof(d, x0, x1, y_eave, y_peak, name_ramp="Jerami", inset=None):
    inset = inset if inset is not None else (x1 - x0) * 0.28
    pts = [(x0, y_eave), (x1, y_eave), (x1 - inset, y_peak), (x0 + inset, y_peak)]
    d.polygon(pts, fill=col(f"{name_ramp}2" if name_ramp == "Jerami" else "Bayangan3"))
    dark = col(f"{name_ramp}1" if name_ramp == "Jerami" else "Bayangan2")
    lite = col(f"{name_ramp}3" if name_ramp == "Jerami" else "Langit2")
    for y in range(int(y_peak) + 2, int(y_eave), 3):
        t = (y - y_peak) / max(1, (y_eave - y_peak))
        xa = x0 + inset * (1 - t)
        xb = x1 - inset * (1 - t)
        d.line([(xa, y), (xb, y)], fill=dark)
    for x in range(int(x0) + 3, int(x1) - 2, 7):
        d.line([(x, y_eave - 1), (x + 2, y_eave - 5)], fill=lite)
    d.line([(x0, y_eave), (x1, y_eave)], fill=col("Bayangan1"))
    d.line([(x0 + inset, y_peak), (x1 - inset, y_peak)], fill=dark)


def bale(d, x0, x1, base, roof_h, rnd, ramp="Jerami"):
    w = x1 - x0
    d.rectangle([x0 - 2, base - 6, x1 + 2, base], fill=col("Batu3"))
    d.line([(x0 - 2, base - 6), (x1 + 2, base - 6)], fill=col("Batu5"))
    d.line([(x0 - 2, base), (x1 + 2, base)], fill=col("Batu1"))
    y_eave = base - 6 - roof_h * 0.6
    d.rectangle([x0 + 4, y_eave, x1 - 4, base - 6], fill=col("Bayangan2"))
    for i in range(4):
        x = x0 + 4 + i * (w - 10) / 3
        d.rectangle([x, y_eave, x + 3, base - 6], fill=col("Tanah3"))
        d.line([(x, y_eave), (x, base - 6)], fill=col("Tanah4"))
    thatch_roof(d, x0 - 8, x1 + 8, y_eave, y_eave - roof_h * 0.7, ramp)


def meru(d, cx, base, rnd, tiers=5, w=56):
    brick(d, cx - w * 0.35, base - 18, cx + w * 0.35, base, rnd)
    y = base - 18
    tw = w
    for i in range(tiers):
        d.rectangle([cx - tw * 0.18, y - 6, cx + tw * 0.18, y], fill=col("Tanah2"))
        y -= 6
        thatch_roof(d, cx - tw / 2, cx + tw / 2, y, y - 9, "Ijuk", inset=tw * 0.22)
        y -= 9
        tw *= 0.8
    d.line([(cx, y), (cx, y - 8)], fill=col("Emas2"))


def kelapa(d, x, base, h, lean, rnd):
    pts = []
    for i in range(h + 1):
        t = i / h
        pts.append((x + lean * t * t, base - i))
    for (ax, ay), (bx, by) in zip(pts, pts[1:]):
        d.line([(ax, ay), (bx, by)], fill=col("Tanah3"), width=3)
        if int(ay) % 5 == 0:
            d.point((ax - 1, ay), fill=col("Tanah2"))
    tx, ty = pts[-1]
    for a in range(0, 360, 30):
        r = rnd.randint(16, 22)
        ex = tx + r * math.cos(math.radians(a))
        ey = ty + r * 0.55 * math.sin(math.radians(a)) + (r * 0.35 if 0 < a < 180 else r * 0.15)
        d.line([(tx, ty), ((tx + ex) / 2, ty - 3 + (ey - ty) / 3), (ex, ey)], fill=col("Daun3"), width=2)
        d.line([(tx, ty - 1), ((tx + ex) / 2, ty - 4 + (ey - ty) / 3)], fill=col("Daun5"))
    d.ellipse([tx - 3, ty - 2, tx + 3, ty + 3], fill=col("Tanah3"))


def kamboja(d, x, base, rnd, scale=1.0):
    def br(x0, y0, x1, y1, w):
        d.line([(x0, y0), (x1, y1)], fill=col("Tanah4"), width=w)
        d.line([(x0 + 1, y0), (x1 + 1, y1)], fill=col("Tanah5"))
    s = scale
    br(x, base, x, base - 18 * s, int(4 * s))
    br(x, base - 14 * s, x - 14 * s, base - 30 * s, int(3 * s))
    br(x, base - 16 * s, x + 12 * s, base - 34 * s, int(3 * s))
    br(x, base - 18 * s, x + 2 * s, base - 38 * s, int(2 * s))
    for cx, cy in ((x - 14 * s, base - 33 * s), (x + 12 * s, base - 37 * s), (x + 2 * s, base - 41 * s),
                   (x - 6 * s, base - 38 * s), (x + 18 * s, base - 30 * s)):
        r = 9 * s
        d.ellipse([cx - r, cy - r * 0.6, cx + r, cy + r * 0.6], fill=col("Daun3"))
        d.ellipse([cx - r * 0.7, cy - r * 0.6, cx + r * 0.4, cy + r * 0.1], fill=col("Daun4"))
        for _ in range(int(5 * s)):
            fx, fy = cx + rnd.uniform(-r, r), cy + rnd.uniform(-r * 0.5, r * 0.4)
            d.rectangle([fx, fy, fx + 1, fy + 1], fill=col("Putih3"))
            d.point((fx, fy + 1), fill=col("Emas4"))


def penjor(d, x, base, h, bend, rnd):
    pts = []
    for i in range(41):
        t = i / 40
        px = x + bend * (t ** 2.2)
        py = base - h * math.sin(t * math.pi * 0.62) / math.sin(math.pi * 0.62)
        pts.append((px, py))
    d.line(pts, fill=col("Jerami2"), width=2)
    d.line([(p[0] - 1, p[1]) for p in pts[:30]], fill=col("Jerami3"))
    for i in range(10, 41, 4):
        px, py = pts[i]
        d.line([(px, py), (px + rnd.randint(-1, 1), py + 5)], fill=col("Jerami3"))
    ex, ey = pts[-1]
    d.polygon([(ex - 4, ey), (ex + 4, ey), (ex + 1, ey + 14), (ex - 1, ey + 14)], fill=col("Jerami3"))
    d.line([(ex, ey), (ex, ey + 14)], fill=col("Emas3"))
    d.rectangle([x - 6, base - 22, x + 6, base - 18], fill=col("Jerami3"))
    d.rectangle([x - 4, base - 18, x + 4, base - 14], fill=col("Emas3"))


def beringin(d, x, base, rnd, w=200, h=190, poleng=True):
    for i in range(14):
        cx = x + rnd.uniform(-w * 0.45, w * 0.45)
        cy = base - h * 0.75 + rnd.uniform(-h * 0.2, h * 0.15)
        r = rnd.uniform(w * 0.18, w * 0.3)
        d.ellipse([cx - r, cy - r * 0.6, cx + r, cy + r * 0.6], fill=col("Daun2"))
    for i in range(10):
        cx = x + rnd.uniform(-w * 0.4, w * 0.35)
        cy = base - h * 0.85 + rnd.uniform(-h * 0.12, h * 0.1)
        r = rnd.uniform(w * 0.1, w * 0.18)
        d.ellipse([cx - r, cy - r * 0.55, cx + r, cy + r * 0.45], fill=col("Daun3"))
        d.ellipse([cx - r * 0.6, cy - r * 0.55, cx + r * 0.2, cy - r * 0.05], fill=col("Daun4"))
    tw = w * 0.16
    d.polygon([(x - tw, base), (x + tw, base), (x + tw * 0.5, base - h * 0.55), (x - tw * 0.6, base - h * 0.55)],
              fill=col("Tanah2"))
    for i in range(7):
        xx = x - tw + i * tw / 3
        d.line([(xx, base), (xx + rnd.randint(-3, 3), base - h * 0.5)], fill=col("Tanah1"), width=2)
        d.line([(xx + 2, base), (xx + 2, base - h * 0.45)], fill=col("Tanah3"))
    for i in range(16):
        rx = x + rnd.uniform(-w * 0.45, w * 0.45)
        top = base - h * 0.62 + rnd.uniform(-10, 10)
        d.line([(rx, top), (rx + rnd.uniform(-3, 3), top + rnd.uniform(30, 70))], fill=col("Tanah2"))
    if poleng:
        y0 = base - h * 0.32
        for yy in range(int(y0), int(y0 + 18), 3):
            for xx in range(int(x - tw * 0.9), int(x + tw * 0.9), 3):
                d.rectangle([xx, yy, xx + 2, yy + 2],
                            fill=col("Bayangan1" if ((xx // 3 + yy // 3) % 2) else "Putih2"))
        d.line([(x - tw, y0 + 18), (x + tw, y0 + 18)], fill=col("Merah3"), width=2)


def nisan(d, x, base, tilt, rnd, h=14):
    d.polygon([(x, base), (x + 8, base + tilt), (x + 8, base - h + tilt), (x + 4, base - h - 4), (x, base - h)],
              fill=col("Batu3"))
    d.line([(x, base), (x, base - h)], fill=col("Batu4"))
    d.line([(x + 8, base + tilt), (x + 8, base - h + tilt)], fill=col("Batu2"))
    d.line([(x + 1, base - h + 1), (x + 4, base - h - 3)], fill=col("Batu5"))
    if rnd.random() < 0.6:
        d.line([(x, base - 2), (x + 5, base - 1)], fill=col("Daun3"))
    d.line([(x - 2, base + 1), (x + 10, base + 1 + tilt)], fill=col("Bayangan1"))


def obor(d, x, base, h=34):
    d.line([(x, base), (x, base - h)], fill=col("Jerami1"), width=2)
    d.line([(x - 1, base), (x - 1, base - h)], fill=col("Jerami2"))
    d.rectangle([x - 2, base - h - 2, x + 2, base - h + 2], fill=col("Tanah2"))
    d.polygon([(x - 3, base - h - 2), (x + 3, base - h - 2), (x, base - h - 12)], fill=col("Api1"))
    d.polygon([(x - 2, base - h - 2), (x + 2, base - h - 2), (x, base - h - 9)], fill=col("Api2"))
    d.line([(x, base - h - 3), (x, base - h - 6)], fill=col("Api3"))
    return (x, base - h - 6)


def arca(d, x, base, rnd, h=46):
    d.rectangle([x - 12, base - 8, x + 12, base], fill=col("Batu2"))
    d.line([(x - 12, base - 8), (x + 12, base - 8)], fill=col("Batu4"))
    d.polygon([(x - 9, base - 8), (x + 9, base - 8), (x + 7, base - h * 0.62), (x - 7, base - h * 0.62)], fill=col("Batu3"))
    d.ellipse([x - 7, base - h, x + 7, base - h * 0.62 + 2], fill=col("Batu3"))
    d.ellipse([x - 5, base - h + 2, x + 1, base - h * 0.75], fill=col("Batu4"))
    d.rectangle([x - 4, base - h * 0.84, x - 2, base - h * 0.82], fill=col("Bayangan2"))
    d.rectangle([x + 2, base - h * 0.84, x + 4, base - h * 0.82], fill=col("Bayangan2"))
    d.line([(x + 9, base - 10), (x + 13, base - h * 0.55)], fill=col("Batu2"), width=4)
    for _ in range(18):
        mx, my = x + rnd.uniform(-10, 10), base - rnd.uniform(4, h * 0.5)
        d.point((mx, my), fill=col("Daun3"))
    for yy in range(int(base - h * 0.45), int(base - h * 0.3), 3):
        for xx in range(x - 8, x + 8, 3):
            d.rectangle([xx, yy, xx + 2, yy + 2], fill=col("Bayangan1" if ((xx // 3 + yy // 3) % 2) else "Putih2"))


def tempayan(d, x, base):
    d.ellipse([x - 6, base - 11, x + 6, base], fill=col("Bata3"))
    d.ellipse([x - 5, base - 10, x - 1, base - 4], fill=col("Bata4"))
    d.rectangle([x - 3, base - 13, x + 3, base - 10], fill=col("Bata2"))
    d.line([(x - 6, base), (x + 6, base)], fill=col("Bayangan1"))


def sesajen(d, x, base):
    d.rectangle([x - 4, base - 3, x + 4, base], fill=col("Daun4"))
    d.point((x - 2, base - 4), fill=col("Merah4"))
    d.point((x, base - 4), fill=col("Putih3"))
    d.point((x + 2, base - 4), fill=col("Emas4"))


def grass(img, rnd, y0, y1, n, names=("Daun4", "Daun5")):
    d = ImageDraw.Draw(img)
    for _ in range(n):
        x, y = rnd.randint(0, W - 1), rnd.randint(int(y0), int(y1))
        c = col(rnd.choice(names))
        d.line([(x, y), (x - 1, y - 2)], fill=c)
        d.line([(x + 1, y), (x + 2, y - 2)], fill=c)


def paste(layer, im, fx, fy):
    w, h = im.size
    layer.alpha_composite(im, (int(fx - w // 2), int(fy - h + 3)))


def shadow(layer, cx, fy, rx, ry=2):
    # dithered pixel-art shadow: solid core, checkered rim (stays inside the palette)
    px = layer.load()
    for y in range(int(fy - ry), int(fy + ry) + 1):
        for x in range(int(cx - rx), int(cx + rx) + 1):
            if not (0 <= x < layer.width and 0 <= y < layer.height):
                continue
            r = ((x - cx) / rx) ** 2 + ((y - fy) / max(ry, 1)) ** 2
            if r <= 0.45 or (r <= 1.0 and (x + y) % 2 == 0):
                px[x, y] = col("Bayangan1")


# =========================================================== camera effects (1280x720 float)
def up(layer, blur=0.0):
    im = layer.resize((W * S, H * S), Image.NEAREST)
    if blur:
        im = im.filter(ImageFilter.GaussianBlur(blur))
    return im


def to_f(im):
    return np.asarray(im.convert("RGB"), dtype=np.float32) / 255.0


def radial(cx, cy, r, ry=None):
    ry = ry or r
    yy, xx = np.mgrid[0:H * S, 0:W * S].astype(np.float32)
    d = np.sqrt(((xx - cx) / r) ** 2 + ((yy - cy) / ry) ** 2)
    return np.clip(1 - d, 0, 1)


def light_pass(base, ambient, lights):
    acc = np.ones_like(base) * np.array(ambient, dtype=np.float32)
    glow = np.zeros_like(base)
    for (x, y, r, color, inten, g) in lights:
        f = radial(x * S, y * S, r * S, r * S * 0.75)
        c = np.array(color, dtype=np.float32) / 255.0
        acc += (f ** 2)[..., None] * c * inten
        glow += (f ** 4)[..., None] * c * g
    return base * acc + glow


def bloom(img, thr=0.62, radius=16, k=0.9):
    br = np.clip(img - thr, 0, None)
    bi = Image.fromarray(np.clip(br * 255 / (1 - thr), 0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(radius))
    return img + np.asarray(bi, dtype=np.float32) / 255.0 * (1 - thr) * k


def grade(img, sat=0.85, tint=(1, 1, 1), lift=(0, 0, 0), vignette=0.45):
    lum = (img * np.array([0.299, 0.587, 0.114], dtype=np.float32)).sum(axis=2, keepdims=True)
    img = lum + (img - lum) * sat
    img = img * np.array(tint, dtype=np.float32) + np.array(lift, dtype=np.float32)
    yy, xx = np.mgrid[0:H * S, 0:W * S].astype(np.float32)
    r2 = ((xx - W * S / 2) / (W * S / 2)) ** 2 + ((yy - H * S / 2) / (H * S / 2)) ** 2
    img = img * (1 - vignette * np.clip(r2 - 0.25, 0, 1.5) / 1.5)[..., None]
    return img


def finish(img):
    return Image.fromarray(np.clip(img * 255, 0, 255).astype(np.uint8)).convert("RGBA")


def compose(layers):
    out = Image.new("RGBA", (W * S, H * S), (0, 0, 0, 255))
    for im in layers:
        out.alpha_composite(im)
    return out


def check_palette(layers, name):
    for i, lay in enumerate(layers):
        a = np.asarray(lay)
        rgb = a[..., :3][a[..., 3] == 255]
        cols = {tuple(c) for c in np.unique(rgb.reshape(-1, 3), axis=0)}
        bad = cols - PAL_SET
        assert not bad, f"{name} layer {i}: off-palette {list(bad)[:4]}"


# =========================================================== UI (native res, crisp)
FONT = ImageFont.load_default_imagefont()


def text(d, xy, s, name="TeksUI", outline=None):
    if outline:
        for ox, oy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            d.text((xy[0] + ox, xy[1] + oy), s, font=FONT, fill=col(outline))
    d.text(xy, s, font=FONT, fill=col(name))


def panel(d, x0, y0, x1, y1, alpha=225, edge="Emas2"):
    d.rectangle([x0, y0, x1, y1], fill=C["Bayangan1"] + (alpha,))
    d.rectangle([x0, y0, x1, y1], outline=col(edge))
    d.line([(x0 + 2, y0 + 2), (x1 - 2, y0 + 2)], fill=col("Bayangan3"))


def slanted(d, pts, fill, outline=None, alpha=255):
    d.polygon(pts, fill=C[fill] + (alpha,))
    if outline:
        d.line(pts + [pts[0]], fill=col(outline), width=1)


def bar(d, x, y, w, h, frac, light, dark):
    d.rectangle([x, y, x + w, y + h], fill=col("Bayangan1"), outline=col("Bayangan3"))
    fw = int((w - 2) * frac)
    if fw > 0:
        d.rectangle([x + 1, y + 1, x + fw, y + h - 1], fill=col(dark))
        d.line([(x + 1, y + 1), (x + fw, y + 1)], fill=col(light))
        if h > 3:
            d.rectangle([x + 1, y + 1, x + fw, y + h // 2], fill=col(light))


def tri(d, cx, y, name):
    d.polygon([(cx - 3, y), (cx + 3, y), (cx, y + 4)], fill=col(name), outline=col("Bayangan1"))


def battle_ui(narr, active=0, party=None, turn=None, ronde="RONDE 1"):
    ui = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(ui)
    panel(d, 6, 6, 98, 21)
    text(d, (12, 8), ronde, "Emas4")
    panel(d, 190, 5, 450, 24)
    for i, (skin, hair, ring) in enumerate(turn or []):
        x = 196 + i * 23
        d.rectangle([x, 8, x + 16, 21], fill=col("Bayangan2"), outline=col(ring))
        d.ellipse([x + 3, 10, x + 13, 21], fill=col(skin), outline=col("Bayangan1"))
        d.rectangle([x + 3, 10, x + 13, 13], fill=col(hair))
    text(d, (196 + len(turn or []) * 23 + 4, 8), "> giliran", "Putih1")
    d.rectangle([0, 300, W, H], fill=C["Bayangan1"] + (190,))
    d.line([(0, 300), (W, 300)], fill=col("Emas1"))
    panel(d, 4, 304, 250, 355)
    for i, ln in enumerate(narr[:3]):
        text(d, (11, 309 + i * 14), ln[:39], "TeksUI" if i < len(narr) - 1 else "Putih1")
    tri(d, 240, 345, "TeksUI")
    for i, (n, hp, en, hpt) in enumerate(party):
        x = 256 + i * 96
        panel(d, x, 304, x + 92, 355, edge="Emas4" if i == active else "Emas1")
        text(d, (x + 6, 308), n, "Emas4" if i == active else "TeksUI")
        text(d, (x + 6, 320), hpt, "Putih1")
        bar(d, x + 6, 333, 80, 5, hp, "Nyawa", "Merah2")
        bar(d, x + 6, 342, 80, 4, en, "Energibatin", "Langit2")
    return ui, d


def float_menu(d, x, y, sel=0):
    slanted(d, [(x + 6, y), (x + 70, y), (x + 64, y + 52), (x, y + 52)], "Bayangan1", "Emas3", alpha=235)
    for i, t in enumerate(["Serang", "Jurus", "Pusaka", "Bertahan"]):
        yy = y + 5 + i * 12
        if i == sel:
            slanted(d, [(x + 6, yy - 1), (x + 66, yy - 1), (x + 65, yy + 9), (x + 4, yy + 9)], "Emas3")
            text(d, (x + 9, yy), "> " + t, "Bayangan1")
        else:
            text(d, (x + 15, yy), t, "TeksUI")


def enemy_bar(d, cx, y, frac, name):
    text(d, (cx - len(name) * 3, y - 11), name, "TeksUI", outline="Bayangan1")
    bar(d, cx - 20, y, 40, 3, frac, "Nyawa", "Merah2")


# =========================================================== scene 1: Hutan Beringin battle (day, sparse)
def scene_hutan():
    rnd = random.Random(11)
    hz = 132
    sky = Image.new("RGBA", (W, H), col("Langit4"))
    d = ImageDraw.Draw(sky)
    d.rectangle([0, 0, W, 60], fill=col("Langit5"))
    dither(sky, 0, 52, W, 64, "Langit4")
    far = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(far)
    for x in range(-20, W + 40, 18):
        r = rnd.randint(18, 34)
        d.ellipse([x - r, hz - 70 - r, x + r, hz - 70 + r * 0.8], fill=col("Langit3"))
    d.rectangle([0, hz - 70, W, hz + 4], fill=col("Langit3"))
    for x in range(-20, W + 40, 14):
        r = rnd.randint(12, 24)
        d.ellipse([x - r, hz - 40 - r, x + r, hz - 30 + r * 0.5], fill=col("Daun2"))
    d.rectangle([0, hz - 36, W, hz + 4], fill=col("Daun2"))
    for x in range(0, W, 9):
        d.line([(x, hz - 30), (x + rnd.randint(-2, 2), hz + 4)], fill=col("Daun1"), width=2)
    dither(far, 0, hz - 76, W, hz - 50, "Langit4", 4)
    mid = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(mid)
    beringin(d, 96, hz + 18, rnd, w=230, h=220)
    for x in (520, 590, 640):
        beringin(d, x, hz + 10, rnd, w=120, h=200, poleng=False)
    arca(d, 470, hz + 14, rnd)
    d.rectangle([548, hz - 6, 572, hz + 10], fill=col("Batu2"))
    meru(d, 560, hz - 4, rnd, tiers=2, w=34)
    play = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(play)
    d.rectangle([0, hz, W, H], fill=col("Daun3"))
    for i in range(14):
        t = i / 14
        y = hz + (H - hz) * t ** 1.4
        d.line([(0, y), (W, y)], fill=col("Daun2" if i % 2 else "Daun3"))
    d.ellipse([170, hz + 30, 490, hz + 150], fill=col("Tanah3"))
    d.ellipse([200, hz + 44, 460, hz + 136], fill=col("Tanah4"))
    dither(play, 160, hz + 28, 500, hz + 50, "Daun3", 4)
    for _ in range(260):
        x, y = rnd.randint(200, 460), rnd.randint(hz + 50, hz + 130)
        play.putpixel((x, y), col(rnd.choice(["Tanah3", "Tanah5", "Batu3"])))
    grass(play, rnd, hz + 2, H, 900, ("Daun4", "Daun5", "Daun6"))
    for _ in range(40):
        x, y = rnd.choice([rnd.randint(0, 150), rnd.randint(500, 640)]), rnd.randint(hz + 10, 290)
        d.ellipse([x, y, x + 5, y + 3], fill=col("Batu3"))
        d.point((x + 1, y), fill=col("Batu5"))
    nisan(d, 30, hz + 40, 1, rnd)
    sesajen(d, 470, hz + 22)
    sprites = []
    sprites.append((leak().render(), 110, 196, "float"))
    sprites.append((leak().render(), 170, 246, "float"))
    sprites.append((pengikut().render(), 92, 282, "ground"))
    party = [(pendekar(), 470, 214), (juru_mantra(), 522, 238), (peramu(), 466, 262), (pengintai(), 520, 288)]
    for spr, x, y in party:
        sprites.append((spr.render(mirror=True), x, y, "ground"))
    for im, x, y, kind in sorted(sprites, key=lambda s: s[2]):
        shadow(play, x, y + (14 if kind == "float" else 0), 12, 3)
        paste(play, im, x, y)
    front = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(front)
    for x in list(range(-30, 140, 26)) + list(range(560, 680, 26)):
        r = rnd.randint(18, 30)
        d.ellipse([x - r, 290 - r * 0.5, x + r, 310 + r], fill=col("Daun1"))
        d.ellipse([x - r * 0.6, 290 - r * 0.4, x + r * 0.2, 300], fill=col("Daun2"))
    layers = [sky, far, mid, play, front]
    check_palette(layers, "hutan")
    img = compose([up(sky, 4), up(far, 3), up(mid, 1.4), up(play), up(front, 5)])
    base = to_f(img)
    lights = [(300, 60, 260, (246, 227, 168), 0.35, 0.05), (110, 186, 22, (159, 146, 194), 0.6, 0.35),
              (170, 236, 22, (159, 146, 194), 0.6, 0.35), (522, 186, 16, (244, 239, 226), 0.5, 0.4)]
    out = light_pass(base, (0.92, 0.95, 0.88), lights)
    shafts = Image.new("L", (W * S, H * S), 0)
    sd = ImageDraw.Draw(shafts)
    for x0 in (220, 330, 470, 600, 760):
        sd.polygon([(x0 * 1.0, 0), (x0 + 60, 0), (x0 + 280, H * S * 0.75), (x0 + 170, H * S * 0.75)], fill=70)
    shafts = shafts.filter(ImageFilter.GaussianBlur(26))
    out += (np.asarray(shafts, dtype=np.float32) / 255.0)[..., None] * np.array([0.95, 0.9, 0.7], np.float32) * 0.55
    dust = np.zeros_like(out)
    for _ in range(70):
        x, y = rnd.randint(300, 1200), rnd.randint(40, 520)
        dust[y:y + 2, x:x + 2] = 0.8
    out += Image_blur(dust, 1.2) * np.array([1, 0.95, 0.8], np.float32)
    out = bloom(out, 0.7, 14, 0.8)
    out = grade(out, sat=0.8, tint=(0.97, 1.0, 0.95), lift=(0.01, 0.015, 0.02), vignette=0.5)
    final = finish(out)
    ui, d = battle_ui(["Leak melayang rendah dan mencakar",
                       "Peramu! Peramu menerima 9 kerusakan.",
                       "Giliran Pendekar."],
                      active=0,
                      party=[("Pendekar", 0.9, 1.0, "120/120"), ("Juru Mantra", 1.0, 0.7, "85/85"),
                             ("Peramu", 0.9, 0.9, "86/95"), ("Pengintai", 1.0, 0.6, "90/90")],
                      turn=[("Kulit3", "Merah3", "Emas4"), ("Kulit4", "Bayangan2", "Emas1"), ("Batu4", "Bayangan1", "Girah3"),
                            ("Kulit3", "Langit2", "Emas1"), ("Kulit3", "Putih2", "Emas1"), ("Kulit2", "Girah1", "Girah3"),
                            ("Batu4", "Bayangan1", "Girah3")],
                      ronde="RONDE 1")
    float_menu(d, 372, 168)
    tri(d, 470, 142, "Emas4")
    enemy_bar(d, 110, 202, 1.0, "Leak 1")
    enemy_bar(d, 170, 252, 0.7, "Leak 2")
    enemy_bar(d, 92, 286, 1.0, "Pengikut")
    final.alpha_composite(up(ui))
    return final, layers


def Image_blur(arr, r):
    im = Image.fromarray(np.clip(arr * 255, 0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(r))
    return np.asarray(im, dtype=np.float32) / 255.0


# =========================================================== scene 2: Pemakaman Candi Durga battle (night)
def scene_pemakaman():
    rnd = random.Random(5)
    hz = 128
    sky = Image.new("RGBA", (W, H), col("Langit1"))
    d = ImageDraw.Draw(sky)
    d.rectangle([0, 0, W, 50], fill=col("Bayangan1"))
    dither(sky, 0, 44, W, 56, "Langit1")
    for i in range(6):
        y = 30 + i * 14
        x0 = rnd.randint(-100, 400)
        d.ellipse([x0, y, x0 + rnd.randint(200, 360), y + 10], fill=col("Langit2"))
    far = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(far)
    cx = 300
    tiers = [(70, 0, 34), (58, 34, 56), (46, 56, 72), (34, 72, 86), (22, 86, 98), (10, 98, 112)]
    for hw, b, t in tiers:
        d.rectangle([cx - hw, hz - t, cx + hw, hz - b], fill=col("Bata1"))
        d.line([(cx - hw, hz - t), (cx + hw, hz - t)], fill=col("Bata2"))
    d.rectangle([cx - 4, hz - 120, cx + 4, hz - 112], fill=col("Bata2"))
    d.rectangle([cx - 10, hz - 26, cx + 10, hz], fill=col("Bayangan1"))
    for x in range(-30, W + 40, 22):
        if abs(x - cx) < 90:
            continue
        r = rnd.randint(14, 26)
        d.ellipse([x - r, hz - 30 - r, x + r, hz - 20 + r * 0.6], fill=col("Langit1"))
    d.rectangle([0, hz - 22, W, hz + 2], fill=col("Langit1"))
    dither(far, 0, hz - 60, W, hz - 10, "Langit2", 4)
    dither(far, 0, hz - 24, W, hz + 2, "Langit2", 2)
    mid = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(mid)
    beringin(d, 590, hz + 16, rnd, w=200, h=200)
    kamboja(d, 470, hz + 8, rnd, 1.1)
    kamboja(d, 40, hz + 12, rnd, 1.3)
    for i, x in enumerate(range(150, 460, 26)):
        nisan(d, x + rnd.randint(-4, 4), hz + 8 + (i % 2) * 4, rnd.randint(-2, 2), rnd)
    d.rectangle([28, hz + 4, 112, hz + 22], fill=col("Batu2"))
    d.rectangle([22, hz - 2, 118, hz + 4], fill=col("Batu3"))
    d.line([(22, hz - 2), (118, hz - 2)], fill=col("Batu5"))
    d.ellipse([46, hz - 8, 94, hz + 2], fill=col("Merah2"))
    d.ellipse([56, hz - 6, 84, hz], fill=col("Api1"))
    play = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(play)
    d.rectangle([0, hz, W, H], fill=col("Bayangan2"))
    for i in range(16):
        t = i / 16
        y = hz + (H - hz) * t ** 1.4
        d.line([(0, y), (W, y)], fill=col("Langit1" if i % 2 else "Bayangan2"))
    for _ in range(700):
        x, y = rnd.randint(0, W - 1), rnd.randint(hz + 1, H - 1)
        play.putpixel((x, y), col(rnd.choice(["Batu1", "Bayangan3", "Langit1", "Tanah1"])))
    grass(play, rnd, hz + 4, 170, 80, ("Daun2", "Daun3"))
    for side in ((0, 150), (510, 640)):
        for _ in range(4):
            nisan(d, rnd.randint(*side), rnd.randint(hz + 30, 270), rnd.randint(-3, 3), rnd, h=rnd.randint(14, 20))
    torches = [obor(d, 196, 196), obor(d, 404, 176), obor(d, 610, 268)]
    rx, ry = 150, 262
    for k, (a, b) in enumerate(((70, 16), (60, 13))):
        d.ellipse([rx - a, ry - b, rx + a, ry + b], outline=col("Merah3" if k == 0 else "Api1"))
    for a in range(0, 360, 20):
        d.point((rx + 66 * math.cos(math.radians(a)), ry + 14.5 * math.sin(math.radians(a))), fill=col("Api3"))
    sprites = [(calon_arang_ritual().render(shade=2), 150, 262, "ground", 30),
               (leak().render(), 70, 194, "float", 12), (pengikut().render(), 250, 292, "ground", 12),
               (pendekar().render(mirror=True), 462, 214, "ground", 12),
               (juru_mantra().render(mirror=True), 512, 238, "ground", 12),
               (peramu().render(mirror=True), 458, 262, "ground", 12),
               (pengintai().render(mirror=True), 512, 288, "ground", 12),
               (ratna().render(mirror=True), 594, 204, "ground", 11),
               (bharadah().render(mirror=True), 576, 236, "ground", 12)]
    for im, x, y, kind, r in sorted(sprites, key=lambda s: s[2]):
        shadow(play, x, y + (14 if kind == "float" else 0), r, 3)
        paste(play, im, x, y)
    front = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(front)
    for x in list(range(-30, 120, 24)) + list(range(560, 680, 24)):
        r = rnd.randint(16, 26)
        d.ellipse([x - r, 292 - r * 0.4, x + r, 312 + r], fill=col("Bayangan1"))
    d.line([(0, 296), (90, 284), (140, 300)], fill=col("Tanah1"), width=4)
    layers = [sky, far, mid, play, front]
    check_palette(layers, "pemakaman")
    img = compose([up(sky, 4), up(far, 3), up(mid, 1.4), up(play), up(front, 5)])
    base = to_f(img)
    fire = (232, 178, 94)
    lights = [(70, 116, 80, (201, 119, 60), 1.6, 0.6), (150, 266, 70, (176, 71, 58), 0.5, 0.12),
              (520, 170, 200, (117, 135, 154), 0.55, 0.0),
              (torches[0][0], torches[0][1], 70, fire, 1.5, 0.9), (torches[1][0], torches[1][1], 70, fire, 1.5, 0.9),
              (torches[2][0], torches[2][1], 70, fire, 1.5, 0.9), (70, 182, 18, (159, 146, 194), 1.0, 0.8),
              (150, 200, 40, (159, 146, 194), 0.6, 0.25), (512, 186, 26, (244, 239, 226), 0.7, 0.5)]
    out = light_pass(base, (0.5, 0.54, 0.72), lights)
    fog = np.zeros(out.shape[:2], np.float32)
    for _ in range(30):
        x, y = rnd.randint(-100, 1300), rnd.randint(380, 600)
        fog += radial(x, y, rnd.randint(120, 260), rnd.randint(30, 60)) * 0.35
    out = out * (1 - np.clip(fog, 0, 0.5)[..., None] * 0.3) + np.clip(fog, 0, 0.6)[..., None] * np.array([0.42, 0.37, 0.6], np.float32) * 0.5
    ash = np.zeros_like(out)
    for _ in range(90):
        x, y = rnd.randint(0, 1270), rnd.randint(0, 560)
        ash[y:y + 2, x:x + 2] = rnd.choice([0.5, 0.9])
    out += Image_blur(ash, 0.8) * np.array([0.95, 0.65, 0.4], np.float32)
    out = bloom(out, 0.55, 18, 1.1)
    out = grade(out, sat=0.85, tint=(0.95, 0.95, 1.05), lift=(0.01, 0.01, 0.03), vignette=0.7)
    final = finish(out)
    ui, d = battle_ui(["Calon Arang: \"Jika kalian ingin",
                       "menghentikanku, tunjukkan kekuatan!\"",
                       "Giliran Juru Mantra."],
                      active=1,
                      party=[("Pendekar", 0.62, 0.4, "74/120"), ("Juru Mantra", 0.8, 0.9, "68/85"),
                             ("Peramu", 0.45, 0.5, "43/95"), ("Pengintai", 0.7, 0.3, "63/90")],
                      turn=[("Kulit4", "Putih2", "Emas4"), ("Kulit3", "Putih2", "Girah3"), ("Kulit3", "Merah3", "Emas1"),
                            ("Batu4", "Bayangan1", "Girah3"), ("Kulit3", "Langit2", "Emas1"), ("Kulit2", "Girah1", "Girah3")],
                      ronde="RONDE 3 DARI 6")
    float_menu(d, 360, 150, sel=1)
    tri(d, 512, 168, "Emas4")
    enemy_bar(d, 150, 268, 0.55, "Calon Arang")
    enemy_bar(d, 70, 200, 0.8, "Leak")
    enemy_bar(d, 250, 296, 1.0, "Pengikut")
    final.alpha_composite(up(ui))
    return final, layers


# =========================================================== scene 3: Desa Girah story (dusk, busy) + Persona dialog
def ratna_portrait():
    s = Spr(128, 192)
    s.ellipse(60, 96, 36, 72, "hair")
    s.ellipse(36, 42, 13, 13, "hair")
    s.poly([(54, 92), (72, 92), (74, 112), (52, 112)], "skin")
    s.poly([(48, 106), (78, 106), (110, 122), (116, 132), (8, 132), (16, 122)], "skinA")
    s.poly([(14, 128), (110, 128), (122, 192), (2, 192)], "white")
    s.rect(14, 128, 110, 131, "gold")
    s.poly([(10, 124), (38, 114), (84, 192), (52, 192)], "mauve")
    s.ellipse(62, 66, 25, 31, "skin")
    s.ellipse(58, 50, 29, 19, "hair")
    s.poly([(36, 52), (88, 50), (86, 60), (70, 56), (52, 60), (38, 62)], "hair")
    s.line(26, 38, 44, 32, "gold", 2)
    for fx, fy in ((24, 36), (30, 32)):
        s.ellipse(fx, fy, 2.5, 2.5, "flowerW"); s.put(fx, fy, "flowerC")
    s.rect(52, 112, 72, 113, "gold")
    for ex in (50, 70):
        s.rect(ex, 70, ex + 7, 73, "eyeW")
        s.rect(ex + 3, 71, ex + 6, 73, "eye")
        s.rect(ex - 1, 69, ex + 8, 69, "lid")
    s.line(49, 65, 57, 62, "brow", 2)
    s.line(70, 62, 78, 65, "brow", 2)
    s.rect(64, 79, 66, 81, "nose")
    s.line(59, 88, 70, 88, "mouth"); s.put(58, 89, "mouth"); s.put(71, 89, "mouth")
    return s


def scene_girah():
    rnd = random.Random(21)
    hz = 120
    sky = Image.new("RGBA", (W, H), col("Senja3"))
    d = ImageDraw.Draw(sky)
    bands = [(0, "Langit2"), (26, "Senja1"), (52, "Senja2"), (80, "Senja3"), (104, "Senja4")]
    for i, (y, c) in enumerate(bands):
        y1 = bands[i + 1][0] if i + 1 < len(bands) else hz
        d.rectangle([0, y, W, y1], fill=col(c))
        if i:
            dither(sky, 0, y - 5, W, y, c)
    for i in range(5):
        y = 20 + i * 15
        x0 = rnd.randint(-100, 480)
        d.ellipse([x0, y, x0 + rnd.randint(160, 300), y + 8], fill=col(rnd.choice(["Senja2", "Senja3"])))
    far = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(far)
    d.polygon([(380, hz - 10), (480, 34), (500, 30), (520, 36), (640, hz - 30), (640, hz), (380, hz)], fill=col("Senja2"))
    d.polygon([(470, 40), (480, 34), (500, 30), (520, 36), (530, 44), (500, 50)], fill=col("Senja3"))
    d.polygon([(-20, hz), (120, hz - 40), (260, hz - 20), (300, hz)], fill=col("Senja2"))
    for k in range(5):
        y = hz - 22 + k * 5
        d.line([(0, y), (W, y + rnd.randint(-2, 2))], fill=col("Senja1" if k % 2 else "Senja2"))
    for x in (40, 150, 560, 610):
        kelapa(d, x, hz + 4, 70, rnd.randint(-8, 8), rnd)
    dither(far, 0, hz - 30, W, hz + 4, "Senja3", 4)
    mid = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(mid)
    meru(d, 520, hz + 10, rnd, tiers=5, w=60)
    meru(d, 90, hz + 6, rnd, tiers=3, w=44)
    candi_bentar(d, 320, hz + 20, 92, 34, rnd, gap=26)
    for x0, x1, base, rh in ((180, 262, hz + 26, 54), (378, 470, hz + 26, 56)):
        bale(d, x0, x1, base, rh, rnd)
    play = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(play)
    d.rectangle([0, hz + 18, W, H], fill=col("Daun2"))
    for i in range(12):
        t = i / 12
        y = hz + 18 + (H - hz - 18) * t ** 1.4
        d.line([(0, y), (W, y)], fill=col("Daun3" if i % 2 else "Daun2"))
    for _ in range(14):
        x, y = rnd.randint(0, W), rnd.randint(hz + 30, H)
        r = rnd.randint(10, 30)
        d.ellipse([x - r, y - r * 0.3, x + r, y + r * 0.3], fill=col("Tanah2"))
    grass(play, rnd, hz + 20, H, 1400, ("Daun3", "Daun4", "Daun5"))
    for _ in range(60):
        x, y = rnd.randint(0, W), rnd.randint(hz + 24, H)
        if 150 < x < 470:
            continue
        play.putpixel((x, y), col(rnd.choice(["Putih3", "Merah4", "Emas4"])))
    vx, vy = 320, hz + 20
    pl, pr = 300, 340
    for i in range(18):
        t0, t1 = (i / 18) ** 1.5, ((i + 1) / 18) ** 1.5
        y0, y1 = vy + (H - vy) * t0, vy + (H - vy) * t1
        xl0, xr0 = pl - (pl - 170) * t0, pr + (450 - pr) * t0
        xl1, xr1 = pl - (pl - 170) * t1, pr + (450 - pr) * t1
        d.polygon([(xl0, y0), (xr0, y0), (xr1, y1), (xl1, y1)], fill=col("Batu3" if i % 2 else "Batu4"))
        n = 5 + i // 3
        for k in range(1, n):
            xa = xl0 + (xr0 - xl0) * k / n + (3 if i % 2 else 0)
            xb = xl1 + (xr1 - xl1) * k / n + (3 if i % 2 else 0)
            d.line([(xa, y0), (xb, y1)], fill=col("Batu2"))
        d.line([(xl0, y0), (xr0, y0)], fill=col("Batu2"))
    for side in (-1, 1):
        pts_in, pts_out = [], []
        for i in range(0, 21):
            t = (i / 20) ** 1.5
            y = vy + (H - vy) * t
            xin = (pl - (pl - 170) * t) if side < 0 else (pr + (450 - pr) * t)
            w_, h_ = 3 + 16 * t, 2 + 12 * t
            pts_in.append((xin + side * 1, y))
            pts_out.append((xin + side * w_, y - h_))
        poly = pts_in + pts_out[::-1]
        d.polygon(poly, fill=col("Bata3"))
        d.line(pts_out, fill=col("Bata5"))
        for i in range(1, 20, 1):
            (ax, ay), (bx, by) = pts_in[i], pts_out[i]
            d.line([(ax, ay), (bx, by)], fill=col("Bata1"))
        for k in (0.35, 0.7):
            d.line([(a[0] + (b[0] - a[0]) * k, a[1] + (b[1] - a[1]) * k) for a, b in zip(pts_in, pts_out)],
                   fill=col("Bata2"))
    for x, base in ((214, hz + 36), (240, hz + 50), (420, hz + 40), (452, hz + 54)):
        tempayan(d, x, base)
    for x, base in ((262, hz + 30), (370, hz + 30), (280, hz + 46), (360, hz + 46)):
        sesajen(d, x, base)
    lamps = []
    for x, base, h, bend in ((250, hz + 70, 120, 46), (396, hz + 72, 120, -46), (186, hz + 120, 150, 60)):
        penjor(d, x, base, h, bend, rnd)
    kamboja(d, 124, hz + 116, rnd, 1.6)
    kamboja(d, 530, hz + 120, rnd, 1.5)
    for x, base in ((160, hz + 64), (476, hz + 66)):
        lamps.append(obor(d, x, base, 30))
    sprites = []
    for i, (top, bot, hair, carry, x, y) in enumerate((
            ("white", "red", "bun", True, 212, hz + 60), ("dark", "wood", "short", False, 444, hz + 62),
            ("mauve", "green", "bun", False, 560, hz + 96), ("whiteD", "dark", "short", False, 90, hz + 98),
            ("red", "white", "short", False, 482, hz + 100))):
        sprites.append((npc(top, bot, hair, carry).render(mirror=(x > 320)), x, y, 9))
    sprites.append((ratna().render(), 352, hz + 118, 12))
    for spr, x, y in ((pendekar(), 282, hz + 132), (juru_mantra(), 250, hz + 150), (peramu(), 300, hz + 170),
                      (pengintai(), 236, hz + 186)):
        sprites.append((spr.render(), x, y, 12))
    for im, x, y, r in sorted(sprites, key=lambda s: s[2]):
        shadow(play, x, y, r, 3)
        paste(play, im, x, y)
    front = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(front)
    for x in list(range(-20, 110, 22)) + list(range(560, 680, 22)):
        r = rnd.randint(16, 28)
        d.ellipse([x - r, 300 - r * 0.4, x + r, 320 + r], fill=col("Daun1"))
        d.ellipse([x - r * 0.5, 300 - r * 0.3, x + r * 0.2, 308], fill=col("Daun2"))
    layers = [sky, far, mid, play, front]
    check_palette(layers, "girah")
    img = compose([up(sky, 3), up(far, 2.4), up(mid, 1.0), up(play), up(front, 5)])
    base = to_f(img)
    fire = (232, 178, 94)
    lights = [(lamps[0][0], lamps[0][1], 60, fire, 1.3, 0.8), (lamps[1][0], lamps[1][1], 60, fire, 1.3, 0.8),
              (320, 60, 360, (176, 143, 149), 0.25, 0.08)]
    out = light_pass(base, (0.78, 0.74, 0.84), lights)
    fl = np.zeros_like(out)
    for _ in range(40):
        x, y = rnd.randint(0, 1270), rnd.randint(250, 640)
        fl[y:y + 2, x:x + 2] = 1.0
    out += Image_blur(fl, 1.5) * np.array([0.9, 0.95, 0.6], np.float32) * 1.4
    out = bloom(out, 0.62, 16, 0.9)
    out = grade(out, sat=0.82, tint=(1.0, 0.96, 1.02), lift=(0.02, 0.01, 0.03), vignette=0.55)
    return out, layers


def story_ui(dialog=True):
    ui = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(ui)
    slanted(d, [(8, 8), (232, 8), (224, 24), (0, 24)], "Merah2", "Merah4")
    text(d, (14, 12), "BABAK 2 - LUKA RATNA MANGGALI")
    panel(d, 420, 6, 634, 22)
    text(d, (427, 9), "Misi: Penawar untuk Dua Desa", "Emas4")
    if dialog:
        portrait = ratna_portrait().render(shade=3)
        ui.alpha_composite(portrait, (8, H - 192 + 2))
        slanted(d, [(126, 272), (636, 262), (632, 356), (118, 358)], "Bayangan1", "TeksUI", alpha=238)
        d.line([(134, 278), (320, 274)], fill=col("Merah3"), width=2)
        slanted(d, [(150, 254), (296, 250), (292, 268), (146, 272)], "Emas3", "Bayangan1")
        text(d, (160, 256), "Ratna Manggali", "Bayangan1")
        text(d, (150, 288), "\"Mereka takut kepada Ibu. Lama-kelamaan,")
        text(d, (150, 304), "mereka takut kepadaku juga.\"")
        tri(d, 612, 340, "TeksUI")
    return ui


def finish_story(out, dim=True):
    if dim:
        out = out * 0.82
    final = finish(out)
    if dim:
        bl = final.filter(ImageFilter.GaussianBlur(1.2))
        final = Image.blend(final, bl, 0.5)
    final.alpha_composite(up(story_ui(dialog=dim)))
    return final


def save(img, name):
    img.convert("RGB").save(OUT / f"{name}.png")


if __name__ == "__main__":
    OUT = Path(sys.argv[1])
    OUT.mkdir(parents=True, exist_ok=True)
    hutan, _ = scene_hutan()
    save(hutan, "mockup-battle-hutan")
    pem, _ = scene_pemakaman()
    save(pem, "mockup-battle-pemakaman")
    gir, _ = scene_girah()
    save(finish_story(gir, dim=True), "mockup-adegan")
    save(finish_story(gir, dim=False), "mockup-adegan-tanpa-dialog")
    print("ok")
