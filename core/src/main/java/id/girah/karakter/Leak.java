package id.girah.karakter;

import java.util.List;

/**
 * Leak: makhluk gaib penjaga ritual. Musuh standar yang muncul berkelompok.
 *
 * <p>Catatan budaya (dari naskah): penggambaran leak di game ini adalah fantasi
 * dan perlu ditinjau bersama narasumber budaya Bali.
 */
public class Leak extends MakhlukGirah {

    public static final int NYAWA_AWAL = 60;
    public static final int ENERGI_BATIN_AWAL = 20;
    public static final int KEKUATAN_AWAL = 13;
    public static final int PERTAHANAN_AWAL = 3;
    public static final int KECEPATAN_AWAL = 8;

    public static final int BIAYA_JURUS = 10;
    public static final int PENGALI_JURUS = 2;

    public Leak(String nama) {
        super(nama, NYAWA_AWAL, ENERGI_BATIN_AWAL, KEKUATAN_AWAL, PERTAHANAN_AWAL, KECEPATAN_AWAL);
    }

    @Override
    public String serang(KarakterMitos target) {
        if (!bisaMenyerang(target)) {
            return narasiAksiBatal(target);
        }
        StringBuilder narasi = new StringBuilder();
        narasi.append(getNama()).append(" melayang rendah dan mencakar ")
              .append(target.getNama()).append("!\n");
        narasi.append(laporanKerusakan(target, target.terimaSerangan(getKekuatan())));
        return narasi.toString();
    }

    /** Jerat Kabut: kekuatan x {@value #PENGALI_JURUS} ke satu lawan. */
    @Override
    public String gunakanJurus(List<KarakterMitos> sasaran) throws EnergiBatinHabisException {
        KarakterMitos target = sasaranTunggal(sasaran);
        if (!bisaMenyerang(target)) {
            return narasiAksiBatal(target);
        }
        pakaiEnergiBatin(BIAYA_JURUS);

        StringBuilder narasi = new StringBuilder();
        narasi.append("Kabut ungu menggulung dari tubuh ").append(getNama())
              .append(" dan menjerat ").append(target.getNama()).append("!\n");
        narasi.append(laporanKerusakan(target, target.terimaSerangan(getKekuatan() * PENGALI_JURUS)));
        return narasi.toString();
    }

    @Override
    protected String narasiGugur() {
        return getNama() + " memudar menjadi asap dupa dan lenyap.";
    }

    @Override
    public String getNamaJurus() {
        return "Jerat Kabut";
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
    public String getPeran() {
        return "Leak";
    }

    @Override
    public String getIdSprite() {
        return "leak";
    }
}
