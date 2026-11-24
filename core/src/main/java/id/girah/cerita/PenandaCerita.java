package id.girah.cerita;

/**
 * Keputusan penting yang tercatat sepanjang cerita. Kunci JSON-nya dipakai di naskah,
 * mis. {@code "efek": [{"tandai": ["bahula_jujur"]}]}.
 *
 * <p>Daftarnya sengaja tetap (enum): penanda yang salah ketik di naskah langsung ditolak
 * saat naskah dibaca, bukan diam-diam membuat ending tidak bisa dicapai.
 */
public enum PenandaCerita {
    /** Babak 2: penawar pertama dikirim ke Girah. */
    PENAWAR_GIRAH_DULU("penawar_girah_dulu"),
    /** Babak 2: penawar pertama dikirim ke Tapak Wangi; Janda Wening dirawat lebih cepat. */
    PENAWAR_TAPAK_WANGI_DULU("penawar_tapak_wangi_dulu"),
    /** Babak 3: kelompok mendukung Bahula untuk berkata jujur. */
    DUKUNG_BAHULA_JUJUR("dukung_bahula_jujur"),
    /** Babak 4, pilihan A: Bahula mengakui siasatnya kepada Ratna. */
    BAHULA_JUJUR("bahula_jujur"),
    /** Babak 4, pilihan A: Ratna menunjukkan tempat kitab. Syarat Ending 2. */
    RATNA_MEMBANTU_KITAB("ratna_membantu_kitab"),
    /** Babak 4, pilihan B: kitab diambil tanpa sepengetahuan Ratna. Menutup Ending 2. */
    KITAB_DIAMBIL_DIAM_DIAM("kitab_diambil_diam_diam"),
    /** Babak 5, setelah ritual dihentikan: memilih menyerahkan Calon Arang kepada kerajaan. */
    SERAHKAN_KE_KERAJAAN("serahkan_ke_kerajaan"),
    /** Babak 5, setelah ritual dihentikan: memilih jalan pemulihan (hanya jika terbuka). */
    PILIH_JALAN_PEMULIHAN("pilih_jalan_pemulihan");

    private final String kunciJson;

    PenandaCerita(String kunciJson) {
        this.kunciJson = kunciJson;
    }

    public String getKunciJson() {
        return kunciJson;
    }

    /** Penanda dengan kunci JSON {@code kunci}, atau {@code null} jika tidak dikenal. */
    public static PenandaCerita dariKunci(String kunci) {
        for (PenandaCerita p : values()) {
            if (p.kunciJson.equals(kunci)) {
                return p;
            }
        }
        return null;
    }
}
