package id.girah.cerita;

import java.util.Objects;

/**
 * Aturan tiga ending dan epilog, diambil dari dokumen cerita (bagian 5 dan 6).
 *
 * <ul>
 *   <li><b>Ending 3, Akhir Tragis</b>: sumber ritual tidak dihancurkan sebelum batas ronde habis.</li>
 *   <li><b>Ending 2, Jalan Pemulihan</b>: ritual dihentikan tepat waktu, jalan pemulihan terbuka
 *       ({@link #jalanPemulihanTerbuka}), dan pemain memilihnya.</li>
 *   <li><b>Ending 1, Pengadilan Kerajaan</b>: ritual dihentikan tepat waktu, selain kasus di atas.</li>
 * </ul>
 */
public final class PenentuEnding {

    /**
     * Kepercayaan Ratna minimal agar jalan pemulihan terbuka. Nilai yang bisa dikumpulkan dari
     * naskah: Babak 2 (Girah dulu +1 / Tapak Wangi dulu -1), Babak 3 (dukung Bahula jujur +1),
     * Babak 4 (Bahula jujur +2 / kitab diambil diam-diam -2), Babak 5 (akui luka Calon Arang +1).
     * Ambang 3 berarti pilihan A di Babak 4 ditambah minimal satu pilihan lain yang mendukung.
     */
    public static final int AMBANG_KEPERCAYAAN_RATNA_TINGGI = 3;

    private PenentuEnding() {
    }

    /**
     * Syarat Ending 2 dari naskah: kepercayaan Ratna tinggi dan Ratna membantu memperoleh kitab
     * (tidak dicuri diam-diam). Tidak bergantung pada hasil ritual, jadi bisa dipakai untuk
     * menampilkan atau menyembunyikan opsi "jalan pemulihan" sebelum dan sesudah pertarungan.
     */
    public static boolean jalanPemulihanTerbuka(StatusCerita status) {
        Objects.requireNonNull(status, "Status tidak boleh null");
        return status.getNilai(NilaiCerita.KEPERCAYAAN_RATNA) >= AMBANG_KEPERCAYAAN_RATNA_TINGGI
                && status.punya(PenandaCerita.RATNA_MEMBANTU_KITAB)
                && !status.punya(PenandaCerita.KITAB_DIAMBIL_DIAM_DIAM);
    }

    /**
     * Janda Wening selamat jika penawar pertama di Babak 2 dikirim ke Tapak Wangi
     * ("Janda Wening lebih cepat menerima perawatan"). Dipakai epilog.
     */
    public static boolean weningSelamat(StatusCerita status) {
        Objects.requireNonNull(status, "Status tidak boleh null");
        return status.punya(PenandaCerita.PENAWAR_TAPAK_WANGI_DULU);
    }

    /**
     * Ending yang dicapai.
     *
     * @throws IllegalStateException jika pertarungan ritual Babak 5 belum selesai
     */
    public static Ending tentukan(StatusCerita status) {
        Objects.requireNonNull(status, "Status tidak boleh null");
        if (!status.isRitualDihentikanTepatWaktu()) {
            return Ending.AKHIR_TRAGIS;
        }
        if (status.punya(PenandaCerita.PILIH_JALAN_PEMULIHAN) && jalanPemulihanTerbuka(status)) {
            return Ending.JALAN_PEMULIHAN;
        }
        return Ending.PENGADILAN_KERAJAAN;
    }
}
