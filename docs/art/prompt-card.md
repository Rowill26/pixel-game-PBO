# Prompt Card — Codixel

> **Status:** DRAF v3 (gaya 2.5D). Perbarui bagian "Seed yang disetujui" setelah style board disetujui.

Supaya hasil Codixel tidak melenceng dari `art-direction-brief.md`, setiap prompt terdiri dari
**bagian tetap** (selalu disalin utuh) dan **bagian khusus** (diganti per asset).

Hasil Codixel selalu dianggap **draf**. Sebelum masuk `assets/raw/`, rapikan di Pixelorama:

1. Pastikan grid pixel benar (tidak ada pixel setengah/ganda).
2. Hapus pixel acak dan noise.
3. Pastikan semua warna ada di `palette-girah.gpl` (64 warna).
4. Pastikan ukuran kanvas dan titik jangkar sesuai tabel C1 dan C3 di brief.
5. Bandingkan berdampingan dengan seed di ukuran asli, **dan** di atas latar yang sebenarnya.

Gambar di `referensi asset/` boleh dipakai untuk memahami bentuk dan suasana, tetapi **jangan**
diberikan ke Codixel untuk ditiru atau di-trace.

---

## Bagian tetap

```text
TUJUAN: asset produksi untuk game RPG naratif 2.5D pixel art "Calon Arang: Bayang-Bayang Girah"
GAYA: pixel art untuk adegan 2.5D (lapisan berkedalaman + efek kamera ditambahkan di engine);
      sudut pandang dari depan dengan kamera agak menunduk (±30 derajat)
LATAR: Kerajaan Daha; perpaduan Jawa Timur kuno (Majapahit: bata merah, candi ramping, pendopo)
       dan Bali (batu paras, meru beratap ijuk, bale alang-alang, penjor, kain poleng, kamboja)
SUASANA: mistis dan muram, warna redup seperti warna tanah, tidak terlalu hangat;
         kisah tragedi seorang ibu, bukan kisah monster
ARAH VISUAL: hanya warna dari palet docs/art/palette-girah.hex (64 warna); hue shift: bayangan
             dingin kebiruan, terang kekuningan; sprite tokoh memakai selective outline 1px
             (warna tergelap ramp di sebelahnya); latar dan bangunan tanpa outline;
             tanpa anti-aliasing, gradasi halus, atau blur di dalam sprite
TEKNIS: kanvas [UKURAN], latar transparan, jangkar bawah-tengah (posisi kaki di tanah)
KUNCI: pertahankan proporsi, siluet, kostum, outline, dan arah cahaya dari seed
HINDARI: teks, label, bingkai, objek ganda, bagian terpotong, darah/gore,
         salinan persis topeng sakral (Rangda, Barong), aset atau tampilan khas game lain,
         busana Jawa/Bali modern (blangkon, beskap, kebaya berpayet)
```

## Bagian khusus — sprite tokoh (64×64, tiga-perempat menghadap kamera)

Pengelana menghadap **kiri** (berdiri di kanan arena). Pihak Girah menghadap **kanan**.

### Pendekar / `Senopati` (seed tokoh manusia)
```text
SUBJEK: Pendekar, panglima garis depan; bahu lebar, dada terbuka dengan selendang merah tua,
        kain merah (Merah 2-3) dengan ornamen emas antik, ikat kepala, keris di tangan kanan
        terpisah jelas dari badan
ARAH: tiga-perempat menghadap kiri, wajah terlihat
KANVAS: 64x64, tinggi badan ±44 px
AKSI: idle, 4 frame, napas pelan
```

### Juru Mantra / `Wiku`
```text
SUBJEK: Juru Mantra; kain putih (Putih 1-3) melebar ke bawah, selempang emas,
        tongkat kayu panjang lebih tinggi dari kepala dengan ujung bercahaya
ARAH: tiga-perempat menghadap kiri
KANVAS: 64x64
AKSI: idle, 4 frame
```

### Peramu / `Balian`
```text
SUBJEK: Peramu, tabib; tubuh bulat lembut, kain hijau lumut (Daun 3-5), kantung ramuan
        dan labu kecil di pinggang, rambut disanggul
ARAH: tiga-perempat menghadap kiri
KANVAS: 64x64
AKSI: idle, 4 frame
```

### Pengintai / `TelikSandi`
```text
SUBJEK: Pengintai; tubuh ramping condong ke depan, kain gelap (Bayangan 2-3, Langit 2),
        penutup wajah bagian bawah, belati pendek
ARAH: tiga-perempat menghadap kiri
KANVAS: 64x64
AKSI: idle, 4 frame, berjinjit siap melesat
```

### Leak / makhluk gaib (seed makhluk gaib)
```text
SUBJEK: makhluk gaib melayang; rambut panjang hitam menjuntai, wajah pucat (Batu 4-5),
        mata menyala ungu (Girah 4), tangan bercakar, bagian bawah tubuh memudar menjadi
        kabut ungu (dithering boleh); tanpa organ dalam atau darah
ARAH: tiga-perempat menghadap kanan
KANVAS: 64x64 (bayangan tanah dibuat terpisah)
AKSI: idle, 4 frame, melayang naik-turun 1-2 px
```

### Pengikut Calon Arang
```text
SUBJEK: pengikut, manusia; tubuh membungkuk, kain gelap (Girah 1-2, Merah 1), wajah sebagian
        tertutup bayangan, membawa pedupaan yang berasap
ARAH: tiga-perempat menghadap kanan
KANVAS: 64x64
AKSI: idle, 4 frame
```

### Calon Arang — wujud manusia
```text
SUBJEK: Calon Arang, janda sakti dari Girah; MANUSIA, perempuan tua berwibawa, rambut putih
        panjang terurai, kain merah ritual, gelang emas; wajah tegang namun sedih
ARAH: tiga-perempat menghadap kanan
KANVAS: 64x64
AKSI: idle, 6 frame, rambut dan kain bergerak lambat
```

### Calon Arang — wujud ritual (Babak 5 fase 2–3)
```text
SUBJEK: wujud ritual Calon Arang, terinspirasi unsur Rangda TANPA menyalin topeng sakralnya:
        surai putih sangat panjang menjuntai sampai tanah (Putih 1-3), lengan dan kaki berbelang
        merah-putih, kuku sangat panjang, mahkota emas antik berhias bunga kamboja, kain poleng
        di pinggang; WAJAH tetap wajah Calon Arang (perempuan tua) dengan riasan ritual merah dan
        mata menyala, bukan topeng bertaring
ARAH: tiga-perempat menghadap kanan, kedua lengan terangkat
KANVAS: 128x128
AKSI: idle, 6 frame, surai bergoyang berat
```

### Mpu Bharadah, Ratna Manggali (sekutu Babak 5)
```text
SUBJEK: [Mpu Bharadah: guru tua, jubah pertapa putih, janggut putih, tasbih]
        [Ratna Manggali: perempuan muda ramping, menunduk, kain terang lembut, sanggul sederhana]
ARAH: tiga-perempat menghadap kiri
KANVAS: 64x64
AKSI: idle, 4 frame
```

### Warga / NPC latar
```text
SUBJEK: warga desa [laki-laki petani / perempuan membawa sesajen / anak / orang tua sakit],
        kain dan kebaya pudar
KANVAS: 48x48
AKSI: idle, 2 frame
```

### Animasi lain (ganti baris AKSI)

| Animasi | Baris AKSI |
|---|---|
| serang | `AKSI: serang dasar, 4 frame: ancang-ancang, ayun, kena, kembali` |
| jurus | `AKSI: [nama jurus], 6 frame: kumpulkan tenaga, lepaskan, tahan, kembali` |
| terluka | `AKSI: terluka, 2 frame: terdorong ke belakang 2px, kembali` |
| gugur | `AKSI: gugur, 4 frame: goyah, berlutut, roboh; frame terakhir ditahan` |
| berubah wujud (Calon Arang) | `AKSI: berubah wujud, 8 frame: asap dupa menyelubungi, surai memanjang, wujud ritual muncul` |

## Bagian khusus — properti lingkungan

```text
SUBJEK: properti [gapura bata candi bentar / bale beratap alang-alang / meru beratap ijuk /
        penjor / pohon kamboja / beringin dengan kain poleng / arca batu berlumut / nisan tua /
        tempayan / sesajen canang / obor bambu / pagar bambu / lumbung padi]
ARAH: tampak depan, kamera agak menunduk (bagian atas benda sedikit terlihat)
KANVAS: [16x16 sampai 256x256], jangkar bawah-tengah
DETAIL: material sesuai B7 di brief; tanpa outline luar untuk benda besar
```

## Bagian khusus — lapisan latar

```text
SUBJEK: lapisan [langit / latar jauh / latar tengah / tanah / depan] untuk lokasi [nama lokasi]
VERSI: [cerita = ramai / arena battle = lenggang, tengah kosong]
KANVAS: 640x360 (atau lebih lebar untuk parallax), transparan kecuali langit
ARAH VISUAL: makin jauh makin pudar dan mendekati warna langit; tanpa tokoh dan teks;
             cahaya TIDAK dilukis (sprite cahaya ditambahkan terpisah)
```

## Bagian khusus — potret dialog (gaya penyajian Persona)

```text
SUBJEK: potret [nama tokoh], setengah badan (pinggang ke atas), menghadap kanan
EKSPRESI: [netral / sedih / murka / ...]
KANVAS: 128x192, jangkar bawah-kiri, badan terpotong di tepi bawah kanvas
DETAIL: bayangan boleh 2-3 pixel; wajah adalah fokus utama
KUNCI: untuk ekspresi selain netral, salin potret netral dan HANYA ubah area wajah
```

| Tokoh | Ekspresi |
|---|---|
| Calon Arang | netral, murka, sedih |
| Ratna Manggali | netral, sedih, bahagia, kecewa |
| Mpu Bharadah | netral, tegas |
| Bahula | netral, ragu, menyesal |
| Raja Erlangga | netral, tegas |
| Janda Wening | sakit, pulih |
| Pendekar, Peramu, Pengintai, Juru Mantra | netral, tegas |
| Pejabat kerajaan, Kepala Desa, Pengikut | netral |

## Bagian khusus — efek dan cahaya

```text
SUBJEK: efek [Tebasan Keris Pusaka | Mantra Lingkar Suci | Ramuan Penawar | Langkah Bayangan |
        Jerat Kabut | Sebar Wabah | Lingkar Perlindungan | Kutukan | Ritual runtuh]
WARNA: inti Api 3 / Putih 3 → warna peran → tepi gelap
KANVAS: [64x64 / 96x96 untuk satu sasaran, ±240x140 untuk area]
AKSI: [jumlah] frame, muncul → puncak → memudar
```

Sprite cahaya (lingkaran lembut) dibuat sekali dalam beberapa ukuran dan diwarnai lewat kode.

## Seed yang disetujui

| Seed | File | Disetujui |
|---|---|---|
| Pendekar idle frame 0 | `assets/raw/karakter/senopati/idle_0.png` | belum |
| Leak idle frame 0 | `assets/raw/karakter/leak/idle_0.png` | belum |
| Potret Ratna netral | `assets/raw/potret/ratna_netral.png` | belum |
| Lapisan latar arena Hutan Beringin | `assets/raw/latar/arena/hutan_beringin/` | belum |
