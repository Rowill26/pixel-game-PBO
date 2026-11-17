package id.girah.karakter;

import java.util.List;

/**
 * Calon Arang, janda sakti dari Girah. Bos pertarungan akhir.
 * Jurus Sebar Wabah mengenai semua pengelana, dan ia berucap getir
 * sekali saja ketika nyawanya jatuh ke {@value #AMBANG_NYAWA_RENDAH_PERSEN}% atau kurang.
 */
public class CalonArang extends MakhlukGirah {

    public static final int NYAWA_AWAL = 400;
    public static final int ENERGI_BATIN_AWAL = 120;
    public static final int KEKUATAN_AWAL = 20;
    public static final int PERTAHANAN_AWAL = 10;
    public static final int KECEPATAN_AWAL = 12;

    public static final int BIAYA_JURUS = 30;
    public static final int KEKUATAN_WABAH = 22;
    public static final int AMBANG_NYAWA_RENDAH_PERSEN = 30;

    private boolean sudahBerucapNyawaRendah;

    public CalonArang() {
        super("Calon Arang", NYAWA_AWAL, ENERGI_BATIN_AWAL, KEKUATAN_AWAL, PERTAHANAN_AWAL, KECEPATAN_AWAL);
    }

    @Override
    public String serang(KarakterMitos target) {
        if (!bisaMenyerang(target)) {
            return narasiAksiBatal(target);
        }
        StringBuilder narasi = new StringBuilder();
        narasi.append(getNama()).append(" mengangkat tangan. Asap dupa berputar menghantam ")
              .append(target.getNama()).append("!\n");
        narasi.append(laporanKerusakan(target, target.terimaSerangan(getKekuatan())));
        return narasi.toString();
    }

    /** Sebar Wabah: {@value #KEKUATAN_WABAH} kekuatan ke setiap pengelana yang masih hidup. */
    @Override
    public String gunakanJurus(List<KarakterMitos> sasaran) throws EnergiBatinHabisException {
        List<KarakterMitos> hidup = sasaranHidup(sasaran);
        if (!isHidup() || hidup.isEmpty()) {
            return getNama() + " terdiam. Kabutnya tak menemukan siapa pun.";
        }
        pakaiEnergiBatin(BIAYA_JURUS);

        StringBuilder narasi = new StringBuilder();
        narasi.append(getNama()).append(": \"Rasakan panas dan dingin yang ditanggung Girah!\"\n");
        narasi.append(getNamaJurus().toUpperCase()).append("! Kabut wabah menyelimuti seluruh kelompok.");
        for (KarakterMitos target : hidup) {
            narasi.append('\n').append(laporanKerusakan(target, target.terimaSerangan(KEKUATAN_WABAH)));
        }
        return narasi.toString();
    }

    @Override
    protected String reaksiSetelahTerluka() {
        if (sudahBerucapNyawaRendah || !isNyawaRendah()) {
            return "";
        }
        sudahBerucapNyawaRendah = true;
        return getNama() + ": \"Mereka membuat putriku menangis! Apa lagi yang tersisa untukku?\"";
    }

    @Override
    protected String narasiGugur() {
        return getNama() + " jatuh berlutut di tengah lingkaran ritual. Cahayanya meredup.";
    }

    /** {@code true} jika nyawa sudah di bawah atau sama dengan ambang nyawa rendah. */
    public boolean isNyawaRendah() {
        return getNyawa() * 100 <= getNyawaMaks() * AMBANG_NYAWA_RENDAH_PERSEN;
    }

    @Override
    public String getNamaJurus() {
        return "Sebar Wabah";
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
        return "Janda Sakti dari Girah";
    }
}
