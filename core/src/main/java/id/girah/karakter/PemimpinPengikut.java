package id.girah.karakter;

import java.util.List;

/**
 * Pemimpin para pengikut Calon Arang. Bos pertarungan Babak 4 (serangan ke desa).
 * Jurus Kutukan Kabut Ungu memberi kerusakan besar ke satu pengelana, dan ia
 * berseru sekali saat nyawanya rendah.
 */
public class PemimpinPengikut extends MakhlukGirah {

    public static final int NYAWA_AWAL = 220;
    public static final int ENERGI_BATIN_AWAL = 60;
    public static final int KEKUATAN_AWAL = 17;
    public static final int PERTAHANAN_AWAL = 8;
    public static final int KECEPATAN_AWAL = 11;

    public static final int BIAYA_JURUS = 20;
    public static final int KEKUATAN_KUTUKAN = 38;

    public PemimpinPengikut() {
        super("Pemimpin Pengikut", NYAWA_AWAL, ENERGI_BATIN_AWAL, KEKUATAN_AWAL, PERTAHANAN_AWAL, KECEPATAN_AWAL);
    }

    @Override
    public String serang(KarakterMitos target) {
        if (!bisaMenyerang(target)) {
            return narasiAksiBatal(target);
        }
        StringBuilder narasi = new StringBuilder();
        narasi.append(getNama()).append(" menghantamkan tongkat ritualnya ke ")
              .append(target.getNama()).append("!\n");
        narasi.append(laporanKerusakan(target, target.terimaSerangan(getKekuatan())));
        return narasi.toString();
    }

    /** Kutukan Kabut Ungu: {@value #KEKUATAN_KUTUKAN} kekuatan ke satu pengelana. */
    @Override
    public String gunakanJurus(List<KarakterMitos> sasaran) throws EnergiBatinHabisException {
        KarakterMitos target = sasaranTunggal(sasaran);
        if (!bisaMenyerang(target)) {
            return narasiAksiBatal(target);
        }
        pakaiEnergiBatin(BIAYA_JURUS);

        StringBuilder narasi = new StringBuilder();
        narasi.append(getNama()).append(" mengangkat tongkatnya. Kabut ungu melilit ")
              .append(target.getNama()).append(". ").append(getNamaJurus().toUpperCase()).append("!\n");
        narasi.append(laporanKerusakan(target, target.terimaSerangan(KEKUATAN_KUTUKAN)));
        return narasi.toString();
    }

    @Override
    protected String ucapanNyawaRendah() {
        return getNama() + ": \"Jangan mundur! Calon Arang akan membalas kalian semua!\"";
    }

    @Override
    protected String narasiGugur() {
        return getNama() + " roboh. Tanpa pemimpin, para pengikut kehilangan arah.";
    }

    @Override
    public boolean isBos() {
        return true;
    }

    @Override
    public JarakAksi getJarakJurus() {
        return JarakAksi.JAUH;
    }

    @Override
    public String getNamaJurus() {
        return "Kutukan Kabut Ungu";
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
        return "Pemimpin Pengikut";
    }

    @Override
    public String getIdSprite() {
        return "pemimpin_pengikut";
    }
}
