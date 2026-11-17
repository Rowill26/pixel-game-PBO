package id.girah.pusaka;

import id.girah.karakter.KarakterMitos;
import java.util.Objects;

/**
 * Benda pusaka yang bisa dipakai dari Tas Pusaka.
 *
 * <p>Dipakai sebagai kunci {@code HashMap} di {@link TasPusaka}, jadi {@link #equals} dan
 * {@link #hashCode} di-override: dua objek Tirta Suci dianggap pusaka yang sama.
 * Tanpa override ini, setiap {@code new TirtaSuci()} akan menjadi kunci baru di HashMap.
 */
public abstract class Pusaka {

    private final String nama;
    private final String deskripsi;

    protected Pusaka(String nama, String deskripsi) {
        if (nama == null || nama.isBlank()) {
            throw new IllegalArgumentException("Nama pusaka tidak boleh kosong");
        }
        this.nama = nama;
        this.deskripsi = Objects.requireNonNull(deskripsi, "Deskripsi tidak boleh null");
    }

    /** Apakah pusaka ini berguna bagi {@code target} sekarang (hidup dan tidak penuh). */
    public abstract boolean bisaDipakaiPada(KarakterMitos target);

    /**
     * Memakai pusaka. Tas Pusaka memastikan {@link #bisaDipakaiPada} bernilai {@code true} dulu.
     *
     * @return narasi pemakaian
     */
    public abstract String gunakan(KarakterMitos pengguna, KarakterMitos target);

    public String getNama() {
        return nama;
    }

    public String getDeskripsi() {
        return deskripsi;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return nama.equals(((Pusaka) o).nama);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass().getName(), nama);
    }

    @Override
    public String toString() {
        return nama;
    }
}
