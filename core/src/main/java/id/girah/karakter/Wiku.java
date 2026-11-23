package id.girah.karakter;

import java.util.List;

/**
 * Wiku (peran Juru Mantra dalam naskah): pendeta perapal mantra suci.
 * Rapuh, tetapi jurusnya mengenai semua lawan sekaligus.
 */
public class Wiku extends Pengelana {

    public static final int NYAWA_AWAL = 85;
    public static final int ENERGI_BATIN_AWAL = 80;
    public static final int KEKUATAN_AWAL = 8;
    public static final int PERTAHANAN_AWAL = 4;
    public static final int KECEPATAN_AWAL = 9;

    public static final int BIAYA_JURUS = 25;
    public static final int KEKUATAN_MANTRA = 22;

    public Wiku(String nama) {
        super(nama, NYAWA_AWAL, ENERGI_BATIN_AWAL, KEKUATAN_AWAL, PERTAHANAN_AWAL, KECEPATAN_AWAL);
    }

    @Override
    public String serang(KarakterMitos target) {
        if (!bisaMenyerang(target)) {
            return narasiAksiBatal(target);
        }
        StringBuilder narasi = new StringBuilder();
        narasi.append(getNama()).append(" mengayunkan tongkat cendana ke arah ")
              .append(target.getNama()).append(".\n");
        narasi.append(laporanKerusakan(target, target.terimaSerangan(getKekuatan())));
        return narasi.toString();
    }

    /** Mantra Lingkar Suci: {@value #KEKUATAN_MANTRA} kekuatan ke setiap lawan yang masih hidup. */
    @Override
    public String gunakanJurus(List<KarakterMitos> sasaran) throws EnergiBatinHabisException {
        List<KarakterMitos> hidup = sasaranHidup(sasaran);
        if (!isHidup()) {
            return getNama() + " sudah gugur dan tidak bisa merapal mantra.";
        }
        if (hidup.isEmpty()) {
            return getNama() + " menurunkan tangannya. Tak ada lawan yang tersisa.";
        }
        pakaiEnergiBatin(BIAYA_JURUS);

        StringBuilder narasi = new StringBuilder();
        narasi.append(getNama()).append(" merapal ").append(getNamaJurus())
              .append(". Cahaya putih melingkar di sekeliling medan!");
        for (KarakterMitos target : hidup) {
            narasi.append('\n').append(laporanKerusakan(target, target.terimaSerangan(KEKUATAN_MANTRA)));
        }
        return narasi.toString();
    }

    @Override
    public String getNamaJurus() {
        return "Mantra Lingkar Suci";
    }

    @Override
    public int getBiayaJurus() {
        return BIAYA_JURUS;
    }

    @Override
    public JenisSasaran getSasaranJurus() {
        return JenisSasaran.SEMUA_LAWAN;
    }

    @Override
    public String getPeran() {
        return "Juru Mantra";
    }

    @Override
    public String getIdSprite() {
        return "juru_mantra";
    }
}
