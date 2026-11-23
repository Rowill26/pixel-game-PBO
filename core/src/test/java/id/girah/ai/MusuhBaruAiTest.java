package id.girah.ai;

import static org.junit.jupiter.api.Assertions.*;

import id.girah.battle.BattleManager;
import id.girah.battle.StatusBattle;
import id.girah.karakter.Balian;
import id.girah.karakter.PemimpinPengikut;
import id.girah.karakter.Pengikut;
import id.girah.karakter.Senopati;
import id.girah.karakter.TelikSandi;
import id.girah.karakter.Wiku;
import id.girah.pusaka.TasPusaka;
import id.girah.pusaka.TirtaSuci;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** AI dan BattleManager dengan musuh baru (catatan revisi D1). */
@Timeout(20)
class MusuhBaruAiTest {

    private final BlockingQueue<Runnable> renderThread = new LinkedBlockingQueue<>();
    private PengendaliMusuh ai;

    @AfterEach
    void bersihkan() {
        if (ai != null) {
            ai.hentikan();
        }
    }

    private long lamaSampaiKeputusanMs(BattleManager battle, long jedaBiasa, long jedaBos) throws InterruptedException {
        ai = new PengendaliMusuh(battle, new StrategiDasar(), renderThread::add, jedaBiasa, jedaBos);
        ai.aktifkan();
        long mulai = System.nanoTime();
        battle.mulai();
        assertNotNull(renderThread.poll(5, TimeUnit.SECONDS), "Thread AI tidak mengirim keputusan");
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - mulai);
    }

    @Test
    @DisplayName("Pemimpin Pengikut adalah bos: memakai jeda bos")
    void pemimpinMemakaiJedaBos() throws InterruptedException {
        // Pemimpin 11 lebih cepat dari Wiku 9, jadi giliran pertama milik Pemimpin
        BattleManager battle = new BattleManager(List.of(new Wiku("Dang Hyang")), List.of(new PemimpinPengikut()));
        long lama = lamaSampaiKeputusanMs(battle, 0, 300);
        assertTrue(lama >= 300, "Keputusan datang setelah " + lama + " ms, seharusnya >= 300");
    }

    @Test
    @DisplayName("Pengikut bukan bos: memakai jeda biasa")
    void pengikutMemakaiJedaBiasa() throws InterruptedException {
        // Kecepatan Wiku dan Pengikut sama-sama 9; jika seri, pengelana didahulukan.
        Wiku wiku = new Wiku("Dang Hyang");
        Pengikut pengikut = new Pengikut("Pengikut Girah");
        BattleManager battle = new BattleManager(List.of(wiku), List.of(pengikut));
        ai = new PengendaliMusuh(battle, new StrategiDasar(), renderThread::add, 0, 10_000);
        ai.aktifkan();
        battle.mulai();
        assertSame(wiku, battle.getAktor());

        battle.serang(pengikut);                            // giliran berpindah ke Pengikut
        assertNotNull(renderThread.poll(3, TimeUnit.SECONDS),
                "Jeda biasa (0 ms) yang dipakai, bukan jeda bos (10 detik)");
    }

    @Test
    @DisplayName("Integrasi Babak 4: kelompok penuh vs Pemimpin + dua Pengikut sampai selesai")
    void pertarunganBabak4SampaiSelesai() throws InterruptedException {
        TasPusaka tas = new TasPusaka();
        tas.tambah(new TirtaSuci(), 2);
        BattleManager battle = new BattleManager(
                List.of(new Senopati("Arya"), new Wiku("Dang Hyang"), new TelikSandi("Kebo Ijo"), new Balian("Jero Balian")),
                List.of(new PemimpinPengikut(), new Pengikut("Pengikut Utara"), new Pengikut("Pengikut Selatan")),
                BattleManager.TANPA_BATAS_RONDE, tas);
        ai = new PengendaliMusuh(battle, new StrategiDasar(new Random(7)), renderThread::add, 0, 0);
        ai.aktifkan();
        battle.mulai();

        for (int langkah = 0; !battle.getStatus().isSelesai(); langkah++) {
            assertTrue(langkah < 500, "Pertarungan tidak selesai");
            if (battle.getStatus() == StatusBattle.GILIRAN_PEMAIN) {
                battle.serang(battle.getSasaranSerang().get(0));
            } else {
                Runnable kiriman = renderThread.poll(3, TimeUnit.SECONDS);
                assertNotNull(kiriman, "AI macet di giliran " + battle.getAktor().getNama());
                kiriman.run();
            }
        }

        assertTrue(battle.getStatus() == StatusBattle.MENANG || battle.getStatus() == StatusBattle.KALAH);
        String babad = battle.getBabad().getIsi();
        assertTrue(babad.contains("Pemimpin Pengikut"), "Pemimpin ikut bertindak");
        assertTrue(babad.contains("Pengikut Utara"), "Pengikut ikut bertindak");
    }
}
