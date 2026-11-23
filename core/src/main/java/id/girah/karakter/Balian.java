package id.girah.karakter;

import java.util.List;

/**
 * Balian (peran Peramu dalam naskah): tabib Bali peracik ramuan.
 * Serangannya lemah, jurusnya menyembuhkan satu kawan.
 */
public class Balian extends Pengelana {

    public static final int NYAWA_AWAL = 95;
    public static final int ENERGI_BATIN_AWAL = 60;
    public static final int KEKUATAN_AWAL = 7;
    public static final int PERTAHANAN_AWAL = 6;
    public static final int KECEPATAN_AWAL = 11;

    public static final int BIAYA_JURUS = 15;
    public static final int KEKUATAN_PENYEMBUHAN = 35;

    public Balian(String nama) {
        super(nama, NYAWA_AWAL, ENERGI_BATIN_AWAL, KEKUATAN_AWAL, PERTAHANAN_AWAL, KECEPATAN_AWAL);
    }

    @Override
    public String serang(KarakterMitos target) {
        if (!bisaMenyerang(target)) {
            return narasiAksiBatal(target);
        }
        StringBuilder narasi = new StringBuilder();
        narasi.append(getNama()).append(" melempar kantung getah pahit ke wajah ")
              .append(target.getNama()).append(".\n");
        narasi.append(laporanKerusakan(target, target.terimaSerangan(getKekuatan())));
        return narasi.toString();
    }

    /**
     * Ramuan Penawar: memulihkan {@value #KEKUATAN_PENYEMBUHAN} nyawa satu kawan.
     * Energi batin tidak terpakai jika kawan sudah gugur atau nyawanya sudah penuh.
     */
    @Override
    public String gunakanJurus(List<KarakterMitos> sasaran) throws EnergiBatinHabisException {
        KarakterMitos kawan = sasaranTunggal(sasaran);
        if (!isHidup()) {
            return getNama() + " sudah gugur dan tidak bisa meracik ramuan.";
        }
        if (!kawan.isHidup()) {
            return getNama() + " berlutut di samping " + kawan.getNama()
                    + ". Ramuan apa pun sudah terlambat.";
        }
        if (kawan.getNyawa() == kawan.getNyawaMaks()) {
            return kawan.getNama() + " tidak terluka. " + getNama() + " menyimpan ramuannya.";
        }
        pakaiEnergiBatin(BIAYA_JURUS);

        int pulih = kawan.sembuhkan(KEKUATAN_PENYEMBUHAN);
        StringBuilder narasi = new StringBuilder();
        narasi.append(getNama()).append(" meneteskan ").append(getNamaJurus())
              .append(" ke luka ").append(kawan.getNama()).append(".\n");
        narasi.append(kawan.getNama()).append(" pulih ").append(pulih)
              .append(" nyawa (nyawa ").append(kawan.getNyawa())
              .append('/').append(kawan.getNyawaMaks()).append(").");
        return narasi.toString();
    }

    @Override
    public String getNamaJurus() {
        return "Ramuan Penawar";
    }

    @Override
    public int getBiayaJurus() {
        return BIAYA_JURUS;
    }

    @Override
    public JenisSasaran getSasaranJurus() {
        return JenisSasaran.SATU_KAWAN;
    }

    /** Hanya kawan yang masih hidup dan terluka; menyembuhkan kawan yang sehat tidak ada gunanya. */
    @Override
    public boolean bisaMenjadiSasaranJurus(KarakterMitos calon) {
        return calon.isHidup() && calon.getNyawa() < calon.getNyawaMaks();
    }

    @Override
    public JarakAksi getJarakSerang() {
        return JarakAksi.JAUH;      // melempar kantung getah dari tempatnya
    }

    @Override
    public String getPeran() {
        return "Peramu";
    }

    @Override
    public String getIdSprite() {
        return "peramu";
    }
}
