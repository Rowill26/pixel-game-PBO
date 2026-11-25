package id.girah.battle;

import static org.junit.jupiter.api.Assertions.*;

import id.girah.karakter.Balian;
import id.girah.karakter.CalonArang;
import id.girah.karakter.EnergiBatinHabisException;
import id.girah.karakter.JarakAksi;
import id.girah.karakter.KarakterMitos;
import id.girah.karakter.Leak;
import id.girah.karakter.Senopati;
import id.girah.karakter.TelikSandi;
import id.girah.karakter.Wiku;
import id.girah.pusaka.TasPusaka;
import id.girah.pusaka.TirtaSuci;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Event untuk front end: onAksi beserta HasilAksi (catatan revisi B1, B2). */
class BattleEventTest {

    private Senopati senopati;
    private Wiku wiku;
    private TelikSandi telikSandi;
    private Balian balian;
    private Leak leak;
    private CalonArang calonArang;
    private Perekam perekam;

    @BeforeEach
    void siapkan() {
        senopati = new Senopati("Arya");
        wiku = new Wiku("Dang Hyang");
        telikSandi = new TelikSandi("Kebo Ijo");
        balian = new Balian("Jero Balian");
        leak = new Leak("Leak Setra");
        calonArang = new CalonArang();
        perekam = new Perekam();
    }

    /** Urutan: Telik Sandi 18, Calon Arang 12, Balian 11, Senopati 10, Wiku 9, Leak 8. */
    private BattleManager pertarunganPenuh() {
        BattleManager b = new BattleManager(List.of(senopati, wiku, telikSandi, balian), List.of(leak, calonArang));
        b.tambahListener(perekam);
        b.mulai();
        perekam.reset();
        return b;
    }

    @Test
    @DisplayName("Serangan: siapa, apa, ke siapa, jarak, dan kerusakannya")
    void serangan() {
        BattleManager b = pertarunganPenuh();
        b.serang(leak);

        HasilAksi h = perekam.satuSatunyaAksi();
        assertSame(telikSandi, h.getAktor());
        assertEquals(JenisAksi.SERANG, h.getJenis());
        assertEquals("Serangan", h.getNamaAksi());
        assertEquals(JarakAksi.DEKAT, h.getJarak());
        assertEquals(List.of(leak), h.getSasaran());

        assertEquals(1, h.getPerubahan().size(), "Hanya Leak yang berubah");
        PerubahanStatus p = h.getPerubahan(leak).orElseThrow();
        assertEquals(-9, p.getPerubahanNyawa());
        assertEquals(9, p.getKerusakan());
        assertEquals(0, p.getPemulihanNyawa());
        assertEquals(0, p.getPerubahanEnergiBatin());
        assertFalse(p.isGugur());
        assertTrue(h.getPerubahan(senopati).isEmpty());
        assertTrue(b.getBabad().getIsi().contains(h.getNarasi()), "Narasi yang sama masuk ke Babad");
    }

    @Test
    @DisplayName("Urutan event: onAksi, onNarasi, lalu onGiliranBaru")
    void urutanEvent() {
        BattleManager b = pertarunganPenuh();
        b.serang(leak);
        assertEquals(List.of("aksi", "narasi", "giliran"), perekam.urutan);
    }

    @Test
    @DisplayName("Jurus area: semua pengelana terluka dan energi Calon Arang terpakai")
    void jurusArea() throws EnergiBatinHabisException {
        BattleManager b = pertarunganPenuh();
        b.serang(leak);                                     // Telik Sandi
        perekam.reset();

        b.gunakanJurus(null);                               // Calon Arang: Sebar Wabah

        HasilAksi h = perekam.satuSatunyaAksi();
        assertSame(calonArang, h.getAktor());
        assertEquals(JenisAksi.JURUS, h.getJenis());
        assertEquals("Sebar Wabah", h.getNamaAksi());
        assertEquals(JarakAksi.JAUH, h.getJarak());
        assertEquals(List.of(senopati, wiku, telikSandi, balian), h.getSasaran());

        List<KarakterMitos> urutanPerubahan = new ArrayList<>();
        for (PerubahanStatus p : h.getPerubahan()) {
            urutanPerubahan.add(p.getKarakter());
        }
        assertEquals(List.of(senopati, wiku, telikSandi, balian, calonArang), urutanPerubahan,
                "Urut kelompok lalu lawan; aktor ikut karena energinya terpakai");
        assertEquals(14, h.getPerubahan(senopati).orElseThrow().getKerusakan());
        assertEquals(18, h.getPerubahan(wiku).orElseThrow().getKerusakan());
        assertEquals(17, h.getPerubahan(telikSandi).orElseThrow().getKerusakan());
        assertEquals(16, h.getPerubahan(balian).orElseThrow().getKerusakan());
        PerubahanStatus aktor = h.getPerubahan(calonArang).orElseThrow();
        assertEquals(-CalonArang.BIAYA_JURUS, aktor.getPerubahanEnergiBatin());
        assertEquals(0, aktor.getPerubahanNyawa());
    }

    @Test
    @DisplayName("Langkah Bayangan: jarak DEKAT, lalu giliran kembali ke Telik Sandi")
    void langkahBayangan() throws EnergiBatinHabisException {
        BattleManager b = pertarunganPenuh();
        b.gunakanJurus(calonArang);

        HasilAksi h = perekam.satuSatunyaAksi();
        assertEquals(JarakAksi.DEKAT, h.getJarak());
        assertEquals(TelikSandi.KEKUATAN_AWAL - CalonArang.PERTAHANAN_AWAL,
                h.getPerubahan(calonArang).orElseThrow().getKerusakan());
        assertEquals(-TelikSandi.BIAYA_JURUS, h.getPerubahan(telikSandi).orElseThrow().getPerubahanEnergiBatin());
        assertEquals(List.of(telikSandi), perekam.aktorGiliran);
    }

    @Test
    @DisplayName("Pukulan terakhir: gugur tercatat, lalu onBattleSelesai tanpa giliran baru")
    void gugurDanSelesai() {
        leak.terimaSerangan(62);                            // -> nyawa 1
        BattleManager b = new BattleManager(List.of(senopati), List.of(leak));
        b.tambahListener(perekam);
        b.mulai();
        perekam.reset();

        b.serang(leak);

        HasilAksi h = perekam.satuSatunyaAksi();
        PerubahanStatus p = h.getPerubahan(leak).orElseThrow();
        assertTrue(p.isGugur());
        assertEquals(1, p.getKerusakan(), "Kerusakan sebenarnya, bukan 12");
        assertEquals(List.of(leak), h.getYangGugur());
        assertEquals(List.of("aksi", "narasi", "narasi", "selesai"), perekam.urutan,
                "Narasi aksi, lalu narasi kemenangan");
    }

    @Test
    @DisplayName("Pusaka: jenis PUSAKA, nama pusaka, dan nyawa yang pulih")
    void pusaka() {
        TasPusaka tas = new TasPusaka();
        tas.tambah(new TirtaSuci(), 1);
        BattleManager b = new BattleManager(List.of(senopati), List.of(calonArang), BattleManager.TANPA_BATAS_RONDE, tas);
        b.tambahListener(perekam);
        b.mulai();                                          // Calon Arang 12, Senopati 10
        b.serang(senopati);                                 // 12 kerusakan
        perekam.reset();

        b.gunakanPusaka(new TirtaSuci(), senopati);

        HasilAksi h = perekam.satuSatunyaAksi();
        assertEquals(JenisAksi.PUSAKA, h.getJenis());
        assertEquals("Tirta Suci", h.getNamaAksi());
        assertEquals(JarakAksi.JAUH, h.getJarak());
        assertEquals(12, h.getPerubahan(senopati).orElseThrow().getPemulihanNyawa());
    }

    @Test
    @DisplayName("Aksi yang ditolak tidak memicu event apa pun")
    void aksiDitolakTanpaEvent() throws EnergiBatinHabisException {
        BattleManager b = new BattleManager(List.of(senopati), List.of(leak));
        b.tambahListener(perekam);
        b.mulai();                                          // Senopati 10, Leak 8

        for (int i = 0; i < 2; i++) {                       // Leak: 20 - 10 - 10 = 0
            b.serang(leak);
            b.gunakanJurus(senopati);
        }
        b.serang(leak);
        perekam.reset();

        assertThrows(EnergiBatinHabisException.class, () -> b.gunakanJurus(senopati));
        assertThrows(IllegalArgumentException.class, () -> b.serang(leak), "Leak menyerang dirinya sendiri");
        assertEquals(List.of(), perekam.urutan);
    }

    @Test
    void hasilAksiTidakBisaDiubah() {
        BattleManager b = pertarunganPenuh();
        b.serang(leak);
        HasilAksi h = perekam.satuSatunyaAksi();
        assertThrows(UnsupportedOperationException.class, () -> h.getSasaran().clear());
        assertThrows(UnsupportedOperationException.class, () -> h.getPerubahan().clear());
    }

    static class Perekam implements BattleListener {
        final List<String> urutan = new ArrayList<>();
        final List<HasilAksi> aksi = new ArrayList<>();
        final List<KarakterMitos> aktorGiliran = new ArrayList<>();

        void reset() {
            urutan.clear();
            aksi.clear();
            aktorGiliran.clear();
        }

        HasilAksi satuSatunyaAksi() {
            assertEquals(1, aksi.size(), "Seharusnya tepat satu onAksi");
            return aksi.get(0);
        }

        @Override public void onAksi(HasilAksi h) { urutan.add("aksi"); aksi.add(h); }
        @Override public void onNarasi(String n) { urutan.add("narasi"); }
        @Override public void onGiliranBaru(KarakterMitos a, StatusBattle s) { urutan.add("giliran"); aktorGiliran.add(a); }
        @Override public void onRondeBaru(int r) { urutan.add("ronde"); }
        @Override public void onBattleSelesai(StatusBattle h) { urutan.add("selesai"); }
    }
}
