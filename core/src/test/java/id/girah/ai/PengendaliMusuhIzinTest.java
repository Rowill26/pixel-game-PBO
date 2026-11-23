package id.girah.ai;

import static org.junit.jupiter.api.Assertions.*;

import id.girah.battle.BattleManager;
import id.girah.battle.StatusBattle;
import id.girah.karakter.CalonArang;
import id.girah.karakter.Senopati;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Mode "tunggu izin front end" (catatan revisi B4). Thread uji berperan sebagai render thread. */
@Timeout(10)
class PengendaliMusuhIzinTest {

    private final BlockingQueue<Runnable> renderThread = new LinkedBlockingQueue<>();
    private Senopati senopati;
    private CalonArang calonArang;
    private BattleManager battle;
    private PengendaliMusuh ai;

    @BeforeEach
    void siapkan() {
        senopati = new Senopati("Arya");
        calonArang = new CalonArang();                      // kecepatan 12, mendahului Senopati 10
        battle = new BattleManager(List.of(senopati), List.of(calonArang));
        ai = new PengendaliMusuh(battle, new StrategiDasar(), renderThread::add, 0, 0);
        ai.setTungguIzinFrontEnd(true);
        ai.aktifkan();
    }

    @AfterEach
    void bersihkan() {
        ai.hentikan();
    }

    private void terimaKeputusan() throws InterruptedException {
        Runnable kiriman = renderThread.poll(3, TimeUnit.SECONDS);
        assertNotNull(kiriman, "Thread AI tidak mengirim keputusan");
        kiriman.run();
    }

    @Test
    @DisplayName("Keputusan yang datang ditahan sampai front end memberi izin")
    void ditahanSampaiIzin() throws InterruptedException {
        battle.mulai();
        terimaKeputusan();

        assertTrue(ai.isMenungguIzin());
        assertSame(calonArang, battle.getAktor());
        assertEquals(Senopati.NYAWA_AWAL, senopati.getNyawa(), "Belum ada aksi");

        ai.izinkanBertindak();

        assertFalse(ai.isMenungguIzin());
        assertEquals(108, senopati.getNyawa());
        assertSame(senopati, battle.getAktor());
    }

    @Test
    @DisplayName("Izin yang diberikan selagi AI berpikir: aksi langsung jalan saat keputusan datang")
    void izinSebelumKeputusan() throws InterruptedException {
        battle.mulai();
        ai.izinkanBertindak();

        terimaKeputusan();

        assertFalse(ai.isMenungguIzin());
        assertEquals(108, senopati.getNyawa());
    }

    @Test
    @DisplayName("Izin tidak terbawa ke giliran musuh berikutnya")
    void izinTidakTerbawa() throws InterruptedException {
        battle.mulai();
        ai.izinkanBertindak();
        terimaKeputusan();                                  // ronde 1: Calon Arang bertindak
        ai.izinkanBertindak();                              // izin "berlebih" saat giliran pemain

        battle.serang(calonArang);                          // ronde 2: giliran Calon Arang lagi
        terimaKeputusan();

        assertTrue(ai.isMenungguIzin(), "Butuh izin baru untuk giliran ini");
        assertEquals(108, senopati.getNyawa());
        ai.izinkanBertindak();
        assertEquals(96, senopati.getNyawa());
    }

    @Test
    void hentikanMembuangKeputusanTertunda() throws InterruptedException {
        battle.mulai();
        terimaKeputusan();
        assertTrue(ai.isMenungguIzin());

        ai.hentikan();
        ai.izinkanBertindak();

        assertFalse(ai.isMenungguIzin());
        assertEquals(Senopati.NYAWA_AWAL, senopati.getNyawa());
        assertEquals(StatusBattle.GILIRAN_MUSUH, battle.getStatus());
    }

    @Test
    @DisplayName("Mode bawaan tidak menunggu izin")
    void bawaanTidakMenunggu() throws InterruptedException {
        PengendaliMusuh biasa = new PengendaliMusuh(battle, new StrategiDasar(), renderThread::add, 0, 0);
        assertFalse(biasa.isTungguIzinFrontEnd());
        ai.hentikan();
        biasa.aktifkan();
        battle.mulai();

        terimaKeputusan();

        assertFalse(biasa.isMenungguIzin());
        assertEquals(108, senopati.getNyawa());
        biasa.hentikan();
    }
}
