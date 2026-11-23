package id.girah.battle;

import id.girah.karakter.EnergiBatinHabisException;
import id.girah.karakter.Faksi;
import id.girah.karakter.JarakAksi;
import id.girah.karakter.KarakterMitos;
import id.girah.karakter.MakhlukGirah;
import id.girah.karakter.Pengelana;
import id.girah.pusaka.Pusaka;
import id.girah.pusaka.TasPusaka;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Otak pertarungan berbasis giliran. Murni Java: tidak tahu apa pun tentang LibGDX.
 *
 * <h2>Alur</h2>
 * <ol>
 *   <li>{@link #mulai()} menyusun urutan giliran ronde pertama.</li>
 *   <li>Siapa pun yang sedang mendapat giliran ({@link #getAktor()}) bertindak lewat
 *       {@link #serang} atau {@link #gunakanJurus}. Pemain memanggilnya dari tombol UI,
 *       AI memanggilnya dari logika musuh. BattleManager tidak membedakan keduanya.</li>
 *   <li>Setelah aksi, narasi dicatat, kondisi menang/kalah diperiksa, lalu giliran maju.</li>
 * </ol>
 *
 * <h2>Urutan giliran</h2>
 * Disimpan dalam {@link ArrayList} dan disusun ulang setiap ronde: tercepat lebih dulu,
 * dan jika seri, pengelana didahulukan. Giliran tambahan (Langkah Bayangan Telik Sandi)
 * disisipkan dengan {@code add(indeks + 1, aktor)}. Karena list diubah di tengah ronde,
 * posisi giliran dilacak dengan indeks manual, bukan for-each (for-each akan melempar
 * {@code ConcurrentModificationException}).
 *
 * <h2>Thread</h2>
 * Kelas ini tidak thread-safe. Semua pemanggilan harus dari satu thread, yaitu render
 * thread LibGDX. Thread AI cukup menunggu, lalu mengirim aksinya lewat
 * {@code Gdx.app.postRunnable(...)}.
 */
public class BattleManager {

    /** Nilai batas ronde yang berarti pertarungan tidak dibatasi waktu. */
    public static final int TANPA_BATAS_RONDE = 0;

    /** Nama aksi serangan dasar di {@link HasilAksi#getNamaAksi()}. */
    public static final String NAMA_SERANGAN = "Serangan";

    private final List<KarakterMitos> kelompok;
    private final List<KarakterMitos> lawan;
    private final int batasRonde;
    private final TasPusaka tasPusaka;

    private final List<KarakterMitos> urutanGiliran = new ArrayList<>();
    private final AntreanDialog antreanDialog = new AntreanDialog();
    private final BabadPertarungan babad = new BabadPertarungan();
    private final List<BattleListener> pendengar = new ArrayList<>();

    private int indeksGiliran;
    private int ronde;
    private StatusBattle status = StatusBattle.BELUM_MULAI;

    public BattleManager(List<? extends Pengelana> kelompok, List<? extends MakhlukGirah> lawan) {
        this(kelompok, lawan, TANPA_BATAS_RONDE);
    }

    /**
     * @param batasRonde jumlah ronde maksimum; setelah ronde terakhir selesai dan lawan masih
     *                   hidup, hasilnya {@link StatusBattle#WAKTU_HABIS}. Pakai
     *                   {@link #TANPA_BATAS_RONDE} untuk pertarungan biasa.
     */
    public BattleManager(List<? extends Pengelana> kelompok, List<? extends MakhlukGirah> lawan,
                         int batasRonde) {
        this(kelompok, lawan, batasRonde, new TasPusaka());
    }

    /**
     * @param tasPusaka tas milik kelompok. Objek yang sama dipakai lintas pertarungan,
     *                  jadi pusaka yang terpakai di satu babak tidak kembali di babak berikutnya.
     */
    public BattleManager(List<? extends Pengelana> kelompok, List<? extends MakhlukGirah> lawan,
                         int batasRonde, TasPusaka tasPusaka) {
        this.tasPusaka = Objects.requireNonNull(tasPusaka, "Tas Pusaka tidak boleh null");
        this.kelompok = List.copyOf(Objects.requireNonNull(kelompok, "Kelompok tidak boleh null"));
        this.lawan = List.copyOf(Objects.requireNonNull(lawan, "Lawan tidak boleh null"));
        if (this.kelompok.isEmpty() || this.lawan.isEmpty()) {
            throw new IllegalArgumentException("Kedua pihak harus punya minimal satu karakter");
        }
        if (batasRonde < 0) {
            throw new IllegalArgumentException("Batas ronde tidak boleh negatif");
        }
        Map<KarakterMitos, Boolean> sudahAda = new IdentityHashMap<>();
        for (KarakterMitos k : semuaPeserta()) {
            if (sudahAda.put(k, Boolean.TRUE) != null) {
                throw new IllegalArgumentException(k.getNama() + " terdaftar lebih dari sekali");
            }
        }
        if (!adaYangHidup(this.kelompok) || !adaYangHidup(this.lawan)) {
            throw new IllegalArgumentException("Kedua pihak harus punya minimal satu karakter yang hidup");
        }
        this.batasRonde = batasRonde;
    }

    // ================================================================
    // Pendaftaran listener dan memulai
    // ================================================================

    public void tambahListener(BattleListener listener) {
        pendengar.add(Objects.requireNonNull(listener, "Listener tidak boleh null"));
    }

    public void hapusListener(BattleListener listener) {
        pendengar.remove(listener);
    }

    public void mulai() {
        if (status != StatusBattle.BELUM_MULAI) {
            throw new IllegalStateException("Pertarungan sudah dimulai");
        }
        mulaiRonde();
        umumkanGiliran();
    }

    // ================================================================
    // Aksi untuk karakter yang sedang mendapat giliran
    // ================================================================

    /**
     * Aktor saat ini menyerang {@code target}.
     *
     * @return narasi aksi
     * @throws IllegalStateException    jika pertarungan tidak sedang berjalan
     * @throws IllegalArgumentException jika target bukan lawan aktor yang masih hidup
     */
    public String serang(KarakterMitos target) {
        KarakterMitos aktor = getAktor();
        wajibSasaranSah(target, getSasaranSerang(), "serangan");
        Map<KarakterMitos, StatusSesaat> sebelum = potretStatus();
        String narasi = aktor.serang(target);
        return selesaikanAksi(new HasilAksi(aktor, JenisAksi.SERANG, NAMA_SERANGAN, aktor.getJarakSerang(),
                List.of(target), perubahanSejak(sebelum), narasi));
    }

    /**
     * Aktor saat ini memakai jurusnya. Untuk jurus {@code SEMUA_LAWAN}, {@code target}
     * diabaikan (boleh {@code null}) karena semua lawan hidup otomatis menjadi sasaran.
     *
     * <p>Jika {@link EnergiBatinHabisException} terlempar, giliran <b>tidak</b> habis:
     * pemain boleh memilih aksi lain.
     *
     * @return narasi aksi
     * @throws EnergiBatinHabisException jika energi batin aktor tidak cukup
     */
    public String gunakanJurus(KarakterMitos target) throws EnergiBatinHabisException {
        KarakterMitos aktor = getAktor();
        List<KarakterMitos> sasaranSah = getSasaranJurus();
        List<KarakterMitos> sasaran;
        switch (aktor.getSasaranJurus()) {
            case SEMUA_LAWAN:
                sasaran = sasaranSah;
                break;
            case SATU_LAWAN:
            case SATU_KAWAN:
                wajibSasaranSah(target, sasaranSah, aktor.getNamaJurus());
                sasaran = List.of(target);
                break;
            default:
                throw new IllegalStateException("Jenis sasaran tidak dikenal: " + aktor.getSasaranJurus());
        }
        Map<KarakterMitos, StatusSesaat> sebelum = potretStatus();
        String narasi = aktor.gunakanJurus(sasaran);   // exception keluar sebelum ada event dan giliran maju
        return selesaikanAksi(new HasilAksi(aktor, JenisAksi.JURUS, aktor.getNamaJurus(), aktor.getJarakJurus(),
                sasaran, perubahanSejak(sebelum), narasi));
    }

    /**
     * Pengelana yang sedang mendapat giliran memakai satu pusaka dari Tas Pusaka.
     *
     * @return narasi aksi
     * @throws IllegalStateException    jika bukan giliran pengelana, atau pusaka tidak ada di tas
     * @throws IllegalArgumentException jika target bukan kawan yang terbantu pusaka ini
     */
    public String gunakanPusaka(Pusaka pusaka, KarakterMitos target) {
        KarakterMitos aktor = getAktor();
        if (aktor.getFaksi() != Faksi.PENGELANA) {
            throw new IllegalStateException("Hanya pengelana yang membawa Tas Pusaka");
        }
        Objects.requireNonNull(pusaka, "Pusaka tidak boleh null");
        if (!tasPusaka.punya(pusaka)) {
            throw new IllegalStateException(pusaka.getNama() + " tidak ada di Tas Pusaka");
        }
        wajibSasaranSah(target, getSasaranPusaka(pusaka), pusaka.getNama());
        Map<KarakterMitos, StatusSesaat> sebelum = potretStatus();
        String narasi = tasPusaka.gunakan(pusaka, aktor, target);
        return selesaikanAksi(new HasilAksi(aktor, JenisAksi.PUSAKA, pusaka.getNama(), JarakAksi.JAUH,
                List.of(target), perubahanSejak(sebelum), narasi));
    }

    /** Kawan yang terbantu oleh {@code pusaka} saat ini (hidup dan belum penuh). */
    public List<KarakterMitos> getSasaranPusaka(Pusaka pusaka) {
        Objects.requireNonNull(pusaka, "Pusaka tidak boleh null");
        List<KarakterMitos> sasaran = new ArrayList<>();
        for (KarakterMitos k : kawanDari(getAktor())) {
            if (pusaka.bisaDipakaiPada(k)) {
                sasaran.add(k);
            }
        }
        return sasaran;
    }

    /** Lawan hidup yang boleh diserang oleh aktor saat ini. */
    public List<KarakterMitos> getSasaranSerang() {
        return yangHidup(lawanDari(getAktor()));
    }

    /**
     * Karakter yang boleh dipilih sebagai sasaran jurus aktor saat ini, menurut
     * {@link KarakterMitos#bisaMenjadiSasaranJurus}. Jika kosong (mis. Balian saat semua kawan
     * sehat), UI menonaktifkan tombol jurus.
     */
    public List<KarakterMitos> getSasaranJurus() {
        KarakterMitos aktor = getAktor();
        List<KarakterMitos> calon;
        switch (aktor.getSasaranJurus()) {
            case SATU_KAWAN:
                calon = kawanDari(aktor);
                break;
            case SATU_LAWAN:
            case SEMUA_LAWAN:
                calon = lawanDari(aktor);
                break;
            default:
                throw new IllegalStateException("Jenis sasaran tidak dikenal: " + aktor.getSasaranJurus());
        }
        List<KarakterMitos> sah = new ArrayList<>();
        for (KarakterMitos k : calon) {
            if (aktor.bisaMenjadiSasaranJurus(k)) {
                sah.add(k);
            }
        }
        return sah;
    }

    // ================================================================
    // Inti: setelah aksi, catat, periksa akhir, majukan giliran
    // ================================================================

    private String selesaikanAksi(HasilAksi hasil) {
        for (BattleListener l : salinanPendengar()) {
            l.onAksi(hasil);
        }
        String narasi = hasil.getNarasi();
        catat(narasi);
        if (periksaAkhirPertarungan()) {
            return narasi;
        }
        KarakterMitos aktor = hasil.getAktor();
        if (aktor.isHidup() && aktor.ambilGiliranTambahan()) {
            urutanGiliran.add(indeksGiliran + 1, aktor);
        }
        majuKeGiliranBerikutnya();
        return narasi;
    }

    /** Nyawa dan energi batin satu karakter pada satu saat. */
    private static final class StatusSesaat {
        final int nyawa;
        final int energiBatin;

        StatusSesaat(KarakterMitos k) {
            this.nyawa = k.getNyawa();
            this.energiBatin = k.getEnergiBatin();
        }
    }

    /** Mencatat status semua peserta tepat sebelum aksi, untuk dibandingkan sesudahnya. */
    private Map<KarakterMitos, StatusSesaat> potretStatus() {
        Map<KarakterMitos, StatusSesaat> potret = new IdentityHashMap<>();
        for (KarakterMitos k : semuaPeserta()) {
            potret.put(k, new StatusSesaat(k));
        }
        return potret;
    }

    /** Peserta yang nyawa atau energinya berubah sejak {@code sebelum}, urut kelompok lalu lawan. */
    private List<PerubahanStatus> perubahanSejak(Map<KarakterMitos, StatusSesaat> sebelum) {
        List<PerubahanStatus> perubahan = new ArrayList<>();
        for (KarakterMitos k : semuaPeserta()) {
            StatusSesaat lama = sebelum.get(k);
            int selisihNyawa = k.getNyawa() - lama.nyawa;
            int selisihEnergi = k.getEnergiBatin() - lama.energiBatin;
            if (selisihNyawa != 0 || selisihEnergi != 0) {
                boolean gugur = lama.nyawa > 0 && !k.isHidup();
                perubahan.add(new PerubahanStatus(k, selisihNyawa, selisihEnergi, gugur));
            }
        }
        return perubahan;
    }

    private void majuKeGiliranBerikutnya() {
        indeksGiliran++;
        while (indeksGiliran < urutanGiliran.size() && !urutanGiliran.get(indeksGiliran).isHidup()) {
            indeksGiliran++;   // lewati yang gugur di tengah ronde
        }
        if (indeksGiliran >= urutanGiliran.size()) {
            if (batasRonde != TANPA_BATAS_RONDE && ronde >= batasRonde) {
                akhiri(StatusBattle.WAKTU_HABIS);
                return;
            }
            mulaiRonde();
        }
        umumkanGiliran();
    }

    private void mulaiRonde() {
        ronde++;
        urutanGiliran.clear();
        for (KarakterMitos k : semuaPeserta()) {
            if (k.isHidup()) {
                k.awalRonde();
                urutanGiliran.add(k);
            }
        }
        urutanGiliran.sort(URUTAN_GILIRAN);
        indeksGiliran = 0;

        String judul = "Ronde " + ronde + (batasRonde != TANPA_BATAS_RONDE ? " dari " + batasRonde : "");
        babad.catatJudul(judul);
        for (BattleListener l : salinanPendengar()) {
            l.onRondeBaru(ronde);
        }
    }

    /** Tercepat dulu; jika seri, pengelana dulu. Sort Java stabil, jadi seri sisanya tetap urutan daftar. */
    private static final Comparator<KarakterMitos> URUTAN_GILIRAN =
            Comparator.comparingInt(KarakterMitos::getKecepatan).reversed()
                      .thenComparing(k -> k.getFaksi() == Faksi.PENGELANA ? 0 : 1);

    private boolean periksaAkhirPertarungan() {
        if (!adaYangHidup(lawan)) {
            akhiri(StatusBattle.MENANG);
            return true;
        }
        if (!adaYangHidup(kelompok)) {
            akhiri(StatusBattle.KALAH);
            return true;
        }
        return false;
    }

    private void akhiri(StatusBattle hasil) {
        status = hasil;
        switch (hasil) {
            case MENANG:
                catat("Lawan terakhir tumbang. Kelompok pengelana memenangkan pertarungan!");
                break;
            case KALAH:
                catat("Seluruh pengelana gugur. Kabut Girah menelan medan pertempuran.");
                break;
            case WAKTU_HABIS:
                catat("Waktu habis! Ritual mencapai puncaknya sebelum lawan dapat dihentikan.");
                break;
            default:
                throw new IllegalArgumentException("Bukan status akhir: " + hasil);
        }
        for (BattleListener l : salinanPendengar()) {
            l.onBattleSelesai(hasil);
        }
    }

    private void umumkanGiliran() {
        KarakterMitos aktor = urutanGiliran.get(indeksGiliran);
        status = aktor.getFaksi() == Faksi.PENGELANA ? StatusBattle.GILIRAN_PEMAIN : StatusBattle.GILIRAN_MUSUH;
        for (BattleListener l : salinanPendengar()) {
            l.onGiliranBaru(aktor, status);
        }
    }

    private void catat(String narasi) {
        antreanDialog.tambah(narasi);
        babad.catat(narasi);
        for (BattleListener l : salinanPendengar()) {
            l.onNarasi(narasi);
        }
    }

    // ================================================================
    // Getter untuk UI
    // ================================================================

    /**
     * Karakter yang sedang mendapat giliran.
     *
     * @throws IllegalStateException jika pertarungan belum dimulai atau sudah selesai
     */
    public KarakterMitos getAktor() {
        if (status == StatusBattle.BELUM_MULAI || status.isSelesai()) {
            throw new IllegalStateException("Tidak ada giliran: status pertarungan " + status);
        }
        return urutanGiliran.get(indeksGiliran);
    }

    /** Sisa urutan ronde ini mulai dari aktor sekarang, tanpa yang gugur. Untuk panel antrean giliran. */
    public List<KarakterMitos> getAntrianGiliran() {
        if (status == StatusBattle.BELUM_MULAI || status.isSelesai()) {
            return List.of();
        }
        List<KarakterMitos> sisa = new ArrayList<>();
        for (int i = indeksGiliran; i < urutanGiliran.size(); i++) {
            if (urutanGiliran.get(i).isHidup()) {
                sisa.add(urutanGiliran.get(i));
            }
        }
        return Collections.unmodifiableList(sisa);
    }

    public StatusBattle getStatus() {
        return status;
    }

    public int getRonde() {
        return ronde;
    }

    public int getBatasRonde() {
        return batasRonde;
    }

    public List<KarakterMitos> getKelompok() {
        return kelompok;
    }

    public List<KarakterMitos> getLawan() {
        return lawan;
    }

    public AntreanDialog getAntreanDialog() {
        return antreanDialog;
    }

    public BabadPertarungan getBabad() {
        return babad;
    }

    public TasPusaka getTasPusaka() {
        return tasPusaka;
    }

    // ================================================================
    // Bantuan
    // ================================================================

    private List<KarakterMitos> semuaPeserta() {
        List<KarakterMitos> semua = new ArrayList<>(kelompok);
        semua.addAll(lawan);
        return semua;
    }

    private List<KarakterMitos> lawanDari(KarakterMitos aktor) {
        return aktor.getFaksi() == Faksi.PENGELANA ? lawan : kelompok;
    }

    private List<KarakterMitos> kawanDari(KarakterMitos aktor) {
        return aktor.getFaksi() == Faksi.PENGELANA ? kelompok : lawan;
    }

    private static List<KarakterMitos> yangHidup(List<KarakterMitos> daftar) {
        List<KarakterMitos> hidup = new ArrayList<>();
        for (KarakterMitos k : daftar) {
            if (k.isHidup()) {
                hidup.add(k);
            }
        }
        return hidup;
    }

    private static boolean adaYangHidup(List<KarakterMitos> daftar) {
        for (KarakterMitos k : daftar) {
            if (k.isHidup()) {
                return true;
            }
        }
        return false;
    }

    private static void wajibSasaranSah(KarakterMitos target, List<KarakterMitos> sasaranSah, String aksi) {
        Objects.requireNonNull(target, "Sasaran " + aksi + " tidak boleh null");
        for (KarakterMitos k : sasaranSah) {
            if (k == target) {
                return;
            }
        }
        throw new IllegalArgumentException(target.getNama() + " bukan sasaran yang sah untuk " + aksi);
    }

    /** Salinan, supaya listener boleh melepas dirinya sendiri saat dipanggil. */
    private List<BattleListener> salinanPendengar() {
        return new ArrayList<>(pendengar);
    }
}
