package id.girah.cerita;

/** Tiga akhir cerita dari naskah. Aturan mencapainya ada di {@link PenentuEnding}. */
public enum Ending {
    PENGADILAN_KERAJAAN("Ending 1 - Pengadilan Kerajaan"),
    JALAN_PEMULIHAN("Ending 2 - Jalan Pemulihan"),
    AKHIR_TRAGIS("Ending 3 - Akhir Tragis");

    private final String judul;

    Ending(String judul) {
        this.judul = judul;
    }

    public String getJudul() {
        return judul;
    }

    /** Ending dengan nama konstanta {@code nama} (mis. "AKHIR_TRAGIS"), atau {@code null}. */
    public static Ending dariNama(String nama) {
        for (Ending e : values()) {
            if (e.name().equals(nama)) {
                return e;
            }
        }
        return null;
    }
}
