package id.girah.cerita;

/**
 * Nilai angka yang berubah karena pilihan pemain. Kunci JSON-nya dipakai di naskah,
 * mis. {@code "efek": [{"kepercayaanRatna": 1}]}.
 */
public enum NilaiCerita {
    /** Kepercayaan Ratna Manggali kepada kelompok. Syarat Ending 2 (Jalan Pemulihan). */
    KEPERCAYAAN_RATNA("kepercayaanRatna"),
    /** Kepercayaan warga Desa Girah. */
    KEPERCAYAAN_GIRAH("kepercayaanGirah"),
    /** Kepercayaan warga Desa Tapak Wangi. */
    KEPERCAYAAN_TAPAK_WANGI("kepercayaanTapakWangi"),
    /** Warga yang berhasil diselamatkan saat evakuasi Babak 4. Mengubah epilog. */
    WARGA_SELAMAT("wargaSelamat");

    private final String kunciJson;

    NilaiCerita(String kunciJson) {
        this.kunciJson = kunciJson;
    }

    public String getKunciJson() {
        return kunciJson;
    }

    /** Nilai dengan kunci JSON {@code kunci}, atau {@code null} jika tidak dikenal. */
    public static NilaiCerita dariKunci(String kunci) {
        for (NilaiCerita n : values()) {
            if (n.kunciJson.equals(kunci)) {
                return n;
            }
        }
        return null;
    }
}
