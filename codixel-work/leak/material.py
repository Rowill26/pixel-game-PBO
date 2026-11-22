"""Leak: remove the light halo left by the background, then colour by material with fixed tone shares."""
import json
import numpy as np
from PIL import Image, ImageFilter
from scipy import ndimage

HEX = open('../../docs/art/palette-girah.hex').read().split()
PAL = np.array([[int(h[i:i + 2], 16) for i in (0, 2, 4)] for h in HEX], np.uint8)
RAMP = {
    'hair':  ([0, 1, 2], [0.55, 0.30, 0.15]),      # Bayangan 1-3
    'cloth': ([16, 37, 38], [0.25, 0.50, 0.25]),   # Batu 4, Putih 1-2 (dirty white)
    'skin':  ([15, 16, 17], [0.30, 0.45, 0.25]),   # Batu 3-5 (pale grey)
    'mist':  ([53, 54, 55], [0.35, 0.40, 0.25]),   # Girah 1-3
    'glow':  ([56, 39], [0.6, 0.4]),               # Girah 4 + Putih 3 core (eyes)
}

a = np.array(Image.open('nobg-idle.png').convert('RGBA'))
alpha = a[..., 3] > 0
rgb = a[..., :3].astype(float)
r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
mx, mn = rgb.max(2), rgb.min(2)
sat = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1), 0)
lum = r * 0.299 + g * 0.587 + b * 0.114
hue = np.zeros_like(mx); d = np.maximum(mx - mn, 1e-6)
m = (mx - mn) > 0
hr = m & (mx == r); hg = m & (mx == g) & ~hr; hb = m & (mx == b) & ~hr & ~hg
hue[hr] = (60 * (g - b) / d)[hr] % 360; hue[hg] = (60 * ((b - r) / d + 2))[hg]; hue[hb] = (60 * ((r - g) / d + 4))[hb]

# 1) halo: light, low-saturation pixels within 3 px of transparency (cloth is closed by black lines)
near_edge = np.array(Image.fromarray((~alpha * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(7))) > 0
halo = alpha & near_edge & (lum > 165) & (sat < 0.12)
alpha = alpha & ~halo
# keep only the main body + pieces of mist that touch it (drops loose specks)
lab, k = ndimage.label(alpha)
sizes = ndimage.sum(alpha, lab, range(1, k + 1))
alpha = np.isin(lab, [i + 1 for i, s in enumerate(sizes) if s > 400])

purple = (hue >= 255) & (hue <= 310)
ys = np.nonzero(alpha.any(1))[0]; top, bot = ys.min(), ys.max()
lower = np.zeros_like(alpha); lower[top + int((bot - top) * 0.58):] = True     # the mist only lives down here
glow = alpha & purple & (sat > 0.45) & (lum > 110)
mist = alpha & lower & purple & ~glow & (sat > 0.08)
hair = alpha & ~glow & ~mist & (lum < 95)
rest = alpha & ~glow & ~mist & ~hair
cloth = rest & ((r >= b + 2) | (lum > 185))                   # off-white cloth (warm or very light)
skin = rest & ~cloth                                          # cool / violet pale skin
mats = {'hair': hair, 'cloth': cloth, 'skin': skin, 'mist': mist, 'glow': glow}

out = np.zeros_like(a)
for name, sel in mats.items():
    idx, share = RAMP[name]
    if not sel.any():
        continue
    v = lum[sel]
    cuts = np.quantile(v, np.cumsum(share)[:-1])
    out[sel, :3] = PAL[np.array(idx)[np.searchsorted(cuts, v)]]
    out[sel, 3] = 255
Image.fromarray(out).save('q-idle.png')
# eye centres (glow blobs) for stamping after the shrink
lab2, k2 = ndimage.label(glow)
eyes = sorted([[float(c[1]), float(c[0])] for c in ndimage.center_of_mass(glow, lab2, range(1, k2 + 1))
               if True], key=lambda e: e[0])
sizes2 = ndimage.sum(glow, lab2, range(1, k2 + 1))
eyes = [e for e, s in zip(sorted([[float(c[1]), float(c[0]), s] for c, s in zip(ndimage.center_of_mass(glow, lab2, range(1, k2 + 1)), sizes2)], key=lambda e: -e[2])[:2], [0, 0])]
json.dump({'eyes': [[e[0], e[1]] for e in eyes]}, open('eyes.json', 'w'))
print({k: int(v.sum()) for k, v in mats.items()}, 'halo removed', int(halo.sum()), 'eyes', [[round(e[0]), round(e[1])] for e in eyes])
