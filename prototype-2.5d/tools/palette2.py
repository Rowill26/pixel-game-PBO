"""Palet v2 (64 warna, redup, 2.5D) — Calon Arang: Bayang-Bayang Girah."""

RAMPS = [
    ("Bayangan", "Bayangan dingin, garis luar, malam", ["121019", "1e1b27", "2c2836", "3d3948"]),
    ("Langit", "Langit, air, kabut jauh", ["232b3d", "34405a", "4e5f78", "75879a", "a3b2b8"]),
    ("Senja", "Langit senja, bayangan hangat", ["3b2d42", "5c4660", "86677e", "b08f95"]),
    ("Batu", "Batu andesit: candi, arca, nisan, jalan", ["2f3236", "474c50", "656b6b", "878d87", "aeb1a5"]),
    ("Bata", "Bata merah Majapahit: gapura, candi, tembok", ["43231f", "63342a", "84483a", "a2634c", "bf8667"]),
    ("Daun", "Lumut, rumput, daun, hutan", ["1b261f", "28382b", "3a4c34", "52653f", "6f7f4c", "919a62"]),
    ("Tanah", "Tanah, kayu, tiang bale", ["2a211c", "413227", "5c4634", "7a5f45", "987c5a"]),
    ("Jerami", "Atap alang-alang, anyaman, bambu kering", ["7c6a45", "a08b5c", "c2ad7d"]),
    ("Putih", "Kain putih, jubah, rambut Rangda", ["c9c1ad", "e3dccb", "f4efe2"]),
    ("Kulit", "Kulit semua tokoh", ["4f3326", "74503c", "9a6e52", "bd916f", "d8b490"]),
    ("Merah", "Kain ritual, poleng merah, merah tua", ["3f171b", "642127", "8c2f2e", "b0473a"]),
    ("Emas", "Perhiasan, mahkota, ukiran", ["5e4a24", "8a6d33", "b8954a", "dcc07a"]),
    ("Girah", "Kabut wabah, sihir Calon Arang", ["33294a", "4f416c", "73639a", "9f92c2"]),
    ("Api", "Obor, pelita, bara dupa (sumber cahaya)", ["c9773c", "e8b25e", "f6e3a8"]),
    ("Sistem", "Warna UI dan makna sistem", ["74ad7e", "5f95b9", "c2463d", "ece6d7"]),
]
SYSTEM_NAMES = ["Penyembuhan", "Energi batin", "Nyawa", "Teks UI"]


def palette():
    out = []
    for name, use, hexes in RAMPS:
        for i, h in enumerate(hexes):
            label = SYSTEM_NAMES[i] if name == "Sistem" else f"{name} {i + 1}"
            out.append((label, h, name, use))
    return out


PALETTE = palette()
assert len(PALETTE) == 64, len(PALETTE)
assert len({h for _, h, _, _ in PALETTE}) == 64
