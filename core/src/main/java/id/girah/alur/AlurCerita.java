package id.girah.alur;

/**
 * Titik masuk alur cerita.
 *
 * <p>Alurnya sendiri tersimpan di data: setiap naskah menunjuk ke langkah berikutnya lewat
 * {@code "then"} (naskah lain atau pertarungan di {@code assets/pertarungan/}), dan setiap
 * pertarungan menunjuk ke naskah setelah menang lewat {@code "menang"}.
 */
public final class AlurCerita {

    /** Naskah pertama yang diputar saat pemain menekan "Mulai" di layar judul. */
    public static final String NASKAH_AWAL = "babak1_balai_daha";

    private AlurCerita() {
    }
}
