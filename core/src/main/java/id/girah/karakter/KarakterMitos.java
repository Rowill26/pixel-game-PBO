package id.girah.karakter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Dasar semua karakter dalam Bayang-Bayang Girah.
 *
 * <p>Setiap aksi ({@link #serang}, {@link #gunakanJurus}) mengembalikan {@code String}
 * narasi yang siap ditampilkan di kotak dialog dan dicatat di Babad Pertarungan.
 *
 * <p>Kelas ini tidak bergantung pada LibGDX supaya bisa diuji dengan JUnit biasa.
 */
public abstract class KarakterMitos {

    private final String nama;
    private final int nyawaMaks;
    private final int energiBatinMaks;
    private final int kekuatan;
    private final int pertahanan;
    private final int kecepatan;

    private int nyawa;
    private int energiBatin;

    protected KarakterMitos(String nama, int nyawaMaks, int energiBatinMaks,
                            int kekuatan, int pertahanan, int kecepatan) {
        if (nama == null || nama.isBlank()) {
            throw new IllegalArgumentException("Nama karakter tidak boleh kosong");
        }
        if (nyawaMaks <= 0) {
            throw new IllegalArgumentException("Nyawa maksimum harus > 0");
        }
        if (energiBatinMaks < 0 || kekuatan < 0 || pertahanan < 0 || kecepatan < 0) {
            throw new IllegalArgumentException("Statistik " + nama + " tidak boleh negatif");
        }
        this.nama = nama;
        this.nyawaMaks = nyawaMaks;
        this.energiBatinMaks = energiBatinMaks;
        this.kekuatan = kekuatan;
        this.pertahanan = pertahanan;
        this.kecepatan = kecepatan;
        this.nyawa = nyawaMaks;
        this.energiBatin = energiBatinMaks;
    }

    // ================================================================
    // Aksi: wajib di-override setiap subclass (polymorphism)
    // ================================================================

    /** Serangan dasar tanpa biaya energi batin. */
    public abstract String serang(KarakterMitos target);

    /**
     * Jurus khusus yang memakai energi batin.
     *
     * @param sasaran daftar sasaran sesuai {@link #getSasaranJurus()}
     * @return narasi hasil jurus
     * @throws EnergiBatinHabisException jika energi batin tidak cukup; tidak ada yang berubah
     */
    public abstract String gunakanJurus(List<KarakterMitos> sasaran) throws EnergiBatinHabisException;

    public abstract String getNamaJurus();

    public abstract int getBiayaJurus();

    public abstract JenisSasaran getSasaranJurus();

    /** Peran dalam kelompok, mis. "Senopati" atau "Makhluk Gaib". */
    public abstract String getPeran();

    public abstract Faksi getFaksi();

    /**
     * Kunci aset untuk front end, sama dengan nama file sprite di {@code assets/sprites/},
     * mis. {@code "pendekar"} untuk {@code pendekar.png}. Dua karakter sejenis memakai id yang sama.
     */
    public abstract String getIdSprite();

    // ================================================================
    // Hook: boleh di-override, perilaku bawaannya sudah masuk akal
    // ================================================================

    /** Cara serangan dasar dilakukan di arena. Bawaan: {@link JarakAksi#DEKAT}. */
    public JarakAksi getJarakSerang() {
        return JarakAksi.DEKAT;
    }

    /** Cara jurus dilakukan di arena. Bawaan: {@link JarakAksi#JAUH}. */
    public JarakAksi getJarakJurus() {
        return JarakAksi.JAUH;
    }

    /**
     * Apakah {@code calon} boleh dipilih sebagai sasaran jurus karakter ini.
     * Bawaan: semua yang masih hidup. Penyembuh membatasinya ke kawan yang terluka,
     * supaya pemain tidak bisa membuang giliran tanpa efek.
     */
    public boolean bisaMenjadiSasaranJurus(KarakterMitos calon) {
        return calon.isHidup();
    }

    /** Rumus kerusakan yang diterima. Bawaan: kekuatan dikurangi pertahanan. */
    protected int hitungKerusakanMasuk(int kekuatanSerangan) {
        return kekuatanSerangan - pertahanan;
    }

    /** Kalimat tambahan setelah terluka tapi masih hidup, mis. ejekan Calon Arang. */
    protected String reaksiSetelahTerluka() {
        return "";
    }

    /** Kalimat saat gugur. */
    protected String narasiGugur() {
        return nama + " gugur.";
    }

    /** Dipanggil pengatur pertarungan setiap ronde baru dimulai. */
    public void awalRonde() {
        // bawaan: tidak ada yang perlu disetel ulang
    }

    /**
     * Ditanya pengatur pertarungan setelah karakter selesai beraksi.
     * Mengembalikan {@code true} sekali saja jika karakter berhak atas giliran tambahan.
     */
    public boolean ambilGiliranTambahan() {
        return false;
    }

    /** Apakah jurus bisa dipakai sekarang. UI memakai ini untuk mengaktifkan tombol jurus. */
    public boolean bisaMemakaiJurus() {
        return isHidup() && energiBatin >= getBiayaJurus();
    }

    // ================================================================
    // Proses kerusakan dan pemulihan
    // ================================================================

    /**
     * Satu-satunya jalan untuk mengurangi nyawa. Kerusakan minimal 1 jika
     * serangan > 0, dan nyawa tidak turun di bawah 0.
     *
     * @return kerusakan yang benar-benar mengurangi nyawa
     */
    public final int terimaSerangan(int kekuatanSerangan) {
        if (kekuatanSerangan < 0) {
            throw new IllegalArgumentException("Kekuatan serangan tidak boleh negatif");
        }
        if (kekuatanSerangan == 0 || !isHidup()) {
            return 0;
        }
        int kerusakan = Math.max(1, hitungKerusakanMasuk(kekuatanSerangan));
        int sebenarnya = Math.min(kerusakan, nyawa);
        nyawa -= sebenarnya;
        return sebenarnya;
    }

    /** Menyembuhkan nyawa, dibatasi maksimum. Karakter yang gugur tidak bisa disembuhkan. */
    public int sembuhkan(int jumlah) {
        if (jumlah < 0) {
            throw new IllegalArgumentException("Jumlah penyembuhan tidak boleh negatif");
        }
        if (!isHidup()) {
            return 0;
        }
        int pulih = Math.min(jumlah, nyawaMaks - nyawa);
        nyawa += pulih;
        return pulih;
    }

    /** Memulihkan energi batin, dibatasi maksimum. Karakter yang gugur tidak bisa dipulihkan. */
    public int pulihkanEnergiBatin(int jumlah) {
        if (jumlah < 0) {
            throw new IllegalArgumentException("Jumlah pemulihan tidak boleh negatif");
        }
        if (!isHidup()) {
            return 0;
        }
        int pulih = Math.min(jumlah, energiBatinMaks - energiBatin);
        energiBatin += pulih;
        return pulih;
    }

    /** Mengurangi energi batin. Atomik: jika tidak cukup, energi tidak berubah sama sekali. */
    protected final void pakaiEnergiBatin(int biaya) throws EnergiBatinHabisException {
        if (biaya < 0) {
            throw new IllegalArgumentException("Biaya energi tidak boleh negatif");
        }
        if (biaya > energiBatin) {
            throw new EnergiBatinHabisException(nama, biaya, energiBatin);
        }
        energiBatin -= biaya;
    }

    // ================================================================
    // Bantuan untuk subclass saat menyusun narasi
    // ================================================================

    /** Laporan kerusakan yang seragam untuk semua karakter. */
    protected final String laporanKerusakan(KarakterMitos target, int kerusakan) {
        StringBuilder narasi = new StringBuilder();
        narasi.append(target.getNama()).append(" menerima ").append(kerusakan)
              .append(" kerusakan (nyawa ").append(target.getNyawa())
              .append('/').append(target.getNyawaMaks()).append(").");
        if (target.isHidup()) {
            String reaksi = target.reaksiSetelahTerluka();
            if (!reaksi.isEmpty()) {
                narasi.append('\n').append(reaksi);
            }
        } else {
            narasi.append('\n').append(target.narasiGugur());
        }
        return narasi.toString();
    }

    /** {@code true} jika karakter ini dan targetnya sama-sama masih hidup. */
    protected final boolean bisaMenyerang(KarakterMitos target) {
        Objects.requireNonNull(target, "Target tidak boleh null");
        return isHidup() && target.isHidup();
    }

    /** Narasi ketika aksi batal karena pelaku atau target sudah gugur. */
    protected final String narasiAksiBatal(KarakterMitos target) {
        if (!isHidup()) {
            return nama + " sudah gugur dan tidak bisa bertindak.";
        }
        return target.getNama() + " sudah gugur. " + nama + " menahan serangannya.";
    }

    /** Mengambil satu-satunya sasaran untuk jurus bersasaran tunggal. */
    protected static KarakterMitos sasaranTunggal(List<KarakterMitos> sasaran) {
        Objects.requireNonNull(sasaran, "Daftar sasaran tidak boleh null");
        if (sasaran.size() != 1) {
            throw new IllegalArgumentException("Jurus ini butuh tepat satu sasaran, diberi " + sasaran.size());
        }
        return Objects.requireNonNull(sasaran.get(0), "Sasaran tidak boleh null");
    }

    /** Menyaring sasaran yang masih hidup, untuk jurus area. */
    protected static List<KarakterMitos> sasaranHidup(List<KarakterMitos> sasaran) {
        Objects.requireNonNull(sasaran, "Daftar sasaran tidak boleh null");
        List<KarakterMitos> hidup = new ArrayList<>();
        for (KarakterMitos k : sasaran) {
            if (Objects.requireNonNull(k, "Sasaran tidak boleh null").isHidup()) {
                hidup.add(k);
            }
        }
        return hidup;
    }

    // ================================================================
    // Getter (tidak ada setter publik: nyawa dan energi hanya berubah lewat aturan)
    // ================================================================

    public boolean isHidup() {
        return nyawa > 0;
    }

    public String getNama() {
        return nama;
    }

    public int getNyawa() {
        return nyawa;
    }

    public int getNyawaMaks() {
        return nyawaMaks;
    }

    public int getEnergiBatin() {
        return energiBatin;
    }

    public int getEnergiBatinMaks() {
        return energiBatinMaks;
    }

    public int getKekuatan() {
        return kekuatan;
    }

    public int getPertahanan() {
        return pertahanan;
    }

    public int getKecepatan() {
        return kecepatan;
    }

    @Override
    public String toString() {
        return getPeran() + " " + nama + " [nyawa " + nyawa + "/" + nyawaMaks
                + ", energi batin " + energiBatin + "/" + energiBatinMaks + "]";
    }
}
