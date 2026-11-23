"""Generate PLACEHOLDER assets for the 2.5D arena prototype.

Writes, for each arena, parallax layers + character sprites + automatic normal maps + arena.json.
All art comes from pixelkit.py (palette-only procedural pixel art). Replace any PNG with real
art of the same name; re-run tools/make_normals.py (or Laigter) to rebuild its *_n.png.

Usage (from prototype-2.5d/):  python tools/gen_assets.py
"""
import json
import random
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import pixelkit as pk  # noqa: E402
from make_normals import normal_map  # noqa: E402

ASSETS = HERE.parent.parent / "assets"   # assets/ di root repo
LW, LH = 720, 380          # layer canvas: 640x360 view + 40px / 10px slack for parallax
OX, OY = 40, 10            # where the 640x360 view starts inside a layer canvas
pk.W, pk.H = LW, LH        # let pixelkit helpers (grass, dither bounds) cover the whole canvas


def canvas():
    return Image.new("RGBA", (LW, LH), (0, 0, 0, 0))


def save(img, path, normals=False, strength=2.2):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    if normals:
        normal_map(img, strength).save(path.with_name(path.stem + "_n.png"))


def pole(d, x, base, h=34):
    """Torch pole without flame: the flame and its light are animated in Java."""
    d.line([(x, base), (x, base - h)], fill=pk.col("Jerami1"), width=2)
    d.line([(x - 1, base), (x - 1, base - h)], fill=pk.col("Jerami2"))
    d.rectangle([x - 2, base - h - 2, x + 2, base - h + 2], fill=pk.col("Tanah2"))
    return x, base - h - 4


# ------------------------------------------------------------------ sprites (shared)
def sprites():
    out = ASSETS / "sprites"
    spec = {
        "pendekar": (pk.pendekar(), True, 1), "juru_mantra": (pk.juru_mantra(), True, 1),
        "peramu": (pk.peramu(), True, 1), "pengintai": (pk.pengintai(), True, 1),
        "ratna": (pk.ratna(), True, 1), "bharadah": (pk.bharadah(), True, 1),
        "leak": (pk.leak(), False, 1), "pengikut": (pk.pengikut(), False, 1),
        "calon_arang_ritual": (pk.calon_arang_ritual(), False, 2),
    }
    for name, (spr, mirror, shade) in spec.items():
        save(spr.render(mirror=mirror, shade=shade), out / f"{name}.png", normals=True)


# ------------------------------------------------------------------ arena: Hutan Beringin
def arena_hutan():
    rnd = random.Random(11)
    out = ASSETS / "arena" / "hutan"
    hz = 132 + OY

    for name, bands in (("sky_day", [(0, "Langit5"), (70, "Langit4")]),
                        ("sky_night", [(0, "Bayangan1"), (60, "Langit1"), (120, "Langit2")])):
        sky = Image.new("RGBA", (LW, LH), pk.col(bands[0][1]))
        d = ImageDraw.Draw(sky)
        for i, (y, c) in enumerate(bands):
            d.rectangle([0, y, LW, LH], fill=pk.col(c))
            if i:
                pk.dither(sky, 0, y - 8, LW, y, c)
        save(sky, out / f"{name}.png")

    far = canvas()
    d = ImageDraw.Draw(far)
    for x in range(-20, LW + 40, 18):
        r = rnd.randint(18, 34)
        d.ellipse([x - r, hz - 70 - r, x + r, hz - 70 + r * 0.8], fill=pk.col("Langit3"))
    d.rectangle([0, hz - 70, LW, hz + 4], fill=pk.col("Langit3"))
    for x in range(-20, LW + 40, 14):
        r = rnd.randint(12, 24)
        d.ellipse([x - r, hz - 40 - r, x + r, hz - 30 + r * 0.5], fill=pk.col("Daun2"))
    d.rectangle([0, hz - 36, LW, hz + 4], fill=pk.col("Daun2"))
    for x in range(0, LW, 9):
        d.line([(x, hz - 30), (x + rnd.randint(-2, 2), hz + 4)], fill=pk.col("Daun1"), width=2)
    pk.dither(far, 0, hz - 76, LW, hz - 50, "Langit4", 4)
    save(far, out / "far.png")

    mid = canvas()
    d = ImageDraw.Draw(mid)
    pk.beringin(d, 96 + OX, hz + 18, rnd, w=230, h=220)
    for x in (520, 590, 650, 700):
        pk.beringin(d, x + OX, hz + 10, rnd, w=120, h=200, poleng=False)
    pk.arca(d, 470 + OX, hz + 14, rnd)
    pk.meru(d, 560 + OX, hz - 4, rnd, tiers=2, w=34)
    save(mid, out / "mid.png", normals=True, strength=1.6)

    ground = canvas()
    d = ImageDraw.Draw(ground)
    d.rectangle([0, hz, LW, LH], fill=pk.col("Daun3"))
    for i in range(16):
        t = i / 16
        y = hz + (LH - hz) * t ** 1.4
        d.line([(0, y), (LW, y)], fill=pk.col("Daun2" if i % 2 else "Daun3"))
    d.ellipse([170 + OX, hz + 30, 490 + OX, hz + 150], fill=pk.col("Tanah3"))
    d.ellipse([200 + OX, hz + 44, 460 + OX, hz + 136], fill=pk.col("Tanah4"))
    pk.dither(ground, 160 + OX, hz + 28, 500 + OX, hz + 50, "Daun3", 4)
    for _ in range(300):
        x, y = rnd.randint(200, 460) + OX, rnd.randint(hz + 50, hz + 130)
        ground.putpixel((x, y), pk.col(rnd.choice(["Tanah3", "Tanah5", "Batu3"])))
    pk.grass(ground, rnd, hz + 2, LH, 1100, ("Daun4", "Daun5", "Daun6"))
    for _ in range(46):
        x, y = rnd.choice([rnd.randint(0, 190), rnd.randint(540, LW)]), rnd.randint(hz + 10, 300)
        d.ellipse([x, y, x + 5, y + 3], fill=pk.col("Batu3"))
        d.point((x + 1, y), fill=pk.col("Batu5"))
    pk.nisan(d, 30 + OX, hz + 40, 1, rnd)
    pk.sesajen(d, 470 + OX, hz + 22)
    torches = [pole(d, 214 + OX, hz + 36), pole(d, 600 + OX, hz + 30), pole(d, 28 + OX, hz + 120)]
    save(ground, out / "ground.png", normals=True, strength=0.8)

    front = canvas()
    d = ImageDraw.Draw(front)
    for x in list(range(-30, 190, 26)) + list(range(600, 760, 26)):
        r = rnd.randint(18, 32)
        d.ellipse([x - r, 300 + OY - r * 0.5, x + r, 320 + OY + r], fill=pk.col("Daun1"))
        d.ellipse([x - r * 0.6, 300 + OY - r * 0.4, x + r * 0.2, 310 + OY], fill=pk.col("Daun2"))
    save(front, out / "front.png", normals=True, strength=1.2)

    data = {
        "name": "Tanah lapang Hutan Beringin",
        "modes": {
            "day": {"sky": "sky_day.png", "ambient": [0.95, 0.95, 0.88], "sun": [0.55, 0.5, 0.35],
                    "sunDir": [-0.5, 0.6, 0.6], "torches": False, "shafts": True, "particles": "dust",
                    "grade": {"sat": 0.82, "tint": [0.98, 1.0, 0.95], "lift": [0.01, 0.015, 0.02], "vignette": 0.45}},
            "night": {"sky": "sky_night.png", "ambient": [0.28, 0.31, 0.46], "sun": [0.12, 0.14, 0.24],
                      "sunDir": [0.2, 0.7, 0.7], "torches": True, "shafts": False, "particles": "fireflies",
                      "grade": {"sat": 0.85, "tint": [0.95, 0.95, 1.05], "lift": [0.01, 0.01, 0.03], "vignette": 0.65}},
        },
        "startMode": "night",
        "layers": [
            {"file": "far.png", "parallax": 0.35, "normal": False, "lit": 0.6},
            {"file": "mid.png", "parallax": 0.7, "normal": True, "lit": 1.0},
            {"file": "ground.png", "parallax": 1.0, "normal": True, "lit": 1.0},
        ],
        "front": {"file": "front.png", "parallax": 1.35, "normal": True},
        "torches": [{"x": x - OX, "y": y - OY} for x, y in torches],
        "actors": [
            {"id": "leak1", "name": "Leak 1", "sprite": "leak", "x": 110, "y": 196, "side": "girah", "float": True, "eyes": [36, 22, 39, 22]},
            {"id": "leak2", "name": "Leak 2", "sprite": "leak", "x": 170, "y": 246, "side": "girah", "float": True, "eyes": [36, 22, 39, 22]},
            {"id": "pengikut", "name": "Pengikut", "sprite": "pengikut", "x": 92, "y": 282, "side": "girah"},
            {"id": "pendekar", "name": "Pendekar", "sprite": "pendekar", "x": 470, "y": 214, "side": "pengelana"},
            {"id": "juru_mantra", "name": "Juru Mantra", "sprite": "juru_mantra", "x": 522, "y": 238, "side": "pengelana", "staff": [19, 9]},
            {"id": "peramu", "name": "Peramu", "sprite": "peramu", "x": 466, "y": 262, "side": "pengelana"},
            {"id": "pengintai", "name": "Pengintai", "sprite": "pengintai", "x": 520, "y": 288, "side": "pengelana"},
        ],
    }
    (out / "arena.json").write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")


# ------------------------------------------------------------------ arena: Pemakaman Candi Durga
def arena_pemakaman():
    rnd = random.Random(5)
    out = ASSETS / "arena" / "pemakaman"
    hz = 128 + OY
    sky = Image.new("RGBA", (LW, LH), pk.col("Langit1"))
    d = ImageDraw.Draw(sky)
    d.rectangle([0, 0, LW, 60], fill=pk.col("Bayangan1"))
    pk.dither(sky, 0, 54, LW, 66, "Langit1")
    for i in range(7):
        y = 34 + i * 14
        x0 = rnd.randint(-100, 480)
        d.ellipse([x0, y, x0 + rnd.randint(200, 360), y + 10], fill=pk.col("Langit2"))
    save(sky, out / "sky_night.png")

    far = canvas()
    d = ImageDraw.Draw(far)
    cx = 300 + OX
    for hw, b, t in [(70, 0, 34), (58, 34, 56), (46, 56, 72), (34, 72, 86), (22, 86, 98), (10, 98, 112)]:
        d.rectangle([cx - hw, hz - t, cx + hw, hz - b], fill=pk.col("Bata1"))
        d.line([(cx - hw, hz - t), (cx + hw, hz - t)], fill=pk.col("Bata2"))
    d.rectangle([cx - 4, hz - 120, cx + 4, hz - 112], fill=pk.col("Bata2"))
    d.rectangle([cx - 10, hz - 26, cx + 10, hz], fill=pk.col("Bayangan1"))
    for x in range(-30, LW + 40, 22):
        if abs(x - cx) < 90:
            continue
        r = rnd.randint(14, 26)
        d.ellipse([x - r, hz - 30 - r, x + r, hz - 20 + r * 0.6], fill=pk.col("Langit1"))
    d.rectangle([0, hz - 22, LW, hz + 2], fill=pk.col("Langit1"))
    pk.dither(far, 0, hz - 60, LW, hz - 10, "Langit2", 4)
    pk.dither(far, 0, hz - 24, LW, hz + 2, "Langit2", 2)
    save(far, out / "far.png")

    mid = canvas()
    d = ImageDraw.Draw(mid)
    pk.beringin(d, 600 + OX, hz + 16, rnd, w=200, h=200)
    pk.kamboja(d, 470 + OX, hz + 8, rnd, 1.1)
    pk.kamboja(d, 40 + OX, hz + 12, rnd, 1.3)
    for i, x in enumerate(range(150, 460, 26)):
        pk.nisan(d, x + OX + rnd.randint(-4, 4), hz + 8 + (i % 2) * 4, rnd.randint(-2, 2), rnd)
    d.rectangle([28 + OX, hz + 4, 112 + OX, hz + 22], fill=pk.col("Batu2"))
    d.rectangle([22 + OX, hz - 2, 118 + OX, hz + 4], fill=pk.col("Batu3"))
    d.line([(22 + OX, hz - 2), (118 + OX, hz - 2)], fill=pk.col("Batu5"))
    d.ellipse([46 + OX, hz - 8, 94 + OX, hz + 2], fill=pk.col("Merah2"))
    d.ellipse([56 + OX, hz - 6, 84 + OX, hz], fill=pk.col("Api1"))
    save(mid, out / "mid.png", normals=True, strength=1.6)

    ground = canvas()
    d = ImageDraw.Draw(ground)
    d.rectangle([0, hz, LW, LH], fill=pk.col("Bayangan2"))
    for i in range(18):
        t = i / 18
        y = hz + (LH - hz) * t ** 1.4
        d.line([(0, y), (LW, y)], fill=pk.col("Langit1" if i % 2 else "Bayangan2"))
    for _ in range(900):
        x, y = rnd.randint(0, LW - 1), rnd.randint(hz + 1, LH - 1)
        ground.putpixel((x, y), pk.col(rnd.choice(["Batu1", "Bayangan3", "Langit1", "Tanah1"])))
    pk.grass(ground, rnd, hz + 4, 180, 100, ("Daun2", "Daun3"))
    for side in ((0, 190), (550, LW)):
        for _ in range(5):
            pk.nisan(d, rnd.randint(*side), rnd.randint(hz + 30, 290), rnd.randint(-3, 3), rnd, h=rnd.randint(14, 20))
    torches = [pole(d, 196 + OX, 196 + OY), pole(d, 404 + OX, 176 + OY), pole(d, 610 + OX, 268 + OY)]
    rx, ry = 150 + OX, 262 + OY
    for k, (a, b) in enumerate(((70, 16), (60, 13))):
        d.ellipse([rx - a, ry - b, rx + a, ry + b], outline=pk.col("Merah3" if k == 0 else "Api1"))
    save(ground, out / "ground.png", normals=True, strength=0.8)

    front = canvas()
    d = ImageDraw.Draw(front)
    for x in list(range(-30, 170, 24)) + list(range(600, 760, 24)):
        r = rnd.randint(16, 26)
        d.ellipse([x - r, 302 + OY - r * 0.4, x + r, 322 + OY + r], fill=pk.col("Bayangan1"))
    save(front, out / "front.png", normals=True, strength=1.2)

    data = {
        "name": "Pelataran pemakaman Candi Durga",
        "modes": {
            "night": {"sky": "sky_night.png", "ambient": [0.3, 0.32, 0.48], "sun": [0.1, 0.12, 0.22],
                      "sunDir": [0.2, 0.7, 0.7], "torches": True, "shafts": False, "particles": "ash",
                      "grade": {"sat": 0.85, "tint": [0.95, 0.95, 1.05], "lift": [0.01, 0.01, 0.03], "vignette": 0.7}},
        },
        "startMode": "night",
        "layers": [
            {"file": "far.png", "parallax": 0.35, "normal": False, "lit": 0.95},
            {"file": "mid.png", "parallax": 0.7, "normal": True, "lit": 1.0},
            {"file": "ground.png", "parallax": 1.0, "normal": True, "lit": 1.0},
        ],
        "front": {"file": "front.png", "parallax": 1.35, "normal": True},
        "torches": [{"x": x - OX, "y": y - OY} for x, y in torches],
        "staticLights": [
            {"x": 70, "y": 120, "z": 30, "color": [0.79, 0.47, 0.24], "radius": 110, "intensity": 1.6, "flicker": 0.15},
            {"x": 150, "y": 262, "z": 20, "color": [0.69, 0.28, 0.23], "radius": 90, "intensity": 0.9, "flicker": 0.25},
        ],
        "actors": [
            {"id": "calon_arang", "name": "Calon Arang", "sprite": "calon_arang_ritual", "x": 150, "y": 262, "side": "girah", "boss": True, "eyes": [62, 38, 72, 38]},
            {"id": "leak", "name": "Leak", "sprite": "leak", "x": 70, "y": 194, "side": "girah", "float": True, "eyes": [36, 22, 39, 22]},
            {"id": "pengikut", "name": "Pengikut", "sprite": "pengikut", "x": 250, "y": 292, "side": "girah"},
            {"id": "pendekar", "name": "Pendekar", "sprite": "pendekar", "x": 462, "y": 214, "side": "pengelana"},
            {"id": "juru_mantra", "name": "Juru Mantra", "sprite": "juru_mantra", "x": 512, "y": 238, "side": "pengelana", "staff": [19, 9]},
            {"id": "peramu", "name": "Peramu", "sprite": "peramu", "x": 458, "y": 262, "side": "pengelana"},
            {"id": "pengintai", "name": "Pengintai", "sprite": "pengintai", "x": 512, "y": 288, "side": "pengelana"},
            {"id": "ratna", "name": "Ratna", "sprite": "ratna", "x": 594, "y": 204, "side": "sekutu"},
            {"id": "bharadah", "name": "Mpu Bharadah", "sprite": "bharadah", "x": 576, "y": 236, "side": "sekutu"},
        ],
    }
    (out / "arena.json").write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")


if __name__ == "__main__":
    sprites()
    arena_hutan()
    arena_pemakaman()
    print("assets written to", ASSETS)
