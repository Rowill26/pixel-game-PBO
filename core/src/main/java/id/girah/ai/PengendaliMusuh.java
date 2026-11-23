package id.girah.ai;

import id.girah.battle.BattleListener;
import id.girah.battle.BattleManager;
import id.girah.battle.StatusBattle;
import id.girah.karakter.MakhlukGirah;
import id.girah.karakter.EnergiBatinHabisException;
import id.girah.karakter.KarakterMitos;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Menjalankan giliran musuh dengan thread terpisah.
 *
 * <h2>Alur satu giliran musuh</h2>
 * <pre>
 *  [render thread]  BattleManager mengumumkan GILIRAN_MUSUH
 *                   -> onGiliranBaru(): salin daftar sasaran, start Thread AI
 *  [thread AI]      Thread.sleep(jeda)  -> pemain membaca dialog, game tetap bergerak
 *                   strategi.tentukan() -> KeputusanAksi
 *                   keThreadUtama.execute(...)  (di LibGDX: Gdx.app.postRunnable)
 *  [render thread]  jalankanKeputusan(): cek keputusan masih berlaku, lalu
 *                   battle.serang(...) / battle.gunakanJurus(...)
 * </pre>
 *
 * <p>Semua field kelas ini hanya disentuh render thread, jadi tidak perlu {@code synchronized}.
 * Satu-satunya titik temu antar-thread adalah {@code keThreadUtama}, yang memang dibuat
 * thread-safe oleh LibGDX.
 *
 * <h2>Pemakaian di LibGDX</h2>
 * <pre>
 *  PengendaliMusuh ai = new PengendaliMusuh(battle, new StrategiDasar(), Gdx.app::postRunnable);
 *  ai.setTungguIzinFrontEnd(true);   // opsional: AI menunggu animasi dan dialog selesai
 *  ai.aktifkan();
 *  battle.mulai();
 *  ...
 *  // saat giliran musuh, setelah animasi aksi sebelumnya selesai dan dialog kosong:
 *  ai.izinkanBertindak();
 *  ...
 *  // di Screen.hide() / dispose():
 *  ai.hentikan();
 * </pre>
 */
public class PengendaliMusuh implements BattleListener {

    public static final long JEDA_BIASA_MS = 900;
    public static final long JEDA_BOS_MS = 1800;

    private final BattleManager battle;
    private final StrategiMusuh strategi;
    private final Executor keThreadUtama;
    private final long jedaBiasaMs;
    private final long jedaBosMs;

    private Thread threadAktif;
    private long nomorGiliran;
    private boolean dihentikan;

    private boolean tungguIzinFrontEnd;
    private boolean izinDiberikan;
    private KeputusanTertunda tertunda;

    /** Keputusan AI yang sudah siap tetapi belum diizinkan front end. */
    private static final class KeputusanTertunda {
        final long nomor;
        final KarakterMitos aktor;
        final KeputusanAksi keputusan;

        KeputusanTertunda(long nomor, KarakterMitos aktor, KeputusanAksi keputusan) {
            this.nomor = nomor;
            this.aktor = aktor;
            this.keputusan = keputusan;
        }
    }

    public PengendaliMusuh(BattleManager battle, StrategiMusuh strategi, Executor keThreadUtama) {
        this(battle, strategi, keThreadUtama, JEDA_BIASA_MS, JEDA_BOS_MS);
    }

    /**
     * @param keThreadUtama cara mengirim pekerjaan ke render thread; di LibGDX {@code Gdx.app::postRunnable}
     * @param jedaBiasaMs   lama "berpikir" makhluk biasa
     * @param jedaBosMs     lama "berpikir" bos ({@link MakhlukGirah#isBos()}), lebih panjang agar ucapannya sempat dibaca
     */
    public PengendaliMusuh(BattleManager battle, StrategiMusuh strategi, Executor keThreadUtama,
                           long jedaBiasaMs, long jedaBosMs) {
        this.battle = Objects.requireNonNull(battle, "Battle tidak boleh null");
        this.strategi = Objects.requireNonNull(strategi, "Strategi tidak boleh null");
        this.keThreadUtama = Objects.requireNonNull(keThreadUtama, "Executor tidak boleh null");
        if (jedaBiasaMs < 0 || jedaBosMs < 0) {
            throw new IllegalArgumentException("Jeda tidak boleh negatif");
        }
        this.jedaBiasaMs = jedaBiasaMs;
        this.jedaBosMs = jedaBosMs;
    }

    /** Mendaftar ke BattleManager. Panggil sebelum {@code battle.mulai()}. */
    public void aktifkan() {
        battle.tambahListener(this);
    }

    /** Melepas dari BattleManager dan membatalkan thread yang sedang berpikir. Aman dipanggil berulang. */
    public void hentikan() {
        dihentikan = true;
        battle.hapusListener(this);
        batalkanThread();
        tertunda = null;
    }

    /** {@code true} selama thread AI sedang menunggu atau berpikir. UI bisa menampilkan "...". */
    public boolean isSedangBerpikir() {
        return threadAktif != null && threadAktif.isAlive();
    }

    /**
     * Jika {@code true}, keputusan AI baru dijalankan setelah front end memanggil
     * {@link #izinkanBertindak()}, sehingga aksi musuh tidak menimpa animasi atau dialog yang
     * masih diputar. Bawaan {@code false}: aksi langsung dijalankan setelah jeda berpikir.
     */
    public void setTungguIzinFrontEnd(boolean tunggu) {
        this.tungguIzinFrontEnd = tunggu;
    }

    public boolean isTungguIzinFrontEnd() {
        return tungguIzinFrontEnd;
    }

    /**
     * Dipanggil front end (di render thread) setelah selesai menampilkan aksi sebelumnya
     * (animasi dan antrean dialog kosong). Berlaku untuk giliran musuh yang sedang berjalan saja:
     * <ul>
     *   <li>jika keputusan AI sudah siap, aksi langsung dijalankan;</li>
     *   <li>jika AI masih berpikir, aksi dijalankan begitu keputusannya datang.</li>
     * </ul>
     * Izin hangus saat giliran musuh berikutnya dimulai, jadi front end memanggilnya sekali
     * untuk setiap giliran musuh. Tidak berpengaruh jika mode tunggu izin tidak aktif.
     */
    public void izinkanBertindak() {
        if (!tungguIzinFrontEnd || dihentikan) {
            return;
        }
        izinDiberikan = true;
        if (tertunda != null) {
            KeputusanTertunda t = tertunda;
            tertunda = null;
            jalankanKeputusan(t.nomor, t.aktor, t.keputusan);
        }
    }

    /** {@code true} jika keputusan AI sudah siap dan hanya menunggu {@link #izinkanBertindak()}. */
    public boolean isMenungguIzin() {
        return tertunda != null;
    }

    // ================================================================
    // BattleListener (dipanggil di render thread)
    // ================================================================

    @Override
    public void onGiliranBaru(KarakterMitos aktor, StatusBattle status) {
        if (status == StatusBattle.GILIRAN_MUSUH && !dihentikan) {
            izinDiberikan = false;      // izin giliran sebelumnya tidak terbawa
            tertunda = null;
            mulaiBerpikir(aktor);
        }
    }

    @Override
    public void onBattleSelesai(StatusBattle hasil) {
        batalkanThread();
    }

    // ================================================================

    private void mulaiBerpikir(KarakterMitos aktor) {
        batalkanThread();
        long nomor = ++nomorGiliran;
        List<KarakterMitos> sasaranSerang = battle.getSasaranSerang();
        List<KarakterMitos> sasaranJurus = battle.getSasaranJurus();

        PikiranMusuh pikiran = new PikiranMusuh(aktor, sasaranSerang, sasaranJurus, strategi, jedaUntuk(aktor),
                keputusan -> keThreadUtama.execute(() -> jalankanKeputusan(nomor, aktor, keputusan)));

        threadAktif = new Thread(pikiran, "AI-" + aktor.getNama());
        threadAktif.setDaemon(true);   // tidak menahan JVM tetap hidup saat game ditutup
        threadAktif.start();
    }

    /** Dijalankan di render thread. Keputusan yang sudah basi diabaikan. */
    private void jalankanKeputusan(long nomor, KarakterMitos aktor, KeputusanAksi keputusan) {
        if (dihentikan || nomor != nomorGiliran
                || battle.getStatus() != StatusBattle.GILIRAN_MUSUH || battle.getAktor() != aktor) {
            return;
        }
        threadAktif = null;
        if (tungguIzinFrontEnd && !izinDiberikan) {
            tertunda = new KeputusanTertunda(nomor, aktor, keputusan);
            return;
        }
        try {
            if (keputusan.getJenis() == KeputusanAksi.Jenis.JURUS) {
                battle.gunakanJurus(keputusan.getTarget());
            } else {
                battle.serang(keputusan.getTarget());
            }
        } catch (EnergiBatinHabisException | IllegalArgumentException e) {
            // Energi kurang atau sasaran tidak lagi sah: musuh tetap bertindak dengan serangan biasa.
            battle.serang(battle.getSasaranSerang().get(0));
        }
    }

    private long jedaUntuk(KarakterMitos aktor) {
        return aktor instanceof MakhlukGirah && ((MakhlukGirah) aktor).isBos() ? jedaBosMs : jedaBiasaMs;
    }

    private void batalkanThread() {
        if (threadAktif != null) {
            threadAktif.interrupt();
            threadAktif = null;
        }
    }
}
