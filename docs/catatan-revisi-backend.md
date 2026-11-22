# Catatan Revisi Backend

> **Tujuan:** daftar perubahan yang **diusulkan** untuk backend (kode di `main/java/id/girah/...`),
> ditemukan saat mengerjakan front end dan asset. **Belum ada satu pun yang diterapkan.**
> Setiap butir perlu dibahas dan disetujui pemilik backend sebelum dikerjakan.
>
> File ini terus ditambah. Butir baru masuk ke bagian paling bawah tabel yang sesuai, dengan nomor baru.

## Cara memakai file ini

| Kolom | Arti |
|---|---|
| **Prioritas** | 🔴 **Wajib** (front end tidak bisa jalan/tersambung tanpanya) · 🟠 **Penting** (fitur cerita/visual bergantung padanya) · 🟢 **Saran** (perbaikan kecil, boleh ditunda) |
| **Status** | `Usulan` → `Disetujui` / `Ditolak` → `Selesai` (isi nama + tanggal saat berubah) |

---

## A. Struktur repo dan build

| No | Temuan | Dampak | Usulan | Prioritas | Status |
|---|---|---|---|---|---|
| A1 | `settings.gradle` memuat `include 'core', 'lwjgl3'`, tetapi folder `core/` **tidak ada**. Isi modul core ter-upload langsung ke root repo. | Proyek **tidak bisa di-build** dari root (`gradlew run` gagal). | Pindahkan `build.gradle` root + `main/` + `test/` ke `core/` (menjadi `core/src/main/java`, `core/src/test/java`), lalu buat `build.gradle` root yang benar. | 🔴 Wajib | Usulan |
| A2 | Ada **dua versi kode**: `main/java` + `test/java` (terbaru: punya paket `ai`, `pusaka`, `io`) dan `src/main/java` + `src/test/java` (lama, tanpa ketiganya). Gradle membaca `src/main/java`. | Yang ter-compile justru **versi lama**. | Pakai versi terbaru, hapus versi lama setelah dipastikan tidak ada yang hilang. | 🔴 Wajib | Usulan |
| A3 | Hasil build ikut ter-commit: `classes/`, `reports/`, `test-results/`, `tmp/`, `lwjgl3/build/`, `libs/core-0.1.0.jar`. | Repo berat dan membingungkan. | Hapus dari repo dan tambahkan `.gitignore` (`build/`, `.gradle/`, `*.class`, dll.). | 🟠 Penting | Usulan |
| A4 | Folder `assets/` untuk LibGDX belum ada, padahal `lwjgl3/build.gradle` memakainya sebagai working directory. | Game tidak bisa memuat gambar/suara. | Buat `assets/` di root dan isi dari `prototype-2.5d/assets/` saat front end digabung. | 🔴 Wajib | Usulan |
| A5 | Belum ada `README.md`. | Link repo akan dicantumkan di makalah sebagai bukti source code. | Tambah README: cara menjalankan, kontrol, struktur, anggota. | 🟢 Saran | Usulan |
| A6 | Commit dibuat lewat "Add files via upload" di web GitHub. | Riwayat perubahan tidak jelas; struktur folder mudah rusak (penyebab A1). | Pakai `git push` dari folder proyek LibGDX yang asli. | 🟢 Saran | Usulan |

## B. Kebutuhan front end (animasi dan tampilan)

| No | Temuan | Dampak | Usulan | Prioritas | Status |
|---|---|---|---|---|---|
| B1 | `BattleListener` hanya mengirim **teks** (`onNarasi`). Front end tidak diberi tahu **siapa melakukan apa ke siapa**. | Animasi serang/jurus/terluka **musuh** tidak bisa diputar dengan benar (aksi musuh dijalankan AI, bukan oleh front end). | Tambah event, misalnya: `default void onAksi(KarakterMitos aktor, JenisAksi jenis, List<KarakterMitos> sasaran) {}` (jenis: SERANG, JURUS, PUSAKA) dipanggil di `BattleManager` sebelum narasi dicatat. | 🔴 Wajib | Usulan |
| B2 | Tidak ada event saat karakter **menerima kerusakan**, **disembuhkan**, atau **gugur**. | Front end harus menebak dari perubahan nyawa untuk memutar animasi terluka/gugur dan angka kerusakan. | Tambah `onKerusakan(KarakterMitos target, int jumlah)`, `onPemulihan(KarakterMitos target, int jumlah)`, `onGugur(KarakterMitos k)` — atau satu `onHasilAksi(...)` yang memuat semuanya. | 🟠 Penting | Usulan |
| B3 | `KarakterMitos` tidak punya **kunci sprite** (id). Dua Leak hanya dibedakan lewat nama ("Leak 1", "Leak 2"). | Front end harus memetakan sprite dari nama kelas/nama tampilan, mudah rusak. | Tambah `getJenis()` / `getIdSprite()` (mis. `"pendekar"`, `"leak"`) di tiap subclass. | 🟢 Saran | Usulan |
| B4 | AI musuh bertindak setelah jeda tetap (`JEDA_BIASA_MS = 900`, `JEDA_BOS_MS = 1800`) **tanpa menunggu** pemain selesai membaca dialog (`AntreanDialog`). | Narasi musuh bisa menumpuk bila dialog panjang; animasi bisa bertumpuk. | Beri cara bagi front end untuk menahan AI sampai antrean dialog/animasi selesai (mis. `PengendaliMusuh.lanjutkan()` dipanggil front end), atau jeda yang bisa diatur. | 🟠 Penting | Usulan |
| B5 | Nama tampilan: kelas memakai `Senopati / Wiku / Balian / TelikSandi`, cerita memakai `Pendekar / Juru Mantra / Peramu / Pengintai`. | Label di HUD/dialog bisa tidak konsisten. | Sepakati satu. Usulan: nama cerita sebagai nama tampilan (`getPeran()`), nama kelas tetap. | 🟢 Saran | Usulan |
| B6 | `GirahGame.JUDUL = "Bayang-Bayang Girah"`, judul di dokumen cerita "Calon Arang: Bayang-Bayang Girah". | Judul jendela berbeda dari judul game. | Samakan. | 🟢 Saran | Usulan |
| B7 | Backend tidak menyimpan apakah sebuah aksi **jarak dekat** (tokoh harus maju ke sasaran, misalnya keris Pendekar, cakar Leak) atau **jarak jauh** (dilakukan dari tempatnya, misalnya kutukan Calon Arang, Lingkar Perlindungan Bharadah). | Gerak maju ke sasaran adalah urusan front end dan **sudah dibuat di prototipe**, tetapi daftar tokoh/aksi jarak dekat sekarang ditulis manual di front end (`ArenaPrototype.isMelee`). Bila backend menambah aksi atau tokoh baru, front end bisa salah menebak. | Tambahkan informasi jarak pada aksi, misalnya `enum JarakAksi { DEKAT, JAUH }` dan `JarakAksi getJarakSerang()` / `getJarakJurus()` di `KarakterMitos`, lalu ikut dikirim di event `onAksi` (B1). Ini hanya data; logika battle tidak perlu berubah. | 🟢 Saran | Usulan |

## C. Logika battle (perilaku yang perlu diputuskan)

| No | Temuan | Dampak | Usulan | Prioritas | Status |
|---|---|---|---|---|---|
| C1 | `Balian.gunakanJurus` ke kawan yang nyawanya **penuh**: energi tidak terpakai, tetapi **giliran tetap habis** (`BattleManager.selesaikanAksi` tetap dipanggil). | Pemain kehilangan giliran tanpa efek. | Pilih salah satu: (a) `getSasaranJurus()` hanya mengembalikan kawan yang terluka, atau (b) lempar exception agar giliran tidak habis. | 🟢 Saran | Usulan |
| C2 | `TelikSandi.gunakanJurus` melempar `IllegalStateException` bila dipakai dua kali dalam satu ronde. | Game bisa crash jika UI lupa mengecek. | Tetap boleh (sudah terdokumentasi), tetapi UI **wajib** menonaktifkan tombol berdasarkan `bisaMemakaiJurus()`. Catat sebagai kontrak front end. | 🟢 Saran | Usulan |

## D. Fitur cerita yang belum ada di backend

Dari dokumen alur cerita dan brief arah visual (bagian E). Perlu diputuskan **mana yang masuk lingkup** tugas.

| No | Kebutuhan | Kondisi sekarang | Asset yang menunggu | Prioritas | Status |
|---|---|---|---|---|---|
| D1 | Musuh **Pengikut** dan **Pemimpin Pengikut** (Babak 1, 4, 5) | Belum ada kelas (baru disebut di komentar `StrategiMusuh`) | Sprite pengikut, pemimpin pengikut | 🟠 Penting | Usulan |
| D2 | **Ratna** dan **Mpu Bharadah** di arena Babak 5 (dilindungi / membantu) | Belum ada kelas | Sprite battle Ratna, Bharadah | 🟠 Penting | Usulan |
| D3 | **Sumber ritual** yang dihancurkan sebelum batas giliran habis (fase 3) | Ada `batasRonde` + `WAKTU_HABIS`, tetapi belum ada target "sumber ritual" | Sprite sumber ritual (utuh/retak/hancur) | 🟠 Penting | Usulan |
| D4 | **Calon Arang berganti wujud** (manusia → wujud ritual) di fase 2 | `CalonArang` hanya satu wujud; `isNyawaRendah()` bisa dipakai sebagai pemicu | Sprite wujud ritual + animasi berubah wujud | 🟠 Penting | Usulan |
| D5 | **Kabut mengurangi akurasi** (fase 1) dan **kerusakan berkala** (fase 3) | Belum ada | Lapisan kabut, efek kerusakan berkala | 🟢 Saran | Usulan |
| D6 | **Sistem dialog, pilihan, kepercayaan (Ratna/warga), evakuasi, tiga ending** | Belum ada sama sekali | Layar cerita, potret dialog, UI pilihan | 🟠 Penting | Usulan |
| D7 | **Pertarungan tiga fase** dalam satu battle akhir | `BattleManager` satu fase | — | 🟢 Saran | Usulan |

---

## Riwayat

| Tanggal | Perubahan | Oleh |
|---|---|---|
| 2026-11-22 | Catatan awal: A1–A6, B1–B6, C1–C2, D1–D7 dari pemeriksaan kode backend, brief, dan prototipe 2.5D | Front end (usulan) |
| 2026-11-23 | Tambah B7: informasi jarak dekat/jauh per aksi (gerak maju sudah dibuat di front end) | Front end (usulan) |
