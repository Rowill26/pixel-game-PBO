"""Seragamkan gambar "pixel art" buatan AI supaya cocok dengan game.

Gambar pixel dari AI hanya TERLIHAT seperti pixel art: ukuran kotaknya bukan bilangan bulat dan
berbeda-beda tiap gambar, warnanya ratusan (sisa JPEG), latarnya tidak transparan, dan tinggi
tokohnya acak. Alat ini:

  1. mengukur ukuran kotak pixel (grid) secara otomatis,
  2. mengambil satu warna dari tengah setiap kotak  -> gambar pixel "asli",
  3. menghapus latar polos,
  4. mencocokkan warna ke palet game (64 warna, bisa dibatasi ke ramp tertentu),
  5. menyamakan tinggi tokoh (bawaan 50 pixel) dengan warna mayoritas per sel,
  6. menaruhnya di kanvas 96 x --canvas-h dengan kaki/ujung bawah 3 px di atas tepi bawah, tengah di x=48
     (bisa dibalik kiri-kanan),
  7. membuat normal map (_n.png) untuk cahaya di game.

Pemakaian (dari folder prototype-2.5d):
  python tools/normalize_ai_pixel.py <gambar-ai> <keluaran.png> [opsi]

Opsi:
  --height 50            tinggi tokoh di game (pixel)
  --ramps Bayangan,Batu  batasi warna ke ramp palet ini (lihat docs/art/art-direction-brief.md B4)
  --mirror               balik kiri-kanan (pengelana menghadap kiri, musuh menghadap kanan)
  --bg-tol 18            toleransi warna latar (naikkan jika sisa latar masih ada)
  --feet-x 48            posisi tengah kaki/ujung bawah di kanvas
  --scale 0.704          pakai skala tetap (pixel asli -> pixel game) alih-alih --height;
                         WAJIB untuk pose-pose satu tokoh supaya ukurannya sama
  --top-y 11             ratakan ubun-ubun ke baris ini dan tengah KEPALA ke --feet-x (untuk tokoh melayang yang ujung
                         bawahnya berubah-ubah antar pose); bawaan: ratakan ujung bawah ke baris 60
  --canvas-h 64          tinggi kanvas (naikkan untuk tokoh bertongkat panjang / pose tinggi)
  --min-hole 4           celah tertutup berwarna latar sebesar >= ini (sel grid) ikut dihapus
  --anchor-feet          ratakan posisi kiri-kanan dari KAKI (tokoh yang berdiri di tanah)
  --preview file.png     simpan gambar perbandingan (asli, grid, hasil)
"""
import argparse
import sys
from collections import deque
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from make_normals import normal_map  # noqa: E402
from palette2 import PALETTE  # noqa: E402

CANVAS_W = 96


def find_pitch(rgb):
    """Ukuran kotak pixel (boleh pecahan) dan titik awalnya, dari letak garis-garis perubahan warna."""
    profiles = []
    for axis in (1, 0):
        d = np.abs(np.diff(rgb, axis=axis)).sum(2).sum(1 - axis)
        profiles.append(d / (d.mean() + 1e-6))

    def score(p):
        """Rata-rata kedua sumbu; untuk tiap sumbu ambil titik awal terbaik. Hasil: (skor, ox, oy)."""
        total, offs = 0.0, []
        for prof in profiles:
            n = len(prof)
            best = (-1.0, 0.0)
            for off in np.arange(0, p, 0.25):
                idx = np.round(np.arange(off, n - 1, p)).astype(int) - 1
                idx = idx[(idx >= 0) & (idx < n)]
                if len(idx) >= 8:
                    best = max(best, (prof[idx].mean(), off))
            total += best[0]
            offs.append(best[1])
        return total / 2, offs[0], offs[1]

    def refine(center, width, step):
        return max((score(p) + (p,) for p in np.arange(center - width, center + width, step)), key=lambda t: t[0])

    coarse = max((score(p)[0], p) for p in np.arange(5.0, 40.0, 0.1))
    top = refine(coarse[1], 0.12, 0.005)
    # a multiple of the real pitch also lines up with every other edge: prefer the smallest
    # pitch p/k that scores almost as well
    for k in (4, 3, 2):
        if top[3] / k < 5:
            continue
        cand = refine(top[3] / k, 0.1, 0.005)
        if cand[0] >= 0.85 * top[0]:
            return cand[3], cand[1], cand[2]
    return top[3], top[1], top[2]


def sample_grid(rgb, pitch, ox, oy):
    h, w = rgb.shape[:2]
    nx, ny = int((w - ox) // pitch), int((h - oy) // pitch)
    out = np.zeros((ny, nx, 3), np.uint8)
    for j in range(ny):
        for i in range(nx):
            x0, x1 = int(ox + i * pitch + pitch * 0.3), int(ox + i * pitch + pitch * 0.7) + 1
            y0, y1 = int(oy + j * pitch + pitch * 0.3), int(oy + j * pitch + pitch * 0.7) + 1
            out[j, i] = np.median(rgb[y0:y1, x0:x1].reshape(-1, 3), axis=0)
    return out


def remove_background(img, tol, min_hole=4):
    """Hapus latar polos dengan flood fill dari tepi gambar."""
    h, w = img.shape[:2]
    edge = np.concatenate([img[0], img[-1], img[:, 0], img[:, -1]]).astype(float)
    bg = np.median(edge, axis=0)
    near = np.sqrt(((img.astype(float) - bg) ** 2).sum(2)) <= tol
    keep = np.ones((h, w), bool)
    q = deque((y, x) for y in range(h) for x in (0, w - 1) if near[y, x])
    q.extend((y, x) for x in range(w) for y in (0, h - 1) if near[y, x])
    seen = np.zeros((h, w), bool)
    while q:
        y, x = q.popleft()
        if seen[y, x] or not near[y, x]:
            continue
        seen[y, x] = True
        keep[y, x] = False
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            ny, nx = y + dy, x + dx
            if 0 <= ny < h and 0 <= nx < w and not seen[ny, nx]:
                q.append((ny, nx))
    # background trapped in closed gaps (between an arm and a weapon, between kneeling legs): the
    # flood from the edges cannot reach it. Remove every remaining patch of background colour that
    # is at least `min_hole` grid cells big; smaller ones are likely highlights and stay.
    from scipy import ndimage
    holes, n = ndimage.label(near & keep)
    if n:
        sizes = ndimage.sum(near & keep, holes, range(1, n + 1))
        big = [i + 1 for i, sz in enumerate(sizes) if sz >= min_hole]
        keep[np.isin(holes, big)] = False
    return keep


def _hue_sat(c):
    c = np.asarray(c, float)
    mx, mn = c.max(-1), c.min(-1)
    d = np.maximum(mx - mn, 1e-6)
    r, g, b = c[..., 0], c[..., 1], c[..., 2]
    h = np.where(mx == r, (60 * (g - b) / d) % 360,
                 np.where(mx == g, 60 * ((b - r) / d + 2), 60 * ((r - g) / d + 4)))
    s = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1), 0)
    return np.where(mx - mn < 1, 0, h), s


def to_palette(img, mask, ramps, sat_weight=0.0):
    """Warna terdekat di palet, dengan hukuman jika arah warnanya (hue) berbeda: kulit oranye tidak
    boleh menjadi emas, emas tidak boleh menjadi kulit, walaupun jarak RGB-nya dekat."""
    pal = np.array([[int(hx[i:i + 2], 16) for i in (0, 2, 4)] for _, hx, ramp, _ in PALETTE
                    if not ramps or ramp in ramps], float)
    flat = img.reshape(-1, 3).astype(float)
    d = ((flat[:, None, :] - pal[None, :, :]) ** 2).sum(2)
    hp, sp = _hue_sat(flat)
    hq, sq = _hue_sat(pal)
    dh = np.abs(hp[:, None] - hq[None, :])
    dh = np.minimum(dh, 360 - dh)
    both = np.minimum(sp[:, None], sq[None, :])          # only matters when both colours are saturated
    d = d + 6000.0 * (dh / 30.0) ** 2 * both
    # optional: grey must stay grey (e.g. a white/grey mane next to skin): penalise saturation mismatch
    d = d + sat_weight * (sp[:, None] - sq[None, :]) ** 2
    out = pal[d.argmin(1)].reshape(img.shape).astype(np.uint8)
    rgba = np.zeros(img.shape[:2] + (4,), np.uint8)
    rgba[..., :3] = out
    rgba[..., 3] = np.where(mask, 255, 0)
    return rgba


def crop_to_content(rgba):
    ys, xs = np.nonzero(rgba[..., 3])
    return rgba[ys.min():ys.max() + 1, xs.min():xs.max() + 1]


def resize_majority(rgba, w, h, ink=0.0, bright=0.0):
    """Perkecil pixel art: setiap sel baru mengambil warna yang paling banyak (bukan rata-rata).

    ink    > 0: warna tergelap (garis tepi) menang bila menutupi >= ink bagian sel,
                supaya tangan di depan badan tetap punya garis pemisah.
    bright > 0: warna sangat terang (kilau, cahaya) menang bila menutupi >= bright bagian sel,
                walaupun sel itu sebagian besar latar, supaya titik cahaya tidak hilang.
    """
    sh, sw = rgba.shape[:2]
    out = np.zeros((h, w, 4), np.uint8)
    for j in range(h):
        y0, y1 = int(j * sh / h), max(int(j * sh / h) + 1, int((j + 1) * sh / h))
        for i in range(w):
            x0, x1 = int(i * sw / w), max(int(i * sw / w) + 1, int((i + 1) * sw / w))
            cell = rgba[y0:y1, x0:x1].reshape(-1, 4)
            solid = cell[cell[:, 3] > 0]
            if not len(solid):
                continue
            cols, counts = np.unique(solid[:, :3], axis=0, return_counts=True)
            luma = cols @ np.array([0.299, 0.587, 0.114])
            share = counts / len(cell)
            pick = None
            if bright > 0:
                b = luma.argmax()
                c = cols[b].astype(int)
                glow = c.max() - c.min() > 40          # warm glow (gold/fire), not white cloth or eye whites
                outside = len(solid) * 2 < len(cell)   # a sparkle floating next to the body
                if luma[b] >= 170 and share[b] >= bright and (outside or glow and luma[b] - luma[counts.argmax()] > 60):
                    pick = cols[b]
            if pick is None and len(solid) * 2 < len(cell):
                continue
            if pick is None and ink > 0:
                d = luma.argmin()
                if luma[d] < 55 and share[d] >= ink:
                    pick = cols[d]
            out[j, i, :3] = cols[counts.argmax()] if pick is None else pick
            out[j, i, 3] = 255
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("src")
    ap.add_argument("out")
    ap.add_argument("--height", type=int, default=50)
    ap.add_argument("--ramps", default="")
    ap.add_argument("--mirror", action="store_true")
    ap.add_argument("--bg-tol", type=float, default=18)
    ap.add_argument("--feet-x", type=int, default=48)
    ap.add_argument("--preview")
    ap.add_argument("--scale", type=float)
    ap.add_argument("--top-y", type=int)
    ap.add_argument("--anchor-feet", action="store_true")
    ap.add_argument("--min-hole", type=int, default=4)
    ap.add_argument("--canvas-h", type=int, default=64)
    ap.add_argument("--canvas-w", type=int, default=96)
    ap.add_argument("--sat-weight", type=float, default=0.0)
    ap.add_argument("--keep-ink", type=float, default=0.0)
    ap.add_argument("--keep-bright", type=float, default=0.0)
    a = ap.parse_args()

    rgb = np.asarray(Image.open(a.src).convert("RGB"))
    pitch, ox, oy = find_pitch(rgb.astype(float))
    native = sample_grid(rgb, pitch, ox, oy)
    mask = remove_background(native, a.bg_tol, a.min_hole)
    ramps = {r.strip() for r in a.ramps.split(",") if r.strip()}
    sprite = crop_to_content(to_palette(native, mask, ramps, a.sat_weight))
    nh, nw = sprite.shape[:2]
    if a.scale:
        h, w = max(1, round(nh * a.scale)), max(1, round(nw * a.scale))
    else:
        h = a.height
        w = max(1, round(nw * h / nh))
    small = resize_majority(sprite, w, h, a.keep_ink, a.keep_bright) if (w, h) != (nw, nh) else sprite
    if a.mirror:
        small = small[:, ::-1]
    CANVAS_H = a.canvas_h
    CANVAS_W = a.canvas_w
    FEET_Y = CANVAS_H - 4                      # the game draws sprites with the feet 3 px above the bottom edge
    ys, xs = np.nonzero(small[..., 3])
    canvas = np.zeros((CANVAS_H, CANVAS_W, 4), np.uint8)
    if a.anchor_feet:
        # grounded characters: line poses up by the feet (lowest 3 rows), so they do not slide
        feet = ys >= ys.max() - 2
        dx = a.feet_x - int(round(xs[feet].mean()))
    elif a.top_y is not None:
        # floating characters: line poses up by the head (top quarter), not by the body or mist
        head = ys <= ys.min() + (ys.max() - ys.min()) * 0.25
        dx = a.feet_x - int(round(xs[head].mean()))
    else:
        dx = a.feet_x - int(round(xs.mean()))
    dy = (a.top_y - ys.min()) if a.top_y is not None else (FEET_Y - ys.max())
    for y, x in zip(ys, xs):
        cx, cy = x + dx, y + dy
        if 0 <= cx < CANVAS_W and 0 <= cy < CANVAS_H:
            canvas[cy, cx] = small[y, x]
    img = Image.fromarray(canvas, "RGBA")
    out = Path(a.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    normal_map(img, 2.2).save(out.with_name(out.stem + "_n.png"))
    print(f"skala {w / nw:.3f}; " + f"grid {pitch:.2f}px -> gambar asli {native.shape[1]}x{native.shape[0]}, tokoh {nw}x{nh} "
          f"-> {w}x{h} di kanvas {CANVAS_W}x{CANVAS_H}; warna {len(np.unique(canvas[canvas[..., 3] > 0][:, :3], axis=0))}")

    if a.preview:
        def big(im, k):
            return im.resize((im.width * k, im.height * k), Image.NEAREST)
        src = Image.open(a.src).convert("RGB")
        src.thumbnail((400, 400))
        nat = big(Image.fromarray(sprite, "RGBA"), max(1, 400 // max(nh, nw)))
        res = big(img, 6)
        sheet = Image.new("RGB", (src.width + nat.width + res.width + 40, max(src.height, nat.height, res.height)), (30, 27, 39))
        x = 0
        for part in (src, nat, res):
            sheet.paste(part, (x, 0), part if part.mode == "RGBA" else None)
            x += part.width + 20
        sheet.save(a.preview)


if __name__ == "__main__":
    main()
