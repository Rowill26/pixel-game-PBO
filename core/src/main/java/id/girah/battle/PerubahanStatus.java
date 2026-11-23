package id.girah.battle;

import id.girah.karakter.KarakterMitos;
import java.util.Objects;

/**
 * Perubahan nyawa dan energi batin satu karakter akibat satu aksi.
 * Front end memakainya untuk animasi terluka/gugur dan angka kerusakan/pemulihan.
 * Immutable.
 */
public final class PerubahanStatus {

    private final KarakterMitos karakter;
    private final int perubahanNyawa;
    private final int perubahanEnergiBatin;
    private final boolean gugur;

    PerubahanStatus(KarakterMitos karakter, int perubahanNyawa, int perubahanEnergiBatin, boolean gugur) {
        this.karakter = Objects.requireNonNull(karakter, "Karakter tidak boleh null");
        this.perubahanNyawa = perubahanNyawa;
        this.perubahanEnergiBatin = perubahanEnergiBatin;
        this.gugur = gugur;
    }

    public KarakterMitos getKarakter() {
        return karakter;
    }

    /** Selisih nyawa: negatif = terluka, positif = disembuhkan. */
    public int getPerubahanNyawa() {
        return perubahanNyawa;
    }

    /** Selisih energi batin: negatif = terpakai untuk jurus, positif = dipulihkan. */
    public int getPerubahanEnergiBatin() {
        return perubahanEnergiBatin;
    }

    /** Kerusakan yang diterima (0 jika tidak terluka). */
    public int getKerusakan() {
        return Math.max(0, -perubahanNyawa);
    }

    /** Nyawa yang pulih (0 jika tidak disembuhkan). */
    public int getPemulihanNyawa() {
        return Math.max(0, perubahanNyawa);
    }

    /** {@code true} jika karakter gugur karena aksi ini (sebelumnya masih hidup). */
    public boolean isGugur() {
        return gugur;
    }

    @Override
    public String toString() {
        return karakter.getNama() + " nyawa " + tanda(perubahanNyawa) + ", energi " + tanda(perubahanEnergiBatin)
                + (gugur ? ", gugur" : "");
    }

    private static String tanda(int n) {
        return n > 0 ? "+" + n : String.valueOf(n);
    }
}
