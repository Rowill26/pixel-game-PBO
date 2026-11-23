package id.girah.karakter;

import java.util.List;

/**
 * Senopati (peran Pendekar dalam naskah): panglima garis depan.
 * Nyawa dan pertahanan tinggi, jurus fisik bersasaran tunggal.
 */
public class Senopati extends Pengelana {

    public static final int NYAWA_AWAL = 120;
    public static final int ENERGI_BATIN_AWAL = 50;
    public static final int KEKUATAN_AWAL = 15;
    public static final int PERTAHANAN_AWAL = 8;
    public static final int KECEPATAN_AWAL = 10;

    public static final int BIAYA_JURUS = 20;
    public static final int PENGALI_JURUS = 3;

    public Senopati(String nama) {
        super(nama, NYAWA_AWAL, ENERGI_BATIN_AWAL, KEKUATAN_AWAL, PERTAHANAN_AWAL, KECEPATAN_AWAL);
    }

    @Override
    public String serang(KarakterMitos target) {
        if (!bisaMenyerang(target)) {
            return narasiAksiBatal(target);
        }
        StringBuilder narasi = new StringBuilder();
        narasi.append(getNama()).append(" menghunus keris dan menebas ")
              .append(target.getNama()).append("!\n");
        narasi.append(laporanKerusakan(target, target.terimaSerangan(getKekuatan())));
        return narasi.toString();
    }

    /** Tebasan Keris Pusaka: kekuatan x {@value #PENGALI_JURUS}, biaya {@value #BIAYA_JURUS} energi batin. */
    @Override
    public String gunakanJurus(List<KarakterMitos> sasaran) throws EnergiBatinHabisException {
        KarakterMitos target = sasaranTunggal(sasaran);
        if (!bisaMenyerang(target)) {
            return narasiAksiBatal(target);
        }
        pakaiEnergiBatin(BIAYA_JURUS);

        StringBuilder narasi = new StringBuilder();
        narasi.append(getNama()).append(" memusatkan tenaga dalam pada keris pusakanya. ")
              .append(getNamaJurus().toUpperCase()).append("!\n");
        narasi.append(laporanKerusakan(target, target.terimaSerangan(getKekuatan() * PENGALI_JURUS)));
        return narasi.toString();
    }

    @Override
    protected String narasiGugur() {
        return getNama() + " roboh berlutut, kerisnya tertancap di tanah.";
    }

    @Override
    public String getNamaJurus() {
        return "Tebasan Keris Pusaka";
    }

    @Override
    public int getBiayaJurus() {
        return BIAYA_JURUS;
    }

    @Override
    public JenisSasaran getSasaranJurus() {
        return JenisSasaran.SATU_LAWAN;
    }

    @Override
    public JarakAksi getJarakJurus() {
        return JarakAksi.DEKAT;     // tebasan keris: maju ke depan sasaran
    }

    @Override
    public String getPeran() {
        return "Senopati";
    }

    @Override
    public String getIdSprite() {
        return "pendekar";
    }
}
