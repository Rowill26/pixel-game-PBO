package id.girah.pusaka;

import id.girah.karakter.KarakterMitos;

/** Kembang Kantil: bunga harum yang memulihkan {@value #PEMULIHAN_ENERGI} energi batin. */
public class KembangKantil extends Pusaka {

    public static final int PEMULIHAN_ENERGI = 30;

    public KembangKantil() {
        super("Kembang Kantil", "Bunga kantil yang menenangkan batin. Memulihkan "
                + PEMULIHAN_ENERGI + " energi batin.");
    }

    @Override
    public boolean bisaDipakaiPada(KarakterMitos target) {
        return target.isHidup() && target.getEnergiBatin() < target.getEnergiBatinMaks();
    }

    @Override
    public String gunakan(KarakterMitos pengguna, KarakterMitos target) {
        int pulih = target.pulihkanEnergiBatin(PEMULIHAN_ENERGI);
        StringBuilder narasi = new StringBuilder();
        narasi.append(pengguna.getNama()).append(" menyelipkan Kembang Kantil ke tangan ")
              .append(pengguna == target ? "sendiri" : target.getNama())
              .append(". Harumnya menenangkan batin.\n");
        narasi.append(target.getNama()).append(" memulihkan ").append(pulih)
              .append(" energi batin (energi batin ").append(target.getEnergiBatin())
              .append('/').append(target.getEnergiBatinMaks()).append(").");
        return narasi.toString();
    }
}
