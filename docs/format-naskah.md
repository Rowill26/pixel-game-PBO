# Format Naskah Cerita

> Untuk: penulis naskah lima babak.
> Naskah disimpan di `assets/cerita/<nama>.json`. Formatnya sama dengan contoh di prototipe
> (`contoh_babak1.json`, `contoh_babak2.json`), ditambah dua bagian opsional: **`efek`** dan **`syarat`**.

## Memeriksa naskah

Setelah menulis atau mengubah naskah, jalankan dari folder root repo:

```bash
./gradlew test
```

Semua file di `assets/cerita/` diperiksa otomatis. Jika ada yang salah, pesannya menyebut nama
file dan letaknya, misalnya:

```
Naskah 'babak4' di lines[6].efek[0].tandai[0]: penanda tidak dikenal: "bahula_jujr".
Yang dikenal: [penawar_girah_dulu, penawar_tapak_wangi_dulu, ...]
```

`lines[6]` = baris ketujuh (hitungan mulai dari 0), `efek[0]` = efek opsi pertama.

## 1. Bentuk dasar

```json
{
  "title": "BABAK 2 - LUKA RATNA MANGGALI",
  "mission": "Misi: cari asal wabah",
  "arena": "tapak_wangi",
  "lines": [
    {"who": "narator", "text": "Pagi di Desa Tapak Wangi."},
    {"who": "ratna", "name": "Ratna Manggali", "face": "sedih", "text": "Mereka takut kepada Ibu."}
  ],
  "then": "hutan"
}
```

| Kunci | Wajib | Isi |
|---|---|---|
| `title` | tidak | judul babak di layar cerita |
| `mission` | tidak | teks misi |
| `arena` | tidak | latar (folder di `assets/arena/`) |
| `mode` | tidak | waktu latar, mis. `"night"` |
| `lines` | **ya** | daftar baris dan pilihan |
| `then` | tidak | yang dimuat setelah naskah selesai (arena pertarungan atau naskah berikutnya) |

**Baris dialog:** `text` wajib. `who` = kunci potret (bawaan `"narator"`, tanpa potret),
`name` = papan nama, `face` = ekspresi potret (bawaan `"netral"`).

**Pilihan:**

```json
{"choice":  ["Mendahulukan Girah", "Mendahulukan Tapak Wangi"],
 "replies": [[ ...baris jika opsi 1... ], [ ...baris jika opsi 2... ]]}
```

Balasan boleh berisi pilihan lagi.

## 2. Efek pilihan

`"efek"` adalah daftar yang **sejajar** dengan `"choice"`: satu objek per opsi, atau `null` untuk
opsi tanpa efek.

```json
{"choice": ["Mendahulukan Girah", "Mendahulukan Tapak Wangi"],
 "replies": [[...], [...]],
 "efek": [
   {"kepercayaanRatna": 1, "kepercayaanGirah": 1, "tandai": ["penawar_girah_dulu"]},
   {"kepercayaanRatna": -1, "kepercayaanTapakWangi": 1, "tandai": ["penawar_tapak_wangi_dulu"]}
 ]}
```

**Nilai** (angka bulat, boleh negatif, ditambahkan ke nilai sekarang; semua mulai dari 0):

| Kunci | Arti |
|---|---|
| `kepercayaanRatna` | kepercayaan Ratna kepada kelompok (**syarat Ending 2**) |
| `kepercayaanGirah` | kepercayaan warga Girah |
| `kepercayaanTapakWangi` | kepercayaan warga Tapak Wangi |
| `wargaSelamat` | warga yang selamat saat evakuasi Babak 4 (mengubah epilog) |

**Penanda** (di `"tandai"`, keputusan yang dicatat sekali dan tidak bisa dihapus):

| Penanda | Kapan dipakai |
|---|---|
| `penawar_girah_dulu` | Babak 2: penawar pertama ke Girah |
| `penawar_tapak_wangi_dulu` | Babak 2: penawar pertama ke Tapak Wangi (**Janda Wening selamat**) |
| `dukung_bahula_jujur` | Babak 3: mendukung Bahula berkata jujur |
| `bahula_jujur` | Babak 4, pilihan A |
| `ratna_membantu_kitab` | Babak 4, pilihan A (**syarat Ending 2**) |
| `kitab_diambil_diam_diam` | Babak 4, pilihan B (**menutup Ending 2**) |
| `serahkan_ke_kerajaan` | Babak 5, pilihan akhir: menyerahkan Calon Arang |
| `pilih_jalan_pemulihan` | Babak 5, pilihan akhir: jalan pemulihan |

Penanda baru harus ditambahkan dulu di back end (`PenandaCerita.java`); penanda yang tidak
dikenal ditolak saat pemeriksaan.

## 3. Syarat

Baris dialog atau opsi pilihan bisa diberi syarat: hanya tampil jika **semua** bagiannya terpenuhi.

```json
{"who": "ratna", "name": "Ratna Manggali", "face": "bahagia",
 "text": "Aku percaya kalian.", "syarat": {"kepercayaanRatna": 3}}
```

Untuk pilihan, `"syarat"` juga daftar yang sejajar dengan `"choice"` (`null` = selalu tampil):

```json
{"choice": ["Serahkan kepada kerajaan", "Tempuh jalan pemulihan"],
 "syarat": [null, {"jalanPemulihanTerbuka": true}]}
```

| Kunci syarat | Terpenuhi jika |
|---|---|
| `"kepercayaanRatna": 3` (kunci nilai mana pun) | nilai **≥** angka itu |
| `"kurangDari": {"kepercayaanRatna": 3}` | nilai **<** angka itu |
| `"penanda": ["bahula_jujur"]` | semua penanda itu sudah dicatat |
| `"tanpaPenanda": ["kitab_diambil_diam_diam"]` | tidak ada satu pun penanda itu |
| `"jalanPemulihanTerbuka": true` / `false` | syarat Ending 2 terpenuhi / tidak |
| `"weningSelamat": true` / `false` | Janda Wening selamat / tidak |
| `"ending": "JALAN_PEMULIHAN"` | ending itu yang tercapai (hanya setelah pertarungan ritual) |

Nilai `ending`: `PENGADILAN_KERAJAAN`, `JALAN_PEMULIHAN`, `AKHIR_TRAGIS`.
Baris yang syaratnya tidak terpenuhi dilewati; pilihan yang semua opsinya tersembunyi juga dilewati.

## 4. Aturan ending (diatur back end)

| Ending | Syarat |
|---|---|
| **3. Akhir Tragis** | sumber ritual tidak hancur sebelum batas ronde fase terakhir Babak 5 |
| **2. Jalan Pemulihan** | ritual dihentikan tepat waktu **dan** `kepercayaanRatna` ≥ 3 **dan** `ratna_membantu_kitab` **dan** tanpa `kitab_diambil_diam_diam` **dan** pemain memilih `pilih_jalan_pemulihan` |
| **1. Pengadilan Kerajaan** | ritual dihentikan tepat waktu, selain kasus di atas |

Janda Wening selamat jika `penawar_tapak_wangi_dulu`.

## 5. Efek yang disarankan per babak

Supaya ketiga ending bisa dicapai, efek pilihan sebaiknya mengikuti tabel ini (dari dokumen cerita):

| Babak | Pilihan | Efek |
|---|---|---|
| 2 | Mendahulukan Girah | `{"kepercayaanRatna": 1, "kepercayaanGirah": 1, "tandai": ["penawar_girah_dulu"]}` |
| 2 | Mendahulukan Tapak Wangi | `{"kepercayaanRatna": -1, "kepercayaanTapakWangi": 1, "tandai": ["penawar_tapak_wangi_dulu"]}` |
| 3 | Mendukung Bahula jujur | `{"kepercayaanRatna": 1, "tandai": ["dukung_bahula_jujur"]}` |
| 3 | Tidak mendukung | `null` |
| 4 | A: Bahula berkata jujur | `{"kepercayaanRatna": 2, "tandai": ["bahula_jujur", "ratna_membantu_kitab"]}` |
| 4 | B: Mengambil kitab diam-diam | `{"kepercayaanRatna": -2, "tandai": ["kitab_diambil_diam_diam"]}` |
| 4 | Titik evakuasi | `{"wargaSelamat": <jumlah>}` sesuai pilihan |
| 5 | 1: Mengakui luka Calon Arang | `{"kepercayaanRatna": 1}` |
| 5 | 2: Mendesak menyerah | `null` |
| 5 | 3: Menghancurkan altar | `null` |
| 5 | Akhir: serahkan / pemulihan | `serahkan_ke_kerajaan` / `pilih_jalan_pemulihan`, opsi pemulihan diberi `"syarat": {"jalanPemulihanTerbuka": true}` |

Dengan angka ini, Jalan Pemulihan membutuhkan pilihan A di Babak 4 (+2) ditambah minimal satu
pilihan lain yang mendukung Ratna (Girah dulu, mendukung Bahula, atau mengakui luka Calon Arang).

## 6. Contoh epilog yang berubah sesuai ending

```json
{
  "title": "EPILOG",
  "arena": "tapak_wangi",
  "lines": [
    {"who": "narator", "text": "Calon Arang dibawa ke Daha untuk diadili.",
     "syarat": {"ending": "PENGADILAN_KERAJAAN"}},
    {"who": "narator", "text": "Calon Arang hidup dalam pengawasan Mpu Bharadah.",
     "syarat": {"ending": "JALAN_PEMULIHAN"}},
    {"who": "narator", "text": "Gelombang terakhir wabah menyebar ke Daha.",
     "syarat": {"ending": "AKHIR_TRAGIS"}},
    {"who": "wening", "name": "Janda Wening", "face": "netral",
     "text": "Desa ini tidak akan kembali seperti dahulu.",
     "syarat": {"weningSelamat": true}}
  ]
}
```
