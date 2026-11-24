package id.girah.cerita;

import java.util.Objects;

/**
 * Satu langkah dalam naskah: sebuah {@link BarisDialog} atau sebuah {@link Pilihan}.
 * Setiap langkah boleh punya {@link Syarat}; langkah yang syaratnya tidak terpenuhi dilewati.
 */
public abstract class LangkahNaskah {

    private final Syarat syarat;

    protected LangkahNaskah(Syarat syarat) {
        this.syarat = Objects.requireNonNull(syarat, "Syarat tidak boleh null; pakai Syarat.SELALU");
    }

    public Syarat getSyarat() {
        return syarat;
    }

    /** {@code true} jika langkah ini dijalankan untuk {@code status} saat ini. */
    public boolean berlakuUntuk(StatusCerita status) {
        return syarat.terpenuhi(status);
    }
}
