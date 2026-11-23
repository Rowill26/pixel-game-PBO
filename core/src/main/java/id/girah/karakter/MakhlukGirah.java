package id.girah.karakter;

/**
 * Pengikut, makhluk gaib, dan Calon Arang sendiri: dikendalikan AI.
 *
 * <p>Makhluk Girah bisa berucap sekali saja ketika nyawanya jatuh ke
 * {@value #AMBANG_NYAWA_RENDAH_PERSEN}% atau kurang: subclass cukup meng-override
 * {@link #ucapanNyawaRendah()}.
 */
public abstract class MakhlukGirah extends KarakterMitos {

    public static final int AMBANG_NYAWA_RENDAH_PERSEN = 30;

    private boolean sudahBerucapNyawaRendah;

    protected MakhlukGirah(String nama, int nyawaMaks, int energiBatinMaks,
                           int kekuatan, int pertahanan, int kecepatan) {
        super(nama, nyawaMaks, energiBatinMaks, kekuatan, pertahanan, kecepatan);
    }

    @Override
    public final Faksi getFaksi() {
        return Faksi.GIRAH;
    }

    /**
     * Musuh utama sebuah babak. AI memberi bos jeda berpikir lebih lama agar ucapannya
     * sempat dibaca. Bawaan: {@code false}.
     */
    public boolean isBos() {
        return false;
    }

    /** {@code true} jika nyawa sudah di bawah atau sama dengan ambang nyawa rendah. */
    public boolean isNyawaRendah() {
        return getNyawa() * 100 <= getNyawaMaks() * AMBANG_NYAWA_RENDAH_PERSEN;
    }

    /**
     * Kalimat yang diucapkan sekali saat nyawa pertama kali jatuh ke ambang rendah.
     * Bawaan: kosong (tidak berucap).
     */
    protected String ucapanNyawaRendah() {
        return "";
    }

    @Override
    protected String reaksiSetelahTerluka() {
        if (sudahBerucapNyawaRendah || !isNyawaRendah()) {
            return "";
        }
        sudahBerucapNyawaRendah = true;
        return ucapanNyawaRendah();
    }
}
