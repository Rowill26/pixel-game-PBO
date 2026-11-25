"""Serangan Pendekar dengan smear frame dan waktu per frame (game feel).

python smear.py   (dijalankan di folder ini; menulis anim/serang_*.png dan anim/serang_timing.json)

Urutan: ancang (antisipasi, ditahan) -> smear (jejak lengkung keris, sangat cepat) -> tebas
(benturan, ditahan) -> tebas maju 1 px (follow-through) -> idle (pulih).
Koordinat = kanvas 96x64 sesudah --mirror (Pendekar menghadap kiri, menyerang ke kiri).
"""
import json
import math
from PIL import Image

ancang = Image.open("ancang.png").convert("RGBA")
tebas = Image.open("tebas.png").convert("RGBA")
idle = Image.open("idle.png").convert("RGBA")

SHOULDER = (44, 31)          # pusat ayunan
TIP_FROM = (74, 49)          # ujung keris di pose ancang (rendah di belakang)
TIP_TO = (34, 38)            # ujung keris di pose tebas (depan, setinggi pinggang)
# warna baja ke kilau, dari ekor ke kepala jejak (palet: Batu 4-5, Putih 2-3)
TRAIL = [(0x87, 0x8d, 0x87), (0xae, 0xb1, 0xa5), (0xe3, 0xdc, 0xcb), (0xf4, 0xef, 0xe2)]


def polar(p):
    dx, dy = p[0] - SHOULDER[0], p[1] - SHOULDER[1]
    return math.atan2(dy, dx), math.hypot(dx, dy)


def smear_frame():
    """Pose tebas + bulan sabit jejak keris yang menyapu di bawah bahu (dari belakang ke depan)."""
    img = tebas.copy()
    a0, r0 = polar(TIP_FROM)
    a1, r1 = polar(TIP_TO)
    if a1 < a0:
        a1 += 2 * math.pi
    steps = 120
    for i in range(steps + 1):
        t = i / steps                          # 0 = ekor (posisi ancang), 1 = kepala (posisi tebas)
        a = a0 + (a1 - a0) * t
        r = r0 + (r1 - r0) * t
        width = 1 + int(2.5 * t)               # menebal ke arah kepala jejak
        col = TRAIL[min(len(TRAIL) - 1, int(t * len(TRAIL)))]
        if t < 0.25 and i % 2:                 # ekor putus-putus (dither) supaya terasa memudar
            continue
        for w in range(width):
            rr = r - w * 0.9                   # jejak menebal ke dalam (ke arah bahu)
            x = round(SHOULDER[0] + math.cos(a) * rr)
            y = round(SHOULDER[1] + math.sin(a) * rr)
            if 0 <= x < img.width and 0 <= y < img.height:
                img.putpixel((x, y), col + (255,))
    return img


def shift(im, dx, dy):
    out = Image.new("RGBA", im.size)
    out.alpha_composite(im, (dx, dy))
    return out


frames = [ancang, smear_frame(), tebas, shift(tebas, -1, 0), idle]
durations = [0.16, 0.05, 0.12, 0.08, 0.10]
for i, f in enumerate(frames):
    f.save(f"anim/serang_{i}.png")
with open("anim/serang_timing.json", "w") as fh:
    json.dump({"durations": durations, "impact": 2}, fh)

# lembar pratinjau
k = 4
sheet = Image.new("RGBA", (len(frames) * (96 * k + 8), 64 * k + 20), (20, 20, 24, 255))
for i, f in enumerate(frames):
    bg = Image.new("RGBA", f.size, (30, 27, 39, 255))
    bg.alpha_composite(f)
    sheet.alpha_composite(bg.resize((96 * k, 64 * k), Image.NEAREST), (i * (96 * k + 8), 20))
sheet.save("anim/serang-smear-preview.png")
print("ok", durations)
