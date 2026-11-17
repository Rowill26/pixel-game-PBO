package id.girah.karakter;

import java.util.List;

/**
 * Telik Sandi (peran Pengintai dalam naskah): mata-mata tercepat.
 * Jurus Langkah Bayangan menyerang lalu memberi satu giliran tambahan, sekali per ronde.
 */
public class TelikSandi extends Pengelana {

    public static final int NYAWA_AWAL = 90;
    public static final int ENERGI_BATIN_AWAL = 40;
    public static final int KEKUATAN_AWAL = 12;
    public static final int PERTAHANAN_AWAL = 5;
    public static final int KECEPATAN_AWAL = 18;

    public static final int BIAYA_JURUS = 12;

    private boolean sudahLangkahBayanganRondeIni;
    private boolean mintaGiliranTambahan;

    public TelikSandi(String nama) {
        super(nama, NYAWA_AWAL, ENERGI_BATIN_AWAL, KEKUATAN_AWAL, PERTAHANAN_AWAL, KECEPATAN_AWAL);
    }

    @Override
    public String serang(KarakterMitos target) {
        if (!bisaMenyerang(target)) {
            return narasiAksiBatal(target);
        }
        StringBuilder narasi = new StringBuilder();
        narasi.append(getNama()).append(" melesat dari bayangan dan menikam ")
              .append(target.getNama()).append("!\n");
        narasi.append(laporanKerusakan(target, target.terimaSerangan(getKekuatan())));
        return narasi.toString();
    }

    /**
     * Langkah Bayangan: serangan biasa, lalu Telik Sandi berhak atas satu giliran tambahan.
     *
     * @throws IllegalStateException jika sudah dipakai di ronde ini; cek {@link #bisaMemakaiJurus()} dulu
     */
    @Override
    public String gunakanJurus(List<KarakterMitos> sasaran) throws EnergiBatinHabisException {
        KarakterMitos target = sasaranTunggal(sasaran);
        if (sudahLangkahBayanganRondeIni) {
            throw new IllegalStateException(getNamaJurus() + " hanya bisa dipakai sekali per ronde");
        }
        if (!bisaMenyerang(target)) {
            return narasiAksiBatal(target);
        }
        pakaiEnergiBatin(BIAYA_JURUS);
        sudahLangkahBayanganRondeIni = true;
        mintaGiliranTambahan = true;

        return getNama() + " lenyap dalam " + getNamaJurus() + "!\n"
                + serang(target) + "\n"
                + getNama() + " muncul kembali, siap bergerak sekali lagi!";
    }

    @Override
    public boolean bisaMemakaiJurus() {
        return super.bisaMemakaiJurus() && !sudahLangkahBayanganRondeIni;
    }

    @Override
    public boolean ambilGiliranTambahan() {
        boolean hasil = mintaGiliranTambahan && isHidup();
        mintaGiliranTambahan = false;
        return hasil;
    }

    @Override
    public void awalRonde() {
        sudahLangkahBayanganRondeIni = false;
        mintaGiliranTambahan = false;
    }

    @Override
    public String getNamaJurus() {
        return "Langkah Bayangan";
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
        return "Telik Sandi";
    }
}
