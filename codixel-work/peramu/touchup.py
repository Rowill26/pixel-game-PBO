"""Perbaikan pixel manual sesudah normalize_ai_pixel.py untuk Peramu.

python touchup.py <in.png> <out.png> <pose>
Koordinat = kanvas 96x64 sesudah --mirror (menghadap kiri).
Hanya pixel ABU-ABU (warna latar / bayangan lantai) di dalam kotak yang dihapus;
kulit, kain hijau, dan selendang krem tidak tersentuh karena warnanya tidak netral.
"""
import sys
from PIL import Image

# kotak (x0, y0, x1, y1)
ERASE_GREY = {
    "ancang": [(48, 23, 57, 32)],   # latar terjepit di antara lengan yang terangkat dan kepala
    "gugur": [(0, 0, 95, 63)],      # bayangan lantai abu-abu di bawah badan
}


def is_grey(r, g, b):
    return max(r, g, b) - min(r, g, b) < 22 and (r + g + b) / 3 > 100


src, out, pose = sys.argv[1:4]
im = Image.open(src).convert("RGBA")
for x0, y0, x1, y1 in ERASE_GREY.get(pose, []):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            r, g, b, a = im.getpixel((x, y))
            if a and is_grey(r, g, b):
                im.putpixel((x, y), (0, 0, 0, 0))
im.save(out)
