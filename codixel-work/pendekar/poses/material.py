"""Material-based colouring: every pose gets the same ramps and the same share of light/mid/dark,
so the six sprites look like one character drawn by one hand."""
import numpy as np
from PIL import Image, ImageFilter
from scipy import ndimage

HEX = open('../../docs/art/palette-girah.hex').read().split()
PAL = np.array([[int(h[i:i + 2], 16) for i in (0, 2, 4)] for h in HEX], np.uint8)
# material -> (palette indices dark..light, share of pixels per tone dark..light)
RAMP = {
    'skin':  ([41, 42, 43], [0.30, 0.50, 0.20]),   # Kulit 2-4
    'red':   ([45, 46, 47], [0.35, 0.45, 0.20]),   # Merah 1-3
    'gold':  ([50, 51, 52], [0.30, 0.50, 0.20]),   # Emas 2-4
    'steel': ([14, 16, 17], [0.30, 0.45, 0.25]),   # Batu 2,4,5
    'dark':  ([0, 1], [0.70, 0.30]),               # Bayangan 1-2 (outline, hair)
}

for n in ['idle', 'ancang', 'tebas', 'jurus', 'terluka', 'gugur']:
    a = np.array(Image.open(f'poses/nobg-{n}.png').convert('RGBA'))
    alpha = a[..., 3] > 0
    rgb = a[..., :3].astype(float)
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    mx, mn = rgb.max(2), rgb.min(2)
    sat = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1), 0)
    hue = np.zeros_like(mx)
    m = ((mx - mn) > 0) & (mx == r)
    hue[m] = (60 * ((g - b) / np.maximum(mx - mn, 1e-6)))[m] % 360
    lum = r * 0.299 + g * 0.587 + b * 0.114

    grey = alpha & ((mx - mn) < 26) & (mx > 80) & (mx < 236)
    lab, k = ndimage.label(grey)
    sizes = ndimage.sum(grey, lab, range(1, k + 1))
    steel = np.isin(lab, [i + 1 for i, s in enumerate(sizes) if s > 3000])
    # thicken the blade outward so it survives the shrink
    thick = np.array(Image.fromarray((steel * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(45))) > 0
    add = thick & ~alpha
    alpha2 = alpha | add
    lum2 = lum.copy(); lum2[add] = 140

    mat = np.full(alpha.shape, '', dtype=object)
    dark = alpha & (mx < 70)
    gold = alpha & ~dark & (hue >= 36) & (hue <= 60) & (sat > 0.45) & (mx > 90)
    red = alpha & ~dark & ~gold & ((hue <= 14) | (hue >= 330)) & (sat > 0.35) & (r < 170)
    mat[alpha2] = 'skin'
    mat[red] = 'red'; mat[gold] = 'gold'; mat[steel | add] = 'steel'; mat[dark] = 'dark'

    out = np.zeros_like(a)
    for name, (idx, share) in RAMP.items():
        sel = (mat == name)
        if not sel.any():
            continue
        v = lum2[sel]
        cuts = np.quantile(v, np.cumsum(share)[:-1])
        tone = np.searchsorted(cuts, v)
        out[sel, :3] = PAL[np.array(idx)[tone]]
        out[sel, 3] = 255
    Image.fromarray(out).save(f'poses/q-{n}.png')
    print(n, {k2: int((mat == k2).sum()) for k2 in RAMP})
