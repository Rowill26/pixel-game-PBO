# Panduan Integrasi Back End untuk Front End

> Untuk: anggota front end (branch `nambah-asset`).
> Isi: cara menyatukan prototipe arena ke proyek utama, lalu mengganti battle "pertunjukan"
> dengan `BattleManager` sungguhan. Semua contoh kode di sini sudah dicoba dikompilasi
> terhadap kode back end yang ada di `main`.

## Daftar isi

1. [Sinkronkan branch](#1-sinkronkan-branch)
2. [Satukan prototipe ke proyek utama](#2-satukan-prototipe-ke-proyek-utama)
3. [Membuat pertarungan](#3-membuat-pertarungan)
4. [Menu aksi pemain](#4-menu-aksi-pemain)
5. [Event → animasi](#5-event--animasi)
6. [AI musuh](#6-ai-musuh)
7. [Dialog, urutan giliran, HUD](#7-dialog-urutan-giliran-hud)
8. [Akhir pertarungan](#8-akhir-pertarungan)
9. [Tabel data](#9-tabel-data)
10. [Aturan yang wajib dipatuhi](#10-aturan-yang-wajib-dipatuhi)
11. [Layar cerita](#11-layar-cerita)
12. [Alur cerita dan pertarungan](#12-alur-cerita-dan-pertarungan)
13. [Yang belum ada di back end](#13-yang-belum-ada-di-back-end)

---

## 1. Sinkronkan branch

`main` sudah berisi struktur proyek yang benar dan semua perubahan back end dari catatan revisi.
Gabungkan dulu ke `nambah-asset`. Sudah dicoba: **tidak ada konflik**.

**GitHub Desktop:** pilih branch `nambah-asset` → menu **Branch → Update from main** → **Push origin**.

**Terminal:**

```bash
git switch nambah-asset
git pull
git merge origin/main
git push
```

## 2. Satukan prototipe ke proyek utama

Sekarang ada dua proyek Gradle: proyek utama (root) dan `prototype-2.5d/`. Game akhir harus satu
proyek, supaya kode front end bisa memakai `BattleManager`. Langkah ini **sudah diuji**: setelah
dijalankan, `./gradlew build` lolos dan game berjalan dengan semua asset (arena, sprite, font, HUD).

Dependensi `gdx-freetype` untuk font monogram **sudah ditambahkan** di `core/build.gradle` dan
`lwjgl3/build.gradle`, jadi build tidak perlu diubah selain `mainClass`.

Jalankan dari folder root repo (Git Bash), setelah langkah 1:

```bash
# 1. Asset pindah ke assets/ di root (working directory game)
git mv prototype-2.5d/assets assets

# 2. Kode tampilan pindah ke modul core (paket tetap id.girah.proto)
git mv prototype-2.5d/src/main/java/id/girah/proto core/src/main/java/id/girah/proto

# 3. Launcher memakai backend desktop, jadi pindah ke modul lwjgl3
mkdir -p lwjgl3/src/main/java/id/girah/proto
git mv core/src/main/java/id/girah/proto/Launcher.java lwjgl3/src/main/java/id/girah/proto/Launcher.java

# 4. Launcher lama (layar kosong) tidak dipakai lagi
git rm lwjgl3/src/main/java/id/girah/lwjgl3/Lwjgl3Launcher.java

# 5. Build terpisah prototipe dihapus, supaya tidak ada dua versi (lihat catatan revisi A1/A2)
git rm -r prototype-2.5d/build.gradle prototype-2.5d/settings.gradle prototype-2.5d/gradle.properties \
          prototype-2.5d/gradlew prototype-2.5d/gradlew.bat prototype-2.5d/gradle prototype-2.5d/.gitignore

# 6. File cache Python yang ikut ter-commit
git rm -r --cached prototype-2.5d/tools/__pycache__
```

Lalu ubah satu baris di `lwjgl3/build.gradle`:

```gradle
application {
    mainClass = 'id.girah.proto.Launcher'
}
```

Jangan hapus `core/src/main/java/id/girah/GirahGame.java`: konstanta `GirahGame.JUDUL` dipakai
untuk judul Babad. Judul jendela di `Launcher` sebaiknya juga memakai `GirahGame.JUDUL`.

Folder `prototype-2.5d/` tetap berisi `tools/`, `screenshots/`, dan `README.md`. Perbarui perintah
di README: game sekarang dijalankan dari **root** repo:

```bash
./gradlew :lwjgl3:run
./gradlew :lwjgl3:run --args="--arena pemakaman --demo"
```

Cek: `./gradlew build` harus lolos (semua test back end ikut berjalan), lalu commit:
`Satukan prototipe arena ke proyek utama`.

## 3. Membuat pertarungan

Semua kelas back end ada di `core`, paket `id.girah.*`, dan **tidak** bergantung pada layar.
Satu pertarungan = satu `BattleManager`.

```java
// Tas Pusaka dibuat SEKALI untuk seluruh permainan dan dipakai ulang di setiap pertarungan,
// jadi pusaka yang terpakai tidak kembali. Untuk sementara, buat pengelana dan musuh baru
// (nyawa penuh) untuk setiap pertarungan; aturan nyawa antar-babak menyusul bersama D6.
Senopati pendekar = new Senopati("Arya");
Wiku juruMantra = new Wiku("Dang Hyang");
TelikSandi pengintai = new TelikSandi("Kebo Ijo");
Balian peramu = new Balian("Jero Balian");

TasPusaka tas = new TasPusaka();
tas.tambah(new TirtaSuci(), 3);
tas.tambah(new KembangKantil(), 2);

BattleManager battle = new BattleManager(
        List.of(pendekar, juruMantra, pengintai, peramu),
        List.of(new Leak("Leak 1"), new Leak("Leak 2"), new Pengikut("Pengikut")),
        BattleManager.TANPA_BATAS_RONDE,   // atau angka, mis. 8 untuk fase ritual Babak 5
        tas);

battle.tambahListener(layarBattle);         // layar implements BattleListener (lihat bagian 5)
battle.mulai();                             // panggil di Screen.show(), bukan di konstruktor
```

Kelas karakter yang tersedia:

| Pihak | Kelas | Konstruktor |
|---|---|---|
| Pengelana | `Senopati`, `Wiku`, `TelikSandi`, `Balian` | `new Senopati("nama")` |
| Girah | `Leak`, `Pengikut` | `new Leak("Leak 1")` (nama unik per musuh) |
| Girah (bos) | `PemimpinPengikut`, `CalonArang` | `new PemimpinPengikut()`, `new CalonArang()` |

Untuk menghubungkan karakter ke sprite di arena, buat peta saat pertarungan dimulai:

```java
Map<KarakterMitos, Actor25D> aktorDi = new HashMap<>();
for (KarakterMitos k : battle.getKelompok()) aktorDi.put(k, buatAktor(k.getIdSprite(), k.getNama()));
for (KarakterMitos k : battle.getLawan())    aktorDi.put(k, buatAktor(k.getIdSprite(), k.getNama()));
```

Dua Leak punya `getIdSprite()` yang sama (`"leak"`) tetapi objek `KarakterMitos` berbeda, jadi
masing-masing mendapat aktornya sendiri.

`arena.json` tetap menentukan **posisi** tokoh, lampu, dan latar, tetapi **siapa** yang bertarung
(jumlah dan jenis musuh) ditentukan oleh daftar yang diberikan ke `BattleManager`.

## 4. Menu aksi pemain

Menu hanya muncul jika `battle.getStatus() == StatusBattle.GILIRAN_PEMAIN` **dan** tidak ada
animasi yang sedang diputar. Pengelana yang sedang bergiliran: `battle.getAktor()`.

| Tombol | Aktif jika | Pilihan sasaran |
|---|---|---|
| Serang | selalu | `battle.getSasaranSerang()` |
| Jurus | `aktor.bisaMemakaiJurus() && !battle.getSasaranJurus().isEmpty()` | `battle.getSasaranJurus()`; jika `aktor.getSasaranJurus() == JenisSasaran.SEMUA_LAWAN` tidak perlu memilih, kirim `null` |
| Pusaka | `!battle.getTasPusaka().isKosong()` | daftar `battle.getTasPusaka().getDaftarPusaka()`, lalu sasaran `battle.getSasaranPusaka(pusaka)`; pusaka yang daftarnya kosong dinonaktifkan |

Nama jurus untuk menu: `aktor.getNamaJurus()`, biayanya `aktor.getBiayaJurus()`. Jumlah pusaka:
`tas.jumlah(pusaka)`, keterangannya `pusaka.getDeskripsi()`.

```java
void pilihSerang(KarakterMitos sasaran) {
    battle.serang(sasaran);
}

void pilihJurus(KarakterMitos sasaranAtauNull) {
    try {
        battle.gunakanJurus(sasaranAtauNull);
    } catch (EnergiBatinHabisException e) {
        tampilkanPesan(e.getMessage());     // "Arya kehabisan energi batin: butuh 20, tersisa 10"
    }                                       // giliran TIDAK habis: menu tetap terbuka
}

void pilihPusaka(Pusaka pusaka, KarakterMitos sasaran) {
    battle.gunakanPusaka(pusaka, sasaran);
}
```

Sasaran yang tidak ada di daftar di atas akan ditolak dengan `IllegalArgumentException`. Itu tanda
bug di UI, bukan kesalahan pemain, jadi tidak perlu ditangkap.

## 5. Event → animasi

Layar battle meng-implement `BattleListener`. Semua method punya isi kosong bawaan, jadi cukup
override yang perlu.

**Penting:** event dipanggil **di dalam** `battle.serang(...)` dan sejenisnya, jadi saat `onAksi`
tiba, nyawa karakter **sudah** berubah. Simpan `HasilAksi` ke antrean, lalu putar satu per satu.

```java
private final Deque<HasilAksi> antreanAnimasi = new ArrayDeque<>();
private boolean sedangAnimasi;

@Override
public void onAksi(HasilAksi hasil) {
    antreanAnimasi.add(hasil);
}

// dipanggil setiap frame dari render()
void perbaruiAnimasi() {
    if (!sedangAnimasi && !antreanAnimasi.isEmpty()) {
        putar(antreanAnimasi.poll());
    }
}

void putar(HasilAksi h) {
    sedangAnimasi = true;
    Actor25D pelaku = aktorDi.get(h.getAktor());
    String anim = h.getJenis() == JenisAksi.SERANG ? "serang" : "jurus";
    boolean maju = h.getJarak() == JarakAksi.DEKAT;   // pengganti isMelee()
    // ... maju ke h.getSasaran().get(0) jika `maju`, putar `anim` ...
    // pada frame benturan, untuk setiap perubahan:
    for (PerubahanStatus p : h.getPerubahan()) {
        Actor25D a = aktorDi.get(p.getKarakter());
        if (p.getKerusakan() > 0)      { /* animasi "terluka", angka kuning p.getKerusakan() */ }
        if (p.getPemulihanNyawa() > 0) { /* angka hijau p.getPemulihanNyawa() */ }
        if (p.isGugur())               { /* animasi "gugur", tahan frame terakhir */ }
    }
    // setelah animasi selesai: sedangAnimasi = false;
}
```

Isi `HasilAksi`:

| Method | Isi |
|---|---|
| `getAktor()` | pelaku |
| `getJenis()` | `SERANG`, `JURUS`, atau `PUSAKA` |
| `getNamaAksi()` | `"Serangan"`, nama jurus (`"Sebar Wabah"`), atau nama pusaka (`"Tirta Suci"`) |
| `getJarak()` | `DEKAT` (maju ke sasaran) atau `JAUH` (di tempat) |
| `getSasaran()` | sasaran yang dipilih; jurus area: semua lawan yang hidup |
| `getPerubahan()` | semua karakter yang nyawa/energinya berubah, **termasuk energi yang dipakai pelaku** |
| `getPerubahan(k)` | perubahan untuk satu karakter (`Optional`) |
| `getYangGugur()` | karakter yang gugur karena aksi ini |

Kerusakan di `PerubahanStatus` adalah kerusakan **sebenarnya** (Leak dengan nyawa 1 menerima 1,
bukan 12), jadi angka di layar selalu cocok dengan bar nyawa.

Urutan event untuk satu aksi: `onAksi` → `onNarasi` → lalu `onBattleSelesai`, **atau**
`onRondeBaru` (jika ronde berganti) dan `onGiliranBaru`. Aksi yang ditolak tidak memicu event.

## 6. AI musuh

```java
PengendaliMusuh ai = new PengendaliMusuh(battle, new StrategiDasar(), Gdx.app::postRunnable);
ai.setTungguIzinFrontEnd(true);   // AI menunggu animasi dan dialog selesai
ai.aktifkan();                    // sebelum battle.mulai()
battle.mulai();
```

Saat giliran musuh, thread AI langsung berpikir, tetapi aksinya **ditahan** sampai front end
memanggil `ai.izinkanBertindak()`. Panggil setiap frame ketika layar sedang diam. Ini aman,
karena izin hangus sendiri di setiap giliran musuh baru:

```java
// di render(), setelah perbaruiAnimasi()
if (battle.getStatus() == StatusBattle.GILIRAN_MUSUH
        && !sedangAnimasi && antreanAnimasi.isEmpty()
        && !battle.getAntreanDialog().adaBaris()) {
    ai.izinkanBertindak();
}
```

`ai.isSedangBerpikir()` bisa dipakai untuk menampilkan tanda "..." di atas musuh. Bos
(`CalonArang`, `PemimpinPengikut`) berpikir lebih lama dari musuh biasa.

Saat layar ditutup (`Screen.hide()` / `dispose()`): **wajib** `ai.hentikan()`.

## 7. Dialog, urutan giliran, HUD

**Dialog.** `battle.getAntreanDialog()` berisi baris narasi yang belum ditampilkan, sudah dipecah
per baris. Tampilkan dari situ (satu baris per `Enter`), dan abaikan `onNarasi` supaya teks tidak
dobel:

```java
AntreanDialog dialog = battle.getAntreanDialog();
if (tekanEnter && dialog.adaBaris()) {
    tampilkanBaris(dialog.ambilBerikutnya());
}
// tombol "lewati": dialog.kosongkan();
```

Ucapan bos saat nyawanya rendah (Calon Arang, Pemimpin Pengikut) ikut masuk ke narasi ini.

**Urutan giliran** (ikon di atas layar): `battle.getAntrianGiliran()`, diperbarui di
`onGiliranBaru` dan `onRondeBaru`. Saat Pengintai memakai Langkah Bayangan, ia muncul dua kali
berurutan; itu memang giliran tambahannya. Nomor ronde: `battle.getRonde()`, batasnya
`battle.getBatasRonde()` (0 = tanpa batas).

**HUD.** Nama di kartu status: `k.getNama()`; label peran: `k.getPeran()` ("Pendekar",
"Juru Mantra", ...). Nyawa dan energi: `getNyawa()`, `getNyawaMaks()`, `getEnergiBatin()`,
`getEnergiBatinMaks()`. Karena nyawa sudah berubah saat `onAksi`, simpan nilai yang
**ditampilkan** di HUD sendiri dan kurangi pada frame benturan dengan `p.getPerubahanNyawa()`,
supaya bar tidak turun sebelum serangan kena.

## 8. Akhir pertarungan

```java
@Override
public void onBattleSelesai(StatusBattle hasil) {
    try {
        Path file = new BabadExporter(Gdx.files.local("babad").file().toPath()).ekspor(battle);
        tampilkanPesan("Babad tersimpan: " + file.getFileName());
    } catch (IOException e) {
        tampilkanPesan("Babad gagal disimpan: " + e.getMessage());
    }
}
```

Babad tersimpan di `assets/babad/babad_<tanggal>_<jam>.txt` (folder ini sudah di-`.gitignore`).
Setelah animasi terakhir dan dialog selesai:

| `hasil` | Lanjutan |
|---|---|
| `MENANG` | lanjut ke naskah berikutnya |
| `KALAH` | **ulangi pertarungan itu**: buat ulang semua pengelana dan musuh, lalu `BattleManager` baru (konstruktor menolak kelompok yang semuanya gugur) |
| `WAKTU_HABIS` | hanya terjadi jika ada batas ronde (fase ritual Babak 5) → Ending 3, Akhir Tragis |

Setelah selesai, `battle.getAktor()` melempar `IllegalStateException`. Selalu cek
`battle.getStatus().isSelesai()` dulu.

## 9. Tabel data

| Kelas | `getIdSprite()` | `getPeran()` | Serangan | Jurus | Jarak serang / jurus |
|---|---|---|---|---|---|
| `Senopati` | `pendekar` | Pendekar | keris | Tebasan Keris Pusaka (1 lawan) | DEKAT / DEKAT |
| `Wiku` | `juru_mantra` | Juru Mantra | tongkat | Mantra Lingkar Suci (semua lawan) | DEKAT / JAUH |
| `TelikSandi` | `pengintai` | Pengintai | menikam | Langkah Bayangan (1 lawan + giliran tambahan) | DEKAT / DEKAT |
| `Balian` | `peramu` | Peramu | melempar getah | Ramuan Penawar (1 kawan terluka) | **JAUH** / JAUH |
| `Leak` | `leak` | Leak | cakar | Jerat Kabut (1 lawan) | DEKAT / JAUH |
| `Pengikut` | `pengikut` | Pengikut | golok | Asap Wabah (semua lawan) | DEKAT / JAUH |
| `PemimpinPengikut` | `pemimpin_pengikut` | Pemimpin Pengikut | tongkat ritual | Kutukan Kabut Ungu (1 lawan) | DEKAT / JAUH |
| `CalonArang` | `calon_arang` | Janda Sakti dari Girah | asap dupa | Sebar Wabah (semua lawan) | **JAUH** / JAUH |

Catatan: serangan Peramu kini **JAUH** (sesuai narasi "melempar"); di prototipe lama ia maju.
Pengikut yang kalah **menyerah**, bukan mati, jadi animasi `gugur`-nya sebaiknya berlutut.

## 10. Aturan yang wajib dipatuhi

1. **Semua pemanggilan back end di render thread.** Satu-satunya thread lain adalah thread AI, dan
   ia sudah mengirim hasilnya lewat `Gdx.app.postRunnable`.
2. **Tombol jurus** memakai `bisaMemakaiJurus()` **dan** `getSasaranJurus()`. Pengintai yang
   memakai Langkah Bayangan dua kali dalam satu ronde akan melempar `IllegalStateException`.
3. **Tangkap `EnergiBatinHabisException`** saat memanggil `gunakanJurus`, lalu tampilkan pesannya.
4. **Jangan memanggil `serang`/`gunakanJurus`/`gunakanPusaka` saat giliran musuh.** Back end
   tidak membedakan pemain dan AI; pembedanya hanya `getStatus()`.
5. **`ai.hentikan()`** saat layar battle ditutup.
6. Jangan mengubah nyawa langsung (tidak ada setter publik). Semua lewat `BattleManager`.

## 11. Layar cerita

Back end kini membaca naskah dan mengingat pilihan pemain (paket `id.girah.cerita`). Format
naskah untuk penulis: [format-naskah.md](format-naskah.md). Naskah lama tetap terbaca apa adanya.

`StoryLayer` cukup mengganti pembacaan JSON-nya sendiri dengan `PemutarNaskah`:

```java
// sekali untuk seluruh permainan, seperti Tas Pusaka
StatusCerita statusCerita = new StatusCerita();

// setiap kali sebuah naskah dimulai
Naskah naskah;
try {
    naskah = PembacaNaskah.baca(nama, Gdx.files.internal("cerita/" + nama + ".json").readString("UTF-8"));
} catch (NaskahTidakValidException e) {
    Gdx.app.error("Cerita", e.getMessage());   // pesan menyebut file dan letak kesalahannya
    return;
}
PemutarNaskah pemutar = new PemutarNaskah(naskah, statusCerita);
// naskah.getJudul(), getMisi(), getArena(), getMode() untuk latar dan judul babak

// setiap kali layar siap menampilkan langkah berikutnya
if (pemutar.isSelesai()) {
    String berikutnya = pemutar.getLanjutan();   // "then": arena pertarungan atau naskah berikutnya
} else if (pemutar.isPilihan()) {
    List<OpsiPilihan> opsi = pemutar.getOpsiTersedia();   // opsi.get(i).getTeks() untuk tombol
    // saat pemain memilih tombol ke-i:
    pemutar.pilih(i);                                     // efek dicatat, balasan tampil berikutnya
} else {
    BarisDialog baris = pemutar.getBaris();
    // baris.getIdPembicara() (kunci potret, "narator" = tanpa potret), getNama(), getEkspresi(), getTeks()
    // saat pemain menekan Enter:
    pemutar.lanjut();
}
```

Baris dan opsi yang syaratnya tidak terpenuhi sudah dilewati oleh `PemutarNaskah`; layar tidak
perlu memeriksa apa pun. Indeks untuk `pilih(i)` adalah indeks di `getOpsiTersedia()`.

Setelah pertarungan fase terakhir Babak 5 selesai dengan `MENANG` atau `WAKTU_HABIS`:

```java
statusCerita.catatHasilRitual(hasil);
Ending ending = PenentuEnding.tentukan(statusCerita);   // setelah pilihan akhir dibuat
```

## 12. Alur cerita dan pertarungan

Alur permainan tersimpan di data, bukan di kode:

```
naskah babak1_balai_daha  --then-->  naskah babak1_tapak_wangi  --then-->  pertarungan babak1_hutan
                                                                                   |
                                                                            menang |
                                                                                   v
                                                                         naskah babak1_penutup
```

- `assets/cerita/<nama>.json`: naskah. `"then"` boleh menunjuk ke naskah lain **atau** pertarungan.
- `assets/pertarungan/<id>.json`: pertarungan. Menentukan arena, **musuh**, batas ronde, dan
  naskah setelah menang (`"menang"`). Format lengkap: [format-naskah.md](format-naskah.md#7-file-pertarungan).

Perubahan yang dibutuhkan di front end:

> **Status 2026-11-25: langkah 1-5 sudah dikerjakan** (Rowill26, branch `frontend/alur-babak1`):
> `ArenaPrototype.startNewGame()`, `startBattle(id)`, `afterBattle()`, `loadArena(key, mode)` untuk
> lokasi cerita, `StoryLayer.end()`, dan `BattleSession.create(..., RencanaPertarungan, ...)`.
> Naskah tanpa `"then"` kembali ke layar judul. Opsi uji baru: `--pertarungan <id>` (langsung ke
> satu pertarungan) dan `--alur-otomatis` (naskah dibaca otomatis, opsi pertama dipilih), mis.
> `--demo --title --start-at 30 --alur-otomatis --cepat` memainkan seluruh alur tanpa tangan.

**1. Tombol "Mulai" di layar judul** memutar `AlurCerita.NASKAH_AWAL` (`"babak1_balai_daha"`),
bukan `contoh_babak1`.

**2. Akhir naskah (`StoryLayer.end()`)**: urutan pemeriksaan `"then"` menjadi naskah → pertarungan → arena.

```java
String then = player.getLanjutan();
if (exists(then)) {
    start(then);                                                   // naskah berikutnya
} else if (Gdx.files.internal("pertarungan/" + then + ".json").exists()) {
    game.mulaiPertarungan(then);                                   // lihat langkah 3
} else {
    game.loadArena(then, null);                                    // perilaku lama
}
```

**3. Memulai pertarungan dari rencana**: musuh diambil dari file pertarungan, bukan dari `arena.json`.

```java
RencanaPertarungan rencana;
try {
    rencana = PembacaPertarungan.baca(id,
            Gdx.files.internal("pertarungan/" + id + ".json").readString("UTF-8"));
} catch (PertarunganTidakValidException e) {
    Gdx.app.error("Pertarungan", e.getMessage());
    return;
}
// muat arena rencana.getArena() dengan waktu rencana.getMode() (boleh null = bawaan arena)
BattleManager battle = rencana.buatBattle(tas);   // kelompok + musuh baru, nyawa penuh, tas yang sama
// lanjutkan seperti bagian 3-8: listener, PengendaliMusuh, battle.mulai()
```

`arena.json` tetap menentukan **posisi**. Pasangkan musuh dari `battle.getLawan()` ke posisi tokoh
`arena.json` dengan sprite yang sama (`k.getIdSprite()`), berurutan; posisi musuh di `arena.json`
yang tidak dipakai rencana **tidak ditampilkan**. Babak 1 memakai arena `hutan` dengan dua Leak dan
satu Pengikut, tanpa Pemimpin Pengikut.

**4. Lokasi cerita tidak memulai pertarungan.** Naskah boleh memakai arena yang punya musuh di
`arena.json` (mis. `babak1_penutup` di `hutan`). Saat arena dimuat untuk naskah, tampilkan latarnya
saja; pertarungan hanya dimulai dari langkah 3.

**5. Setelah pertarungan**:

| Hasil | Lanjutan |
|---|---|
| `MENANG` | jika `rencana.getLanjutanMenang() != null`, `story.start(rencana.getLanjutanMenang())`; jika `null`, kembali ke layar judul |
| `KALAH` | ulangi: `rencana.buatBattle(tas)` lagi (semua tokoh baru, nyawa penuh) |
| `WAKTU_HABIS` | belum dipakai di Babak 1; aturannya menyusul bersama Babak 5 |

`./gradlew test` memeriksa seluruh jaringan ini: setiap `"then"` dan `"menang"` menunjuk ke file
yang ada, arena dan waktunya ada, ekspresi potret ada, dan semua naskah bisa dicapai dari
`NASKAH_AWAL`. Babak 1 juga dimainkan otomatis dari awal sampai akhir.

## 13. Yang belum ada di back end

Sedang dikerjakan back end (lihat catatan revisi D). Bagian di atas **tidak** perlu menunggu ini.

| Fitur | Catatan |
|---|---|
| Naskah Babak 5 dan epilog | Selesai (format-naskah.md bagian 8). |
| Babak 5 (D3, D4, D7) | Selesai: `SumberRitual` (sprite `sumber_ritual`, gambar sementara), tiga pertarungan berurutan, wujud ritual memakai `nextForm` di front end. `afterBattle()` memanggil `catatHasilRitual` untuk rencana `isRitual()` dan memutar `getLanjutanWaktuHabis()` saat WAKTU_HABIS. |
| Ratna dan Bharadah di arena (D2) | Versi sederhana, dikerjakan terakhir. |

Pertanyaan tentang back end: hubungi pemilik back end (Rowill26).
