# Daftar Audio — Calon Arang: Bayang-Bayang Girah

Daftar semua suara yang dibutuhkan game, disusun menurut tempat suara itu diputar.
Kolom **Prioritas**: 🔴 **Wajib** untuk demo/presentasi · 🟠 **Penting** · 🟢 **Tambahan** (boleh menyusul).

Isi kolom **Sumber** dan **Status** setiap kali satu suara selesai, supaya kita tahu mana yang sudah ada.

---

## Panduan umum

### Suasana dan gaya

- Game ini berlatar Kerajaan Daha (Jawa Timur kuno) dengan nuansa Bali, suasananya **mistis, muram, dan tragis** (kisah seorang ibu, bukan kisah monster).
- Musik sebaiknya bernuansa **gamelan** (laras slendro/pelog): gong, kempul, saron, bonang, kendang, suling, rebab. Boleh dipadu pad/drone ambient yang lembut.
- Hindari drum modern, gitar listrik, dan lagu berlirik.
- Efek suara (SFX) dibuat **realistis tapi pendek dan jelas**, bukan suara chiptune 8-bit.

### Format file

| Jenis | Format | Ketentuan |
|---|---|---|
| Musik latar (BGM) dan suasana (ambience) | **`.ogg`** | 44,1 kHz, stereo. Bisa diulang tanpa jeda yang terdengar (*seamless loop*). Panjang 1–3 menit. |
| Jingle (menang, kalah) | **`.ogg`** | 3–8 detik, tidak diulang. |
| Efek suara (SFX) | **`.wav`** | 44,1 kHz, 16-bit, **mono**. Pendek (umumnya di bawah 2 detik), tanpa hening di awal/akhir. |
| Semua | — | Volume diseragamkan (puncak sekitar −1 dBFS, keras-pelan antarfile tidak jauh berbeda). |

### Lisensi (penting untuk makalah)

- Utamakan **CC0** (bebas tanpa kredit). **CC-BY** boleh, tetapi nama pembuat wajib dicantumkan.
- **Jangan** memakai rekaman gamelan atau lagu dari YouTube/Spotify tanpa izin.
- Setiap file yang dipakai **dicatat di `CREDITS.md`**: nama file, pembuat, link sumber, lisensi.
- Sumber yang aman: [Freesound](https://freesound.org) (saring lisensi CC0), [OpenGameArt](https://opengameart.org), [Kenney](https://kenney.nl/assets?q=audio) (CC0), [Pixabay Music/Sound Effects](https://pixabay.com/sound-effects/) (cek lisensinya), atau dibuat sendiri (Audacity, LMMS, BeepBox, jsfxr/ChipTone untuk suara UI).

### Penamaan dan folder

Simpan dengan nama persis seperti di kolom **File**, di dalam folder `assets/audio/` repo:

```
assets/audio/
├── bgm/        musik latar dan jingle (.ogg)
├── ambience/   suara suasana tempat (.ogg)
└── sfx/        efek suara (.wav)
```

---

## 1. Musik latar (BGM)

| No | File | Diputar saat | Suasana / catatan | Prioritas | Sumber | Status |
|---|---|---|---|---|---|---|
| M1 | `bgm/menu.ogg` | Layar judul | Tenang, misterius, gamelan pelan + drone senja | 🔴 Wajib | | |
| M2 | `bgm/battle.ogg` | Pertarungan biasa (Hutan Beringin, desa diserang) | Tegang tapi tidak terlalu cepat, kendang dominan, loop | 🔴 Wajib | | |
| M3 | `bgm/boss.ogg` | Pertarungan Calon Arang (pemakaman Candi Durga) | Paling intens, gong berat, nuansa ritual, loop | 🔴 Wajib | | |
| M4 | `bgm/cerita.ogg` | Layar cerita/dialog umum (desa, Girah) | Lembut, melankolis, suling + gender | 🟠 Penting | | |
| M5 | `bgm/istana.ogg` | Balai Kerajaan Daha, Istana Daha | Agung tapi muram (kerajaan dilanda wabah) | 🟢 Tambahan | | |
| M6 | `bgm/ending-tragis.ogg` | Ending 3 (Akhir Tragis) | Sedih, lambat, sunyi | 🟢 Tambahan | | |
| M7 | `bgm/ending-pulih.ogg` | Ending 1–2 dan epilog | Hangat, harapan, tetap tenang | 🟢 Tambahan | | |

## 2. Jingle

| No | File | Diputar saat | Suasana / catatan | Prioritas | Sumber | Status |
|---|---|---|---|---|---|---|
| J1 | `bgm/menang.ogg` | Panel MENANG muncul | Singkat, lega, gong penutup | 🔴 Wajib | | |
| J2 | `bgm/kalah.ogg` | Panel KALAH muncul | Singkat, suram, nada turun | 🔴 Wajib | | |
| J3 | `sfx/judul-babak.wav` | Judul babak muncul di layar cerita | Satu pukulan gong + gaung | 🟢 Tambahan | | |

## 3. Suara suasana (ambience, diputar pelan di bawah musik)

| No | File | Lokasi | Isi suara | Prioritas | Sumber | Status |
|---|---|---|---|---|---|---|
| A1 | `ambience/hutan.ogg` | Hutan Beringin | Jangkrik, angin di daun, burung malam sesekali | 🟠 Penting | | |
| A2 | `ambience/malam.ogg` | Pemakaman Candi Durga | Angin dingin, serangga, api obor berderak pelan | 🟠 Penting | | |
| A3 | `ambience/desa.ogg` | Desa Tapak Wangi, Desa Girah | Ayam, suara warga samar, angin | 🟢 Tambahan | | |
| A4 | `ambience/istana.ogg` | Balai Kerajaan Daha | Ruang besar, langkah kaki, lampu minyak | 🟢 Tambahan | | |

## 4. Efek suara pertarungan — serangan dasar

Setiap tokoh punya serangan dasar yang berbeda; satu file per jenis serangan.

| No | File | Dipakai oleh | Bunyi | Prioritas | Sumber | Status |
|---|---|---|---|---|---|---|
| S1 | `sfx/tebas-keris.wav` | Pendekar (keris) | Sabetan logam tajam | 🔴 Wajib | | |
| S2 | `sfx/pukul-tongkat.wav` | Juru Mantra, Pemimpin Pengikut (tongkat) | Pukulan kayu tumpul | 🔴 Wajib | | |
| S3 | `sfx/lempar.wav` | Peramu (melempar kantung getah) | Lemparan + kantung pecah basah | 🟠 Penting | | |
| S4 | `sfx/tikam.wav` | Pengintai (belati) | Tusukan cepat, pendek | 🔴 Wajib | | |
| S5 | `sfx/cakar.wav` | Leak, Calon Arang wujud ritual | Cakaran + desis | 🔴 Wajib | | |
| S6 | `sfx/golok.wav` | Pengikut (golok) | Ayunan golok berat | 🟠 Penting | | |
| S7 | `sfx/asap-dupa.wav` | Calon Arang wujud manusia (serangan jarak jauh) | Hembusan asap + bisikan | 🟠 Penting | | |
| S8 | `sfx/kena.wav` | Semua serangan yang mengenai sasaran | Benturan tubuh (dipadu dengan S1–S7) | 🔴 Wajib | | |

## 5. Efek suara pertarungan — jurus

| No | File | Jurus (tokoh) | Bunyi | Prioritas | Sumber | Status |
|---|---|---|---|---|---|---|
| U1 | `sfx/tebasan-keris-pusaka.wav` | Tebasan Keris Pusaka (Pendekar) | Desing logam panjang + tebasan keras | 🔴 Wajib | | |
| U2 | `sfx/mantra-lingkar-suci.wav` | Mantra Lingkar Suci (Juru Mantra) | Dengung suci naik + ledakan cahaya, genta | 🔴 Wajib | | |
| U3 | `sfx/ramuan-penawar.wav` | Ramuan Penawar (Peramu) | Cairan dituang + kilau lembut | 🔴 Wajib | | |
| U4 | `sfx/langkah-bayangan.wav` | Langkah Bayangan (Pengintai) | Hembusan cepat "whoosh" + tusukan | 🔴 Wajib | | |
| U5 | `sfx/jerat-kabut.wav` | Jerat Kabut (Leak) | Kabut berdesis melilit | 🟠 Penting | | |
| U6 | `sfx/asap-wabah.wav` | Asap Wabah (Pengikut) | Asap menyebar + batuk samar | 🟠 Penting | | |
| U7 | `sfx/kutukan-kabut-ungu.wav` | Kutukan Kabut Ungu (Pemimpin Pengikut) | Gumam mantra gelap + dengung rendah | 🟠 Penting | | |
| U8 | `sfx/sebar-wabah.wav` | Sebar Wabah (Calon Arang) | Gemuruh kabut besar, angin ritual | 🔴 Wajib | | |

## 6. Efek suara pertarungan — keadaan tokoh dan pusaka

| No | File | Diputar saat | Bunyi | Prioritas | Sumber | Status |
|---|---|---|---|---|---|---|
| K1 | `sfx/terluka.wav` | Tokoh manusia terkena serangan | Erangan pendek (netral, bisa dipakai semua) | 🟠 Penting | | |
| K2 | `sfx/gugur.wav` | Pengelana/pengikut gugur | Jatuh berlutut ke tanah | 🔴 Wajib | | |
| K3 | `sfx/leak-lenyap.wav` | Leak gugur (memudar jadi asap) | Desis memudar | 🟠 Penting | | |
| K4 | `sfx/berubah-wujud.wav` | Calon Arang berubah ke wujud ritual | Gemuruh + jeritan + gong berat | 🔴 Wajib | | |
| K5 | `sfx/tirta-suci.wav` | Pusaka Tirta Suci dipakai | Percikan air + denting | 🟠 Penting | | |
| K6 | `sfx/kembang-kantil.wav` | Pusaka Kembang Kantil dipakai | Kilau lembut + angin harum | 🟠 Penting | | |
| K7 | `sfx/pulih.wav` | Nyawa/energi pulih (angka hijau) | Nada naik yang lembut | 🟢 Tambahan | | |
| K8 | `sfx/ritual-runtuh.wav` | Lingkaran ritual runtuh (Babak 5) | Batu retak + ledakan redup | 🟢 Tambahan | | |

## 7. Antarmuka (UI)

| No | File | Diputar saat | Bunyi | Prioritas | Sumber | Status |
|---|---|---|---|---|---|---|
| I1 | `sfx/pindah.wav` | Memindah pilihan menu (panah atas/bawah) | Klik kayu sangat pendek | 🔴 Wajib | | |
| I2 | `sfx/pilih.wav` | Memilih (Enter) | Ketukan bonang/kenong kecil | 🔴 Wajib | | |
| I3 | `sfx/batal.wav` | Kembali (Backspace/X) | Ketukan lebih rendah | 🟠 Penting | | |
| I4 | `sfx/gagal.wav` | Tombol nonaktif / energi batin tidak cukup | Bunyi tumpul "tidak bisa" | 🟠 Penting | | |
| I5 | `sfx/dialog.wav` | Baris dialog/narasi berikutnya muncul | Ketukan sangat halus | 🟢 Tambahan | | |
| I6 | `sfx/giliran.wav` | Giliran pengelana dimulai (menu muncul) | Denting kecil | 🟢 Tambahan | | |
| I7 | `sfx/mulai.wav` | Tombol "Mulai" di layar judul | Gong kecil | 🟢 Tambahan | | |

## 8. Suara adegan cerita (dari naskah)

| No | File | Adegan | Bunyi | Prioritas | Sumber | Status |
|---|---|---|---|---|---|---|
| C1 | `sfx/kentongan.wav` | Klimaks Babak 5 ("bunyi kentongan terdengar pelan") | Kentongan kayu | 🟢 Tambahan | | |
| C2 | `sfx/tabuhan.wav` | Klimaks Babak 5 ("disusul tabuhan rendah") | Tabuhan kendang/gong rendah | 🟢 Tambahan | | |
| C3 | `sfx/warga-berlari.wav` | Babak 4, desa diserang | Langkah banyak orang + teriakan samar | 🟢 Tambahan | | |

---

## Ringkasan

| Kelompok | Jumlah | Wajib |
|---|---|---|
| Musik latar | 7 | 3 |
| Jingle | 3 | 2 |
| Suasana | 4 | 0 |
| Serangan dasar | 8 | 5 |
| Jurus | 8 | 5 |
| Keadaan tokoh dan pusaka | 8 | 2 |
| Antarmuka | 7 | 2 |
| Adegan cerita | 3 | 0 |
| **Total** | **48** | **19** |

Untuk demo dan presentasi, cukup kerjakan dulu **19 suara berprioritas 🔴 Wajib**. Sisanya bisa menyusul.

**Catatan:** suara belum diputar di kode game. Setelah file-file ini ada di `assets/audio/`, front end
tinggal menyambungkannya ke momen-momen di atas.
