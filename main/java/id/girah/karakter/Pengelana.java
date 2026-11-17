package id.girah.karakter;

/** Anggota kelompok pengelana yang dikendalikan pemain. */
public abstract class Pengelana extends KarakterMitos {

    protected Pengelana(String nama, int nyawaMaks, int energiBatinMaks,
                        int kekuatan, int pertahanan, int kecepatan) {
        super(nama, nyawaMaks, energiBatinMaks, kekuatan, pertahanan, kecepatan);
    }

    @Override
    public final Faksi getFaksi() {
        return Faksi.PENGELANA;
    }
}
