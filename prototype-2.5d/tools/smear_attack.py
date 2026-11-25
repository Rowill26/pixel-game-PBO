"""Animasi serang dengan smear frame dan waktu per frame (game feel), untuk semua tokoh.

python tools/smear_attack.py <folder pose> <nama sprite> --ancang ancang.png --strike ayun.png
       [--idle idle.png] --kind arc|claw|thrust|none [opsi jejak] [--install assets/sprites]

Urutan frame: ancang (antisipasi, ditahan) -> smear (pose serang + jejak senjata, sangat cepat)
-> serang (benturan, ditahan) -> serang maju --follow px (follow-through) -> idle (pulih).
Menulis <folder>/anim/serang_*.png + serang_timing.json; dengan --install juga menyalinnya ke
assets/sprites/<nama>_serang_*.png, membuat normal map, dan menghapus frame serang lama yang
tidak terpakai.

Jenis jejak (koordinat = pixel kanvas sprite, sesudah dicerminkan bila tokohnya pengelana):
  arc     lengkung ayunan senjata mengelilingi --pivot dari --from ke --to; --sweep 1 = searah
          jarum jam di layar, -1 = berlawanan (pilih yang lewat jalur ayunan sebenarnya)
  claw    seperti arc, tetapi tiga goresan sejajar (kuku)
  thrust  garis kecepatan di belakang --tip sepanjang --length; --dir-x = arah serangan (-1 kiri)
  none    tanpa jejak, hanya waktu per frame (serangan sihir)
"""
import argparse
import json
import math
import subprocess
import sys
from pathlib import Path

from PIL import Image

COLORS = {   # dari ekor ke kepala jejak, semua dari palet 64 warna
    "steel": ["878d87", "aeb1a5", "e3dccb", "f4efe2"],
    "wood": ["7a5f45", "987c5a", "c2ad7d", "e3dccb"],
    "copper": ["84483a", "a2634c", "bf8667", "f6e3a8"],
    "claw": ["8c2f2e", "b0473a", "e3dccb", "f4efe2"],
    "magic": ["4f416c", "73639a", "9f92c2", "f4efe2"],
    "ghost": ["4f416c", "73639a", "9f92c2", "e3dccb"],
}


def xy(s):
    a, b = s.split(",")
    return float(a), float(b)


def put(img, x, y, hexcol):
    x, y = round(x), round(y)
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), tuple(bytes.fromhex(hexcol)) + (255,))


def arc_trail(img, a, cols, offsets):
    px, py = a.pivot
    a0 = math.atan2(a.frm[1] - py, a.frm[0] - px)
    r0 = math.hypot(a.frm[0] - px, a.frm[1] - py)
    a1 = math.atan2(a.to[1] - py, a.to[0] - px)
    r1 = math.hypot(a.to[0] - px, a.to[1] - py)
    if a.sweep > 0 and a1 < a0:
        a1 += 2 * math.pi
    if a.sweep < 0 and a1 > a0:
        a1 -= 2 * math.pi
    steps = 160
    for off in offsets:
        for i in range(steps + 1):
            t = i / steps                       # 0 = ekor (posisi ancang), 1 = kepala (posisi serang)
            if t < a.start:
                continue
            if t < 0.3 and i % 2:               # ekor putus-putus: terasa memudar
                continue
            ang = a0 + (a1 - a0) * t
            r = r0 + (r1 - r0) * t + off
            width = 1 + int(a.width * t)        # menebal ke arah kepala jejak
            col = cols[min(len(cols) - 1, int(t * len(cols)))]
            for w in range(width):
                rr = r - w * 0.9
                put(img, px + math.cos(ang) * rr, py + math.sin(ang) * rr, col)


def thrust_trail(img, a, cols):
    tx, ty = a.tip
    back = -a.dir_x                              # garis memanjang ke belakang serangan
    for row, frac in ((-2, 0.6), (0, 1.0), (2, 0.75), (-4, 0.35), (4, 0.4)):
        length = int(a.length * frac)
        start = 3 + abs(row)                     # mulai sedikit di belakang ujung senjata
        for i in range(length):
            t = 1 - i / max(1, length)           # 1 dekat ujung, 0 di ekor
            if t < 0.35 and i % 2:
                continue
            col = cols[min(len(cols) - 1, int(t * len(cols)))]
            put(img, tx + back * (start + i), ty + row, col)


def shift(im, dx, dy=0):
    out = Image.new("RGBA", im.size)
    out.alpha_composite(im, (dx, dy))
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("folder")
    ap.add_argument("name")
    ap.add_argument("--ancang", required=True)
    ap.add_argument("--strike", required=True)
    ap.add_argument("--idle", default="idle.png")
    ap.add_argument("--kind", choices=["arc", "claw", "thrust", "none"], required=True)
    ap.add_argument("--pivot", type=xy)
    ap.add_argument("--from", dest="frm", type=xy)
    ap.add_argument("--to", type=xy)
    ap.add_argument("--sweep", type=int, default=1)
    ap.add_argument("--width", type=float, default=2.5)
    ap.add_argument("--start", type=float, default=0.0, help="bagian awal lengkung yang dilewati (0-1)")
    ap.add_argument("--tip", type=xy)
    ap.add_argument("--dir-x", type=int, default=-1)
    ap.add_argument("--length", type=int, default=16)
    ap.add_argument("--colors", default="steel", choices=sorted(COLORS))
    ap.add_argument("--follow", type=int, default=-1, help="geser px frame follow-through (arah serangan)")
    ap.add_argument("--durations", default="0.16,0.05,0.12,0.08,0.10")
    ap.add_argument("--install")
    a = ap.parse_args()

    folder = Path(a.folder)
    ancang = Image.open(folder / a.ancang).convert("RGBA")
    strike = Image.open(folder / a.strike).convert("RGBA")
    idle = Image.open(folder / a.idle).convert("RGBA")
    cols = COLORS[a.colors]

    smear = strike.copy()
    if a.kind == "arc":
        arc_trail(smear, a, cols, [0])
    elif a.kind == "claw":
        arc_trail(smear, a, cols, [-3, 0, 3])
    elif a.kind == "thrust":
        thrust_trail(smear, a, cols)

    frames = [ancang, smear, strike, shift(strike, a.follow), idle]
    durations = [float(d) for d in a.durations.split(",")]
    if a.kind == "none":                          # tanpa smear: frame ke-2 = pose serang biasa
        frames[1] = strike
    out = folder / "anim"
    out.mkdir(exist_ok=True)
    for i, f in enumerate(frames):
        f.save(out / f"serang_{i}.png")
    timing = {"durations": durations, "impact": 2}
    (out / "serang_timing.json").write_text(json.dumps(timing))

    k = 4
    w, h = ancang.size
    sheet = Image.new("RGBA", (len(frames) * (w * k + 8), h * k), (20, 20, 24, 255))
    for i, f in enumerate(frames):
        bg = Image.new("RGBA", f.size, (30, 27, 39, 255))
        bg.alpha_composite(f)
        sheet.alpha_composite(bg.resize((w * k, h * k), Image.NEAREST), (i * (w * k + 8), 0))
    sheet.save(out / "serang-smear-preview.png")

    if a.install:
        dst = Path(a.install)
        for old in dst.glob(f"{a.name}_serang_*"):
            old.unlink()
        normals = Path(__file__).with_name("make_normals.py")
        for i, f in enumerate(frames):
            p = dst / f"{a.name}_serang_{i}.png"
            f.save(p)
            subprocess.run([sys.executable, "-I", str(normals), str(p)], check=True, capture_output=True)
        (dst / f"{a.name}_serang_timing.json").write_text(json.dumps(timing))
    print(f"{a.name}: {a.kind}, {len(frames)} frame, {durations}")


if __name__ == "__main__":
    main()
