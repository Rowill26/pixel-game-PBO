package id.girah.karakter;

import java.util.List;

/**
 * Pengikut Calon Arang: warga yang ikut menjalankan ritual. Musuh biasa di Babak 1, 4, dan 5.
 * Jurus Asap Wabah mengenai seluruh kelompok, tetapi jauh lebih lemah dari Sebar Wabah.
 *
 * <p>Pengikut adalah manusia, jadi saat kalah ia menyerah, bukan mati
 * (di Babak 1 seorang pengikut yang tertangkap menjadi sumber petunjuk).
 */
public class Pengikut extends MakhlukGirah {

    public static final int NYAWA_AWAL = 70;
    public static final int ENERGI_BATIN_AWAL = 30;
    public static final int KEKUATAN_AWAL = 11;
    public static final int PERTAHANAN_AWAL = 4;
    public static final int KECEPATAN_AWAL = 9;

    public static final int BIAYA_JURUS = 15;
    public static final int KEKUATAN_ASAP = 12;

    public Pengikut(String nama) {
        super(nama, NYAWA_AWAL, ENERGI_BATIN_AWAL, KEKUATAN_AWAL, PERTAHANAN_AWAL, KECEPATAN_AWAL);
    }

    @Override
    public String serang(KarakterMitos target) {
        if (!bisaMenyerang(target)) {
            return narasiAksiBatal(target);
        }
        StringBuilder narasi = new StringBuilder();
        narasi.append(getNama()).append(" mengayunkan golok ke arah ")
              .append(target.getNama()).append("!\n");
        narasi.append(laporanKerusakan(target, target.terimaSerangan(getKekuatan())));
        return narasi.toString();
    }

    /** Asap Wabah: {@value #KEKUATAN_ASAP} kekuatan ke setiap pengelana yang masih hidup. */
    @Override
    public String gunakanJurus(List<KarakterMitos> sasaran) throws EnergiBatinHabisException {
        List<KarakterMitos> hidup = sasaranHidup(sasaran);
        if (!isHidup()) {
            return getNama() + " sudah menyerah dan tidak bisa bertindak.";
        }
        if (hidup.isEmpty()) {
            return getNama() + " memegang dupanya. Tak ada lawan yang tersisa.";
        }
        pakaiEnergiBatin(BIAYA_JURUS);

        StringBuilder narasi = new StringBuilder();
        narasi.append(getNama()).append(" membakar dupa wabah. Asap kehijauan menyebar ke seluruh kelompok!");
        for (KarakterMitos target : hidup) {
            narasi.append('\n').append(laporanKerusakan(target, target.terimaSerangan(KEKUATAN_ASAP)));
        }
        return narasi.toString();
    }

    @Override
    protected String narasiGugur() {
        return getNama() + " menjatuhkan goloknya dan berlutut, menyerah.";
    }

    @Override
    public String getNamaJurus() {
        return "Asap Wabah";
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
        return "Pengikut";
    }

    @Override
    public String getIdSprite() {
        return "pengikut";
    }
}
