"""Perbaikan pixel manual sesudah normalize_ai_pixel.py untuk Pengintai.

python touchup.py <in.png> <out.png> <pose>
Koordinat = kanvas 96x64 sesudah --mirror (menghadap kiri).
"""
import sys
from PIL import Image

# latar terjepit: kotak (x0, y0, x1, y1) + warna yang dihapus
ERASE = {
    "idle": [((52, 31, 56, 38), {"a3b2b8"})],          # antara lengan pemegang belati dan badan
    "gugur": [((0, 54, 95, 58), {"75879a", "a3b2b8"})],  # bayangan lantai
}

# bilah belati digambar AI berwarna cokelat kayu: ubah jadi baja (ramp Batu)
STEEL = {"4f3326": "474c50", "5c4634": "656b6b", "74503c": "878d87", "7a5f45": "aeb1a5",
         "9a6e52": "aeb1a5", "2a211c": "2f3236", "413227": "474c50", "987c5a": "aeb1a5"}
BLADE = {
    "idle": [(60, 36, 68, 40)],
    "ancang": [(60, 30, 70, 38)],
    "tusuk": [(19, 29, 31, 34)],
    "jurus": [(27, 47, 36, 52)],
    "terluka": [(58, 39, 66, 48)],
    "gugur": [(30, 57, 45, 62)],
}
# di pose selain idle bilah bersinggungan dengan tangan: hanya warna kayu (ramp Tanah) yang diganti
WOOD_ONLY = {"ancang", "tusuk", "jurus", "terluka", "gugur"}
TANAH = {"2a211c", "413227", "5c4634", "7a5f45", "987c5a"}


def hexof(p):
    return "%02x%02x%02x" % p[:3]


src, out, pose = sys.argv[1:4]
im = Image.open(src).convert("RGBA")
for (x0, y0, x1, y1), cols in ERASE.get(pose, []):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            p = im.getpixel((x, y))
            if p[3] and hexof(p) in cols:
                im.putpixel((x, y), (0, 0, 0, 0))
for x0, y0, x1, y1 in BLADE.get(pose, []):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            p = im.getpixel((x, y))
            if p[3] and hexof(p) in STEEL and (pose not in WOOD_ONLY or hexof(p) in TANAH | {"4f3326"}):
                im.putpixel((x, y), tuple(bytes.fromhex(STEEL[hexof(p)])) + (255,))
im.save(out)
