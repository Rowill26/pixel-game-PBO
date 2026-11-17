package id.girah.pusaka;

import id.girah.karakter.KarakterMitos;

/** Tirta Suci: air suci yang memulihkan {@value #PEMULIHAN_NYAWA} nyawa. */
public class TirtaSuci extends Pusaka {

    public static final int PEMULIHAN_NYAWA = 50;

    public TirtaSuci() {
        super("Tirta Suci", "Air suci dari mata air pura. Memulihkan " + PEMULIHAN_NYAWA + " nyawa.");
    }

    @Override
    public boolean bisaDipakaiPada(KarakterMitos target) {
        return target.isHidup() && target.getNyawa() < target.getNyawaMaks();
    }

    @Override
    public String gunakan(KarakterMitos pengguna, KarakterMitos target) {
        int pulih = target.sembuhkan(PEMULIHAN_NYAWA);
        StringBuilder narasi = new StringBuilder();
        narasi.append(pengguna.getNama()).append(" memercikkan Tirta Suci kepada ")
              .append(pengguna == target ? "dirinya sendiri" : target.getNama()).append(".\n");
        narasi.append(target.getNama()).append(" pulih ").append(pulih)
              .append(" nyawa (nyawa ").append(target.getNyawa())
              .append('/').append(target.getNyawaMaks()).append(").");
        return narasi.toString();
    }
}
