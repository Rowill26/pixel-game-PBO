# Art direction brief — uji coba skill create-game-assets

> Arah visual default yang dipilih sementara karena belum ada keputusan kelompok.
> Ganti bagian mana pun (misalnya genre, ukuran, atau palet) lalu buat ulang asetnya.

## Game frame

- Player fantasy: petualang muda yang menjelajahi dunia RPG top-down
- Core verbs: berjalan, berbicara dengan NPC, mengambil item
- Engine and renderer: Java 17, Swing/AWT (`BufferedImage` + `Graphics2D`), tanpa engine
- Target platforms: desktop Windows/Linux/macOS (proyek mata kuliah OOP)
- Camera/view/facing: top-down 3/4, karakter menghadap ke bawah (ke arah kamera)
- Native viewport and common display scale: 320×180 px, ditampilkan 4× (1280×720)
- Typical asset size on screen: karakter 32×32 px native, jadi 128×128 px di layar

## Visual system

- Shape language: bentuk kotak membulat, kepala besar (proporsi chibi ±1:1,4)
- Silhouette priorities: rambut + kepala besar, tunik biru, sepatu bot gelap
- Value structure: wajah paling terang, tunik nilai menengah, outline paling gelap
- Palette roles and exact swatches (Sweetie 16, palet publik dari GrafxKid):
  - outline `#1a1c2c` · kulit `#f4b98a` / bayangan kulit `#c97c5d`
  - rambut `#8a5a2b` (terang) / `#5d3a1a` (gelap)
  - tunik `#3b5dc9` / terang `#41a6f6` / bayangan `#29366f`
  - sabuk `#6e4a2e` · gesper `#ffcd75`
  - celana `#566c86` / bayangan `#333c57` · bot `#3e2731`
- Materials and surface cues: kain polos, tanpa tekstur
- Edge/line treatment: outline gelap 1 px di sekeliling, tanpa anti-aliasing
- Lighting direction and contrast: cahaya dari kiri atas; sisi kanan diberi bayangan
- Detail density and focal hierarchy: wajah → gesper → bot
- Motion character: jalan ringan, badan turun 1 px saat langkah
- Explicit exclusions: tanpa teks, tanpa piksel semi-transparan, tanpa gradasi

## Technical contract

- Asset dimensions/aspect: frame 32×32; strip jalan 128×32 (4 frame horizontal)
- Alpha/background: PNG RGBA, latar transparan penuh (alpha 0 atau 255 saja)
- Grid/tile/frame size: 32×32; tile dunia nanti 16×16 (2×2 tile per karakter)
- Anchor/pivot/baseline: bottom-center, baris kaki di y = 29
- Filtering/mipmaps/compression: nearest-neighbor, tanpa mipmap, PNG lossless
- Color space: sRGB
- Texture/poly/material budgets: ≤ 16 warna per sprite
- Naming and folders: `assets/<kelompok>/<nama>-<aksi>.png`, contoh `assets/player/player-walk-down.png`

## Visual target

- Approved seed/reference paths: `assets/player/player-idle.png` (belum disetujui pengguna)
- Required do/don't examples: lihat `output/player-preview.png`
- Native-scale gameplay capture: `output/java-snapshot.png`
- Approval owner/date: pengguna — belum disetujui
