package id.girah.alur;

import id.girah.karakter.Balian;
import id.girah.karakter.CalonArang;
import id.girah.karakter.Leak;
import id.girah.karakter.MakhlukGirah;
import id.girah.karakter.PemimpinPengikut;
import id.girah.karakter.Pengelana;
import id.girah.karakter.Pengikut;
import id.girah.karakter.Senopati;
import id.girah.karakter.TelikSandi;
import id.girah.karakter.Wiku;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Membuat karakter dari data. Jenis musuh memakai kunci yang sama dengan
 * {@link id.girah.karakter.KarakterMitos#getIdSprite()}, mis. {@code "leak"}.
 */
public final class PabrikKarakter {

    /** Jenis musuh yang bisa dipakai di {@code assets/pertarungan/*.json}. */
    public static final Set<String> JENIS_MUSUH = Set.of("leak", "pengikut", "pemimpin_pengikut", "calon_arang");

    /** Bos yang namanya tetap (konstruktornya tanpa nama). */
    public static final Set<String> JENIS_BERNAMA_TETAP = Set.of("pemimpin_pengikut", "calon_arang");

    private PabrikKarakter() {
    }

    /**
     * Kelompok pengelana dengan nyawa dan energi penuh, urut seperti kartu status di HUD.
     * Namanya sama dengan nama peran di naskah, karena naskah tidak memberi nama pribadi.
     */
    public static List<Pengelana> buatKelompok() {
        return List.of(new Senopati("Pendekar"), new Wiku("Juru Mantra"),
                new Balian("Peramu"), new TelikSandi("Pengintai"));
    }

    /**
     * Membuat satu musuh.
     *
     * @param nama nama tampilan; wajib {@code null} untuk {@link #JENIS_BERNAMA_TETAP},
     *             dan boleh {@code null} untuk jenis lain (dipakai nama bawaan)
     * @throws IllegalArgumentException jika jenis tidak dikenal atau nama tidak sesuai aturan di atas
     */
    public static MakhlukGirah buatMusuh(String jenis, String nama) {
        Objects.requireNonNull(jenis, "Jenis musuh tidak boleh null");
        if (JENIS_BERNAMA_TETAP.contains(jenis) && nama != null) {
            throw new IllegalArgumentException("Musuh '" + jenis + "' bernama tetap; jangan beri nama");
        }
        switch (jenis) {
            case "leak":
                return new Leak(nama == null ? "Leak" : nama);
            case "pengikut":
                return new Pengikut(nama == null ? "Pengikut" : nama);
            case "pemimpin_pengikut":
                return new PemimpinPengikut();
            case "calon_arang":
                return new CalonArang();
            default:
                throw new IllegalArgumentException("Jenis musuh tidak dikenal: '" + jenis + "'. Yang dikenal: "
                        + new TreeSet<>(JENIS_MUSUH));
        }
    }
}
