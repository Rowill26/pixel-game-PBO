package id.girah.ai;

import static org.junit.jupiter.api.Assertions.*;

import id.girah.battle.BattleManager;
import id.girah.battle.StatusBattle;
import id.girah.karakter.CalonArang;
import id.girah.karakter.Leak;
import id.girah.karakter.MakhlukGirah;
import id.girah.karakter.Senopati;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Thread uji ini berperan sebagai render thread LibGDX: pekerjaan yang dikirim thread AI
 * masuk ke {@code renderThread} dan baru dijalankan saat uji mengambilnya.
 */
@Timeout(10)
class PengendaliMusuhTest {

    private static final long JEDA_UJI_MS = 50;

    private final BlockingQueue<Runnable> renderThread = new LinkedBlockingQueue<>();
    private Senopati senopati;
    private PengendaliMusuh ai;

    @BeforeEach
    void siapkan() {
        senopati = new Senopati("Arya");
    }

    @AfterEach
    void bersihkan() {
        if (ai != null) {
            ai.hentikan();
        }
    }

    private BattleManager siapkanBattle(MakhlukGirah musuh, StrategiMusuh strategi, long jedaMs) {
        BattleManager battle = new BattleManager(List.of(senopati), List.of(musuh));
        ai = new PengendaliMusuh(battle, strategi, renderThread::add, jedaMs, jedaMs);
        ai.aktifkan();
        battle.mulai();
        return battle;
    }

    /** Menunggu kiriman dari thread AI lalu menjalankannya di thread uji. */
    private void jalankanKirimanBerikutnya() throws InterruptedException {
        Runnable kiriman = renderThread.poll(3, TimeUnit.SECONDS);
        assertNotNull(kiriman, "Thread AI tidak mengirim keputusan");
        kiriman.run();
    }

    @Test
    @DisplayName("Giliran musuh: thread AI menunggu, lalu aksi dijalankan di render thread")
    void giliranMusuhDijalankan() throws InterruptedException {
        CalonArang calonArang = new CalonArang();            // kecepatan 12, mendahului Senopati 10
        BattleManager battle = siapkanBattle(calonArang, new StrategiDasar(), JEDA_UJI_MS);

        assertSame(calonArang, battle.getAktor(), "Belum ada yang berubah sebelum kiriman dijalankan");
        assertEquals(Senopati.NYAWA_AWAL, senopati.getNyawa());

        jalankanKirimanBerikutnya();

        assertSame(senopati, battle.getAktor());
        assertEquals(StatusBattle.GILIRAN_PEMAIN, battle.getStatus());
        assertEquals(108, senopati.getNyawa(), "Tinggal satu lawan: serangan biasa 20 - 8 = 12");
    }

    @Test
    @DisplayName("Strategi berjalan di thread lain, setelah jeda")
    void berpikirDiThreadLainSetelahJeda() throws InterruptedException {
        AtomicReference<Thread> threadStrategi = new AtomicReference<>();
        StrategiMusuh perekam = (aktor, serang, jurus) -> {
            threadStrategi.set(Thread.currentThread());
            return KeputusanAksi.serang(serang.get(0));
        };

        long mulai = System.nanoTime();
        siapkanBattle(new CalonArang(), perekam, 200);
        Runnable kiriman = renderThread.poll(3, TimeUnit.SECONDS);
        long lamaMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - mulai);

        assertNotNull(kiriman);
        assertTrue(lamaMs >= 200, "Keputusan datang setelah " + lamaMs + " ms, seharusnya >= 200");
        assertNotSame(Thread.currentThread(), threadStrategi.get());
        assertEquals("AI-Calon Arang", threadStrategi.get().getName());
        assertTrue(threadStrategi.get().isDaemon());
    }

    @Test
    void giliranPemainTidakMemulaiThread() throws InterruptedException {
        BattleManager battle = siapkanBattle(new Leak("Leak"), new StrategiDasar(), 0);  // Senopati duluan
        assertEquals(StatusBattle.GILIRAN_PEMAIN, battle.getStatus());
        assertFalse(ai.isSedangBerpikir());
        assertNull(renderThread.poll(200, TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("hentikan() saat AI sedang berpikir: thread berhenti dan tidak ada aksi")
    void hentikanSaatBerpikir() throws InterruptedException {
        BattleManager battle = siapkanBattle(new CalonArang(), new StrategiDasar(), 5_000);
        assertTrue(ai.isSedangBerpikir());

        ai.hentikan();

        assertNull(renderThread.poll(300, TimeUnit.MILLISECONDS));
        assertEquals(Senopati.NYAWA_AWAL, senopati.getNyawa());
        assertEquals(StatusBattle.GILIRAN_MUSUH, battle.getStatus());
    }

    @Test
    @DisplayName("Kiriman yang sudah terlanjur antre diabaikan setelah hentikan()")
    void kirimanBasiDiabaikan() throws InterruptedException {
        siapkanBattle(new CalonArang(), new StrategiDasar(), 0);
        Runnable kiriman = renderThread.poll(3, TimeUnit.SECONDS);
        assertNotNull(kiriman);

        ai.hentikan();
        kiriman.run();

        assertEquals(Senopati.NYAWA_AWAL, senopati.getNyawa());
    }

    @Test
    @DisplayName("Energi batin habis di tengah pertarungan: try-catch membuat musuh menyerang biasa")
    void energiHabisJatuhKeSeranganBiasa() throws InterruptedException {
        Leak leak = new Leak("Leak Setra");
        StrategiMusuh selaluJurus = (aktor, serang, jurus) -> KeputusanAksi.jurus(jurus.get(0));
        BattleManager battle = siapkanBattle(leak, selaluJurus, 0);   // Senopati 10, Leak 8

        for (int giliran = 0; giliran < 3; giliran++) {
            battle.serang(leak);                                       // pemain: 12 kerusakan
            jalankanKirimanBerikutnya();                               // AI: Jerat Kabut, lalu energi habis
        }

        assertEquals(0, leak.getEnergiBatin(), "20 - 10 - 10 = 0");
        assertEquals(120 - 18 - 18 - 5, senopati.getNyawa(), "Dua Jerat Kabut (18) lalu serangan biasa (5)");
        assertEquals(StatusBattle.GILIRAN_PEMAIN, battle.getStatus());
    }

    @Test
    @DisplayName("Sasaran yang tidak sah dari strategi: musuh tetap bertindak")
    void sasaranTidakSahTetapBertindak() throws InterruptedException {
        StrategiMusuh menyerangDiriSendiri = (aktor, serang, jurus) -> KeputusanAksi.serang(aktor);
        BattleManager battle = siapkanBattle(new CalonArang(), menyerangDiriSendiri, 0);

        jalankanKirimanBerikutnya();

        assertEquals(108, senopati.getNyawa());
        assertSame(senopati, battle.getAktor());
    }

    @Test
    @DisplayName("Integrasi: pemain dan AI bergantian sampai pertarungan selesai")
    void sampaiSelesai() throws InterruptedException {
        Leak leak = new Leak("Leak Setra");
        BattleManager battle = siapkanBattle(leak, new StrategiDasar(), 0);

        while (!battle.getStatus().isSelesai()) {
            if (battle.getStatus() == StatusBattle.GILIRAN_PEMAIN) {
                battle.serang(leak);
            } else {
                jalankanKirimanBerikutnya();
            }
        }

        assertEquals(StatusBattle.MENANG, battle.getStatus());
        assertFalse(ai.isSedangBerpikir());
        assertTrue(battle.getBabad().getIsi().contains("memenangkan pertarungan"));
    }
}
