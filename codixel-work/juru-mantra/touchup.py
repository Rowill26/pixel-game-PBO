"""Perbaikan pixel manual sesudah normalize_ai_pixel.py (tangan yang hilang saat diperkecil).

python touchup.py <in.png> <out.png> <pose>
Koordinat = kanvas 96x80 sesudah --mirror (menghadap kiri).
"""
import sys
from PIL import Image

C = {"b": "121019", "o": "bd916f", "h": "9a6e52", "e": "74503c", "d": "dcc07a", "t": "b8954a"}

# setiap pose: (x kiri, y atas, baris gambar); '.' = biarkan
PATCH = {
    "idle": (40, 38, [
        "..bbb.",
        ".bhob.",
        ".bhob.",
        ".bhob.",
        ".bhob.",
        "bbtdb.",
        "ohbbb.",
        "bb....",
    ]),
}

# sisa latar abu-abu yang terjepit (di antara kain dan tongkat): kotak (x0, y0, x1, y1),
# hanya pixel terang yang dihapus, garis tepi dan tongkat dibiarkan
ERASE = {
    "terluka": [(52, 47, 56, 69, 120)],   # antara kain dan tongkat (abu-abu muda ikut terhapus)
    "ancang": [(57, 33, 60, 38, 195)],    # antara tangan atas dan tongkat (hanya putih; gelang emas tetap)
    "ayun": [(53, 46, 58, 51, 195)],      # antara lengan pemegang tongkat dan badan
}

src, out, pose = sys.argv[1:4]
im = Image.open(src).convert("RGBA")
if pose in PATCH:
    x0, y0, rows = PATCH[pose]
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch != ".":
                im.putpixel((x0 + i, y0 + j), tuple(bytes.fromhex(C[ch])) + (255,))
for x0, y0, x1, y1, thr in ERASE.get(pose, []):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            r, g, b, a = im.getpixel((x, y))
            if a and (r + g + b) / 3 > thr:
                im.putpixel((x, y), (0, 0, 0, 0))
im.save(out)
