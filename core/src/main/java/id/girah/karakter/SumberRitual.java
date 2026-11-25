package id.girah.karakter;

import java.util.List;

/**
 * Sumber ritual: altar di pemakaman dekat Candi Durga, sasaran fase terakhir Babak 5.
 * Kelompok harus menghancurkannya sebelum batas ronde habis.
 *
 * <p>Altar tidak bergerak dan tidak memilih korban. Setiap gilirannya ia berdenyut:
 * jurus Denyut Ritual tidak memakai energi dan mengenai semua pengelana sekaligus
 * (kerusakan berkala). Serangan biasanya hanya dipakai jika tinggal satu sasaran.
 */
public class SumberRitual extends MakhlukGirah {

    public static final int NYAWA_AWAL = 180;
    public static final int ENERGI_BATIN_AWAL = 0;
    public static final int KEKUATAN_AWAL = 10;
    public static final int PERTAHANAN_AWAL = 2;
    public static final int KECEPATAN_AWAL = 4;

    /** Denyut Ritual tidak butuh energi: altar mengambil tenaganya dari ritual itu sendiri. */
    public static final int BIAYA_JURUS = 0;
    public static final int KEKUATAN_DENYUT = 9;

    public SumberRitual() {
        super("Sumber Ritual", NYAWA_AWAL, ENERGI_BATIN_AWAL, KEKUATAN_AWAL, PERTAHANAN_AWAL, KECEPATAN_AWAL);
    }

    @Override
    public String serang(KarakterMitos target) {
        if (!bisaMenyerang(target)) {
            return narasiAksiBatal(target);
        }
        StringBuilder narasi = new StringBuilder();
        narasi.append("Api di altar menjilat ke arah ").append(target.getNama()).append("!\n");
        narasi.append(laporanKerusakan(target, target.terimaSerangan(getKekuatan())));
        return narasi.toString();
    }

    /** Denyut Ritual: {@value #KEKUATAN_DENYUT} kekuatan ke setiap pengelana yang masih hidup. */
    @Override
    public String gunakanJurus(List<KarakterMitos> sasaran) throws EnergiBatinHabisException {
        List<KarakterMitos> hidup = sasaranHidup(sasaran);
        if (!isHidup() || hidup.isEmpty()) {
            return getNama() + " berdenyut pelan, tetapi tidak ada yang tersisa untuk dibakar.";
        }
        pakaiEnergiBatin(BIAYA_JURUS);

        StringBuilder narasi = new StringBuilder();
        narasi.append("Lingkaran di bawah altar menyala merah. ")
              .append(getNamaJurus().toUpperCase()).append("! Panas ritual menyapu seluruh kelompok.");
        for (KarakterMitos target : hidup) {
            narasi.append('\n').append(laporanKerusakan(target, target.terimaSerangan(KEKUATAN_DENYUT)));
        }
        return narasi.toString();
    }

    @Override
    protected String ucapanNyawaRendah() {
        return "Batu altar retak. Cahaya merahnya berkedip tidak beraturan.";
    }

    @Override
    protected String narasiGugur() {
        return "Altar runtuh. Lingkaran ritual padam, dan kabut di atas pemakaman mulai menipis.";
    }

    @Override
    public String getNamaJurus() {
        return "Denyut Ritual";
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
    public JarakAksi getJarakSerang() {
        return JarakAksi.JAUH;      // altar tidak bergerak
    }

    @Override
    public String getPeran() {
        return "Sumber Ritual";
    }

    @Override
    public String getIdSprite() {
        return "sumber_ritual";
    }
}
