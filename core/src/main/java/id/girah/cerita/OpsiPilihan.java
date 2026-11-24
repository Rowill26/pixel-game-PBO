package id.girah.cerita;

import java.util.List;
import java.util.Objects;

/** Satu opsi dalam sebuah {@link Pilihan}: teks tombol, balasan, efek, dan syarat tampilnya. Immutable. */
public final class OpsiPilihan {

    private final String teks;
    private final List<LangkahNaskah> balasan;
    private final Efek efek;
    private final Syarat syarat;

    public OpsiPilihan(String teks, List<LangkahNaskah> balasan, Efek efek, Syarat syarat) {
        this.teks = Objects.requireNonNull(teks, "Teks opsi tidak boleh null");
        this.balasan = List.copyOf(balasan);
        this.efek = Objects.requireNonNull(efek, "Efek tidak boleh null; pakai Efek.KOSONG");
        this.syarat = Objects.requireNonNull(syarat, "Syarat tidak boleh null; pakai Syarat.SELALU");
    }

    public String getTeks() {
        return teks;
    }

    /** Langkah yang dijalankan setelah opsi ini dipilih (boleh berisi pilihan lagi). */
    public List<LangkahNaskah> getBalasan() {
        return balasan;
    }

    public Efek getEfek() {
        return efek;
    }

    public Syarat getSyarat() {
        return syarat;
    }

    /** {@code true} jika opsi ini ditampilkan untuk {@code status} saat ini. */
    public boolean tersediaUntuk(StatusCerita status) {
        return syarat.terpenuhi(status);
    }

    @Override
    public String toString() {
        return teks;
    }
}
