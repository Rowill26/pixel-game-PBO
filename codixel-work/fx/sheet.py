# Composite transparent effect frames on the dark battle colour (real alpha) into one preview row each.
import sys, glob, os
from PIL import Image
names = sys.argv[1:]
for n in names:
    fs = sorted(glob.glob(f'{n}/{n}_*.png'), key=lambda p: int(p.rsplit('_', 1)[1][:-4]))
    ims = [Image.open(f).convert('RGBA') for f in fs]
    w, h = ims[0].size
    k = 3 if w <= 64 else 2
    cols = min(len(ims), 8 if w <= 64 else 5)
    rows = (len(ims) + cols - 1) // cols
    sh = Image.new('RGBA', (cols * (w * k + 6), rows * (h * k + 6)), (12, 10, 16, 255))
    for i, im in enumerate(ims):
        bg = Image.new('RGBA', (w, h), (40, 46, 44, 255)); bg.alpha_composite(im)
        sh.alpha_composite(bg.resize((w * k, h * k), Image.NEAREST), ((i % cols) * (w * k + 6), (i // cols) * (h * k + 6)))
    sh.save(f'{n}/sheet.png')
    print(n, sh.size)
