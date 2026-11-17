package id.girah.ai;

import id.girah.karakter.KarakterMitos;
import java.util.Objects;

/** Hasil pikiran AI: aksi apa yang dipilih dan ke siapa. Immutable, jadi aman dioper antar-thread. */
public final class KeputusanAksi {

    public enum Jenis { SERANG, JURUS }

    private final Jenis jenis;
    private final KarakterMitos target;

    private KeputusanAksi(Jenis jenis, KarakterMitos target) {
        this.jenis = jenis;
        this.target = target;
    }

    public static KeputusanAksi serang(KarakterMitos target) {
        return new KeputusanAksi(Jenis.SERANG, Objects.requireNonNull(target, "Target serangan tidak boleh null"));
    }

    /** @param target boleh {@code null} untuk jurus yang mengenai semua lawan */
    public static KeputusanAksi jurus(KarakterMitos target) {
        return new KeputusanAksi(Jenis.JURUS, target);
    }

    public Jenis getJenis() {
        return jenis;
    }

    /** Sasaran aksi; {@code null} untuk jurus area. */
    public KarakterMitos getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return jenis + (target == null ? "" : " -> " + target.getNama());
    }
}
