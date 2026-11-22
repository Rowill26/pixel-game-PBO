"""Terangkan kain gelap Pengintai sebelum normalize_ai_pixel.py.

python lift.py <src.jpeg> <out.png> [gain] [add]

AI menggambar bajunya hampir hitam, sehingga sesudah dicocokkan ke palet kain jatuh ke warna
garis tepi (Bayangan 1) dan tokohnya menjadi siluet di arena malam. Di sini hanya pixel yang
gelap DAN kebiruan/netral (biru >= merah) yang diterangkan; garis tepi hitam pekat dan kulit
(merah > biru) tidak diubah.
"""
import sys
import numpy as np
from PIL import Image

src, out = sys.argv[1:3]
gain = float(sys.argv[3]) if len(sys.argv) > 3 else 1.7
add = float(sys.argv[4]) if len(sys.argv) > 4 else 14

a = np.asarray(Image.open(src).convert("RGB")).astype(float)
r, g, b = a[..., 0], a[..., 1], a[..., 2]
mx = a.max(axis=2)
cloth = (mx >= 22) & (mx < 120) & (b >= r - 4)
lifted = np.clip(a * gain + add, 0, 255)
a[cloth] = lifted[cloth]
Image.fromarray(a.astype(np.uint8)).save(out)
