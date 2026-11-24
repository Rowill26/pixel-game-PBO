package id.girah.cerita;

import java.util.List;
import java.util.Objects;

/**
 * Satu naskah adegan, dibaca dari {@code assets/cerita/<nama>.json} oleh {@link PembacaNaskah}.
 * Immutable; jalannya naskah diatur oleh {@link PemutarNaskah}.
 */
public final class Naskah {

    private final String nama;
    private final String judul;
    private final String misi;
    private final String arena;
    private final String mode;
    private final List<LangkahNaskah> langkah;
    private final String lanjutan;

    /**
     * @param arena    lokasi latar ({@code "arena"}), boleh {@code null}
     * @param mode     waktu latar ({@code "mode"}, mis. "night"), boleh {@code null}
     * @param lanjutan yang dimuat setelah naskah selesai ({@code "then"}), boleh {@code null}
     */
    public Naskah(String nama, String judul, String misi, String arena, String mode,
                  List<LangkahNaskah> langkah, String lanjutan) {
        this.nama = Objects.requireNonNull(nama, "Nama naskah tidak boleh null");
        this.judul = Objects.requireNonNull(judul, "Judul tidak boleh null");
        this.misi = Objects.requireNonNull(misi, "Misi tidak boleh null");
        this.arena = arena;
        this.mode = mode;
        this.langkah = List.copyOf(langkah);
        this.lanjutan = lanjutan;
    }

    /** Nama file tanpa {@code .json}, mis. "contoh_babak2". */
    public String getNama() {
        return nama;
    }

    /** {@code title}, mis. "BABAK 2 - LUKA RATNA MANGGALI". */
    public String getJudul() {
        return judul;
    }

    /** {@code mission}, mis. "Misi: cari asal wabah". */
    public String getMisi() {
        return misi;
    }

    /** {@code arena}, atau {@code null}. */
    public String getArena() {
        return arena;
    }

    /** {@code mode}, atau {@code null}. */
    public String getMode() {
        return mode;
    }

    public List<LangkahNaskah> getLangkah() {
        return langkah;
    }

    /** {@code then}, atau {@code null}. */
    public String getLanjutan() {
        return lanjutan;
    }

    @Override
    public String toString() {
        return "Naskah[" + nama + ", " + langkah.size() + " langkah]";
    }
}
