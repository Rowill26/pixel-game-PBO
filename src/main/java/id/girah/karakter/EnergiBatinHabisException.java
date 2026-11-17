package id.girah.karakter;

/**
 * Dilempar ketika karakter mencoba memakai jurus tetapi energi batinnya
 * tidak cukup. Checked exception, supaya pemanggil wajib menanganinya
 * (misalnya dengan menampilkan pesan "Energi batin tidak cukup" di UI).
 */
public class EnergiBatinHabisException extends Exception {

    private final String namaKarakter;
    private final int dibutuhkan;
    private final int tersedia;

    public EnergiBatinHabisException(String namaKarakter, int dibutuhkan, int tersedia) {
        super(namaKarakter + " kehabisan energi batin: butuh " + dibutuhkan
                + ", tersisa " + tersedia);
        this.namaKarakter = namaKarakter;
        this.dibutuhkan = dibutuhkan;
        this.tersedia = tersedia;
    }

    public String getNamaKarakter() {
        return namaKarakter;
    }

    public int getDibutuhkan() {
        return dibutuhkan;
    }

    public int getTersedia() {
        return tersedia;
    }

    /** Selisih energi yang kurang untuk memakai jurus. */
    public int getKekurangan() {
        return dibutuhkan - tersedia;
    }
}
