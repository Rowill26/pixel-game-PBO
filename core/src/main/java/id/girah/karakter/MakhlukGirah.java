package id.girah.karakter;

/** Pengikut, makhluk gaib, dan Calon Arang sendiri: dikendalikan AI. */
public abstract class MakhlukGirah extends KarakterMitos {

    protected MakhlukGirah(String nama, int nyawaMaks, int energiBatinMaks,
                           int kekuatan, int pertahanan, int kecepatan) {
        super(nama, nyawaMaks, energiBatinMaks, kekuatan, pertahanan, kecepatan);
    }

    @Override
    public final Faksi getFaksi() {
        return Faksi.GIRAH;
    }
}
