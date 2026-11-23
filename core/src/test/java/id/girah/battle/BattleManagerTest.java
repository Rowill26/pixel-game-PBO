package id.girah.battle;

import static org.junit.jupiter.api.Assertions.*;

import id.girah.karakter.Balian;
import id.girah.karakter.CalonArang;
import id.girah.karakter.EnergiBatinHabisException;
import id.girah.karakter.JenisSasaran;
import id.girah.karakter.KarakterMitos;
import id.girah.karakter.Leak;
import id.girah.karakter.MakhlukGirah;
import id.girah.karakter.Senopati;
import id.girah.karakter.TelikSandi;
import id.girah.karakter.Wiku;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BattleManagerTest {

    private Senopati senopati;
    private Wiku wiku;
    private TelikSandi telikSandi;
    private Balian balian;
    private Leak leak;
    private CalonArang calonArang;

    @BeforeEach
    void siapkan() {
        senopati = new Senopati("Arya");
        wiku = new Wiku("Dang Hyang");
        telikSandi = new TelikSandi("Kebo Ijo");
        balian = new Balian("Jero Balian");
        leak = new Leak("Leak Setra");
        calonArang = new CalonArang();
    }

    private BattleManager pertarunganPenuh() {
        BattleManager b = new BattleManager(List.of(senopati, wiku, telikSandi, balian), List.of(leak, calonArang));
        b.mulai();
        return b;
    }

    /** Semua aktor lain menyerang sasaran pertama yang sah sampai giliran {@code tujuan} tiba. */
    private static void lewatiSampai(BattleManager b, KarakterMitos tujuan) {
        for (int langkah = 0; b.getAktor() != tujuan; langkah++) {
            assertTrue(langkah < 50, "Giliran " + tujuan.getNama() + " tidak pernah tiba");
            b.serang(b.getSasaranSerang().get(0));
        }
    }

    // ================================================================

    @Nested
    class UrutanGiliran {

        @Test
        @DisplayName("Tersusun dari yang tercepat: Telik Sandi 18 ... Leak 8")
        void urutBerdasarkanKecepatan() {
            BattleManager b = pertarunganPenuh();
            assertEquals(List.of(telikSandi, calonArang, balian, senopati, wiku, leak), b.getAntrianGiliran());
            assertEquals(1, b.getRonde());
            assertSame(telikSandi, b.getAktor());
            assertEquals(StatusBattle.GILIRAN_PEMAIN, b.getStatus());
        }

        @Test
        void giliranMajuDanStatusMengikutiFaksi() {
            BattleManager b = pertarunganPenuh();
            b.serang(leak);
            assertSame(calonArang, b.getAktor());
            assertEquals(StatusBattle.GILIRAN_MUSUH, b.getStatus());
        }

        @Test
        @DisplayName("Kecepatan seri: pengelana didahulukan")
        void seriPengelanaDulu() {
            MakhlukUji secepatSenopati = new MakhlukUji("Bayangan", Senopati.KECEPATAN_AWAL);
            BattleManager b = new BattleManager(List.of(senopati), List.of(secepatSenopati));
            b.mulai();
            assertEquals(List.of(senopati, secepatSenopati), b.getAntrianGiliran());
        }

        @Test
        @DisplayName("Setelah semua bertindak, ronde baru dimulai dengan urutan yang sama")
        void rondeBaru() {
            BattleManager b = pertarunganPenuh();
            for (int i = 0; i < 6; i++) {
                b.serang(b.getSasaranSerang().get(0));
            }
            assertEquals(2, b.getRonde());
            assertSame(telikSandi, b.getAktor());
        }

        @Test
        @DisplayName("Karakter yang gugur di tengah ronde dilewati dan tidak masuk ronde berikutnya")
        void yangGugurDilewati() {
            Leak lemah = new Leak("Leak Lemah");
            lemah.terimaSerangan(62);                       // 59 kerusakan -> nyawa 1
            BattleManager b = new BattleManager(List.of(senopati), List.of(lemah, leak));
            b.mulai();                                      // Senopati 10, Leak Lemah 8, Leak Setra 8

            b.serang(lemah);
            assertSame(leak, b.getAktor(), "Leak Lemah sudah gugur, langsung ke Leak Setra");

            b.serang(senopati);
            assertEquals(2, b.getRonde());
            assertEquals(List.of(senopati, leak), b.getAntrianGiliran());
        }
    }

    // ================================================================

    @Nested
    class GiliranTambahan {

        @Test
        @DisplayName("Langkah Bayangan menyisipkan Telik Sandi tepat setelah gilirannya sendiri")
        void langkahBayangan() throws EnergiBatinHabisException {
            BattleManager b = pertarunganPenuh();
            b.gunakanJurus(calonArang);

            assertSame(telikSandi, b.getAktor());
            assertEquals(List.of(telikSandi, calonArang, balian, senopati, wiku, leak), b.getAntrianGiliran());

            b.serang(leak);
            assertSame(calonArang, b.getAktor());
        }

        @Test
        @DisplayName("Ronde berikutnya kembali normal dan jurusnya bisa dipakai lagi")
        void ronde2Normal() throws EnergiBatinHabisException {
            BattleManager b = pertarunganPenuh();
            b.gunakanJurus(calonArang);
            b.serang(leak);                                 // memakai giliran tambahan
            lewatiSampai(b, telikSandi);                    // sisa ronde 1 habis, masuk ronde 2

            assertEquals(2, b.getRonde());
            assertEquals(6, b.getAntrianGiliran().size());
            assertTrue(telikSandi.bisaMemakaiJurus());
        }
    }

    // ================================================================

    @Nested
    class Jurus {

        @Test
        @DisplayName("Energi batin habis: exception keluar, giliran TIDAK habis")
        void energiHabisTidakMenghabiskanGiliran() throws EnergiBatinHabisException {
            BattleManager b = pertarunganPenuh();
            for (int i = 0; i < 2; i++) {                   // 50 - 20 - 20 = 10
                lewatiSampai(b, senopati);
                b.gunakanJurus(calonArang);
            }
            lewatiSampai(b, senopati);
            int peristiwaSebelum = b.getBabad().getJumlahPeristiwa();

            assertThrows(EnergiBatinHabisException.class, () -> b.gunakanJurus(calonArang));

            assertSame(senopati, b.getAktor(), "Senopati masih boleh memilih aksi lain");
            assertEquals(peristiwaSebelum, b.getBabad().getJumlahPeristiwa(), "Tidak ada yang tercatat");
            b.serang(calonArang);
            assertNotSame(senopati, b.getAktor());
        }

        @Test
        @DisplayName("Jurus area: target diabaikan, semua lawan hidup kena")
        void jurusArea() throws EnergiBatinHabisException {
            BattleManager b = pertarunganPenuh();
            lewatiSampai(b, wiku);
            int nyawaCalonArang = calonArang.getNyawa();
            int nyawaLeak = leak.getNyawa();

            b.gunakanJurus(null);

            assertEquals(nyawaCalonArang - (Wiku.KEKUATAN_MANTRA - CalonArang.PERTAHANAN_AWAL), calonArang.getNyawa());
            assertEquals(nyawaLeak - (Wiku.KEKUATAN_MANTRA - Leak.PERTAHANAN_AWAL), leak.getNyawa());
        }

        @Test
        @DisplayName("Ramuan Penawar hanya boleh ke kawan")
        void penyembuhanKeKawan() throws EnergiBatinHabisException {
            BattleManager b = pertarunganPenuh();
            lewatiSampai(b, balian);                        // Calon Arang sempat menyerang Senopati
            assertTrue(senopati.getNyawa() < senopati.getNyawaMaks());
            assertEquals(JenisSasaran.SATU_KAWAN, balian.getSasaranJurus());
            assertEquals(List.of(senopati, wiku, telikSandi, balian), b.getSasaranJurus());

            assertThrows(IllegalArgumentException.class, () -> b.gunakanJurus(leak));
            b.gunakanJurus(senopati);
            assertEquals(senopati.getNyawaMaks(), senopati.getNyawa());
        }
    }

    // ================================================================

    @Nested
    class Validasi {

        @Test
        void tidakBolehMenyerangKawanSendiri() {
            BattleManager b = pertarunganPenuh();
            assertThrows(IllegalArgumentException.class, () -> b.serang(senopati));
            assertSame(telikSandi, b.getAktor(), "Aksi yang ditolak tidak menghabiskan giliran");
        }

        @Test
        void tidakBolehMenyerangYangSudahGugur() {
            BattleManager b = pertarunganPenuh();
            leak.terimaSerangan(1000);
            assertThrows(IllegalArgumentException.class, () -> b.serang(leak));
            assertEquals(List.of(calonArang), b.getSasaranSerang());
        }

        @Test
        void belumDimulai() {
            BattleManager b = new BattleManager(List.of(senopati), List.of(leak));
            assertEquals(StatusBattle.BELUM_MULAI, b.getStatus());
            assertThrows(IllegalStateException.class, b::getAktor);
            assertThrows(IllegalStateException.class, () -> b.serang(leak));
            assertEquals(List.of(), b.getAntrianGiliran());
        }

        @Test
        void tidakBolehDimulaiDuaKali() {
            BattleManager b = pertarunganPenuh();
            assertThrows(IllegalStateException.class, b::mulai);
        }

        @Test
        void pesertaTidakSah() {
            assertThrows(IllegalArgumentException.class, () -> new BattleManager(List.of(), List.of(leak)));
            assertThrows(IllegalArgumentException.class, () -> new BattleManager(List.of(senopati), List.of()));
            assertThrows(IllegalArgumentException.class,
                    () -> new BattleManager(List.of(senopati, senopati), List.of(leak)));
            assertThrows(IllegalArgumentException.class,
                    () -> new BattleManager(List.of(senopati), List.of(leak), -1));

            leak.terimaSerangan(1000);
            assertThrows(IllegalArgumentException.class, () -> new BattleManager(List.of(senopati), List.of(leak)));
        }
    }

    // ================================================================

    @Nested
    class AkhirPertarungan {

        @Test
        void menang() {
            leak.terimaSerangan(62);                        // -> nyawa 1
            BattleManager b = new BattleManager(List.of(senopati), List.of(leak));
            Perekam perekam = new Perekam();
            b.tambahListener(perekam);
            b.mulai();

            b.serang(leak);

            assertEquals(StatusBattle.MENANG, b.getStatus());
            assertEquals(List.of(StatusBattle.MENANG), perekam.hasil);
            assertThrows(IllegalStateException.class, b::getAktor);
            assertThrows(IllegalStateException.class, () -> b.serang(leak));
            assertTrue(b.getBabad().getIsi().contains("memenangkan pertarungan"));
        }

        @Test
        void kalah() {
            wiku.terimaSerangan(88);                        // 84 kerusakan -> nyawa 1
            BattleManager b = new BattleManager(List.of(wiku), List.of(leak));
            b.mulai();                                      // Wiku 9, Leak 8

            b.serang(leak);
            b.serang(wiku);

            assertEquals(StatusBattle.KALAH, b.getStatus());
            assertTrue(b.getStatus().isSelesai());
        }

        @Test
        @DisplayName("Batas ronde habis sebelum lawan kalah: WAKTU_HABIS")
        void waktuHabis() {
            BattleManager b = new BattleManager(List.of(senopati), List.of(calonArang), 1);
            b.mulai();                                      // Calon Arang 12, Senopati 10

            b.serang(senopati);
            b.serang(calonArang);

            assertEquals(StatusBattle.WAKTU_HABIS, b.getStatus());
            assertEquals(1, b.getRonde());
            assertTrue(b.getBabad().getIsi().contains("=== Ronde 1 dari 1 ==="));
        }

        @Test
        void batasRondeBelumHabisLanjutKeRondeBerikutnya() {
            BattleManager b = new BattleManager(List.of(senopati), List.of(calonArang), 2);
            b.mulai();
            b.serang(senopati);
            b.serang(calonArang);
            assertEquals(2, b.getRonde());
            assertEquals(StatusBattle.GILIRAN_MUSUH, b.getStatus());
        }
    }

    // ================================================================

    @Nested
    class NarasiDanListener {

        @Test
        @DisplayName("Narasi masuk ke antrean dialog (per baris) dan ke babad")
        void narasiDicatat() {
            BattleManager b = pertarunganPenuh();
            String narasi = b.serang(leak);

            AntreanDialog antrean = b.getAntreanDialog();
            assertEquals("Kebo Ijo melesat dari bayangan dan menikam Leak Setra!", antrean.ambilBerikutnya());
            assertEquals("Leak Setra menerima 9 kerusakan (nyawa 51/60).", antrean.ambilBerikutnya());
            assertFalse(antrean.adaBaris());

            String babad = b.getBabad().getIsi();
            assertTrue(babad.startsWith("=== Ronde 1 ===\n"), babad);
            assertTrue(babad.contains(narasi), babad);
        }

        @Test
        void listenerMenerimaSemuaPeristiwa() {
            BattleManager b = new BattleManager(List.of(senopati), List.of(leak));
            Perekam perekam = new Perekam();
            b.tambahListener(perekam);

            b.mulai();
            assertEquals(List.of(1), perekam.ronde);
            assertEquals(List.of(senopati), perekam.aktor);

            b.serang(leak);
            assertEquals(1, perekam.narasi.size());
            assertEquals(List.of(senopati, leak), perekam.aktor);

            b.hapusListener(perekam);
            b.serang(senopati);
            assertEquals(1, perekam.narasi.size(), "Listener yang dilepas tidak menerima apa pun lagi");
        }
    }

    // ================================================================
    // Pembantu uji
    // ================================================================

    /** Musuh uji dengan kecepatan bebas, untuk kasus kecepatan seri. */
    static class MakhlukUji extends MakhlukGirah {
        MakhlukUji(String nama, int kecepatan) {
            super(nama, 50, 0, 5, 0, kecepatan);
        }

        @Override public String serang(KarakterMitos target) { return getNama() + " diam."; }
        @Override public String gunakanJurus(List<KarakterMitos> sasaran) { return getNama() + " diam."; }
        @Override public String getNamaJurus() { return "Diam"; }
        @Override public int getBiayaJurus() { return 0; }
        @Override public JenisSasaran getSasaranJurus() { return JenisSasaran.SATU_LAWAN; }
        @Override public String getPeran() { return "Uji"; }
        @Override public String getIdSprite() { return "uji"; }
    }

    static class Perekam implements BattleListener {
        final List<String> narasi = new ArrayList<>();
        final List<KarakterMitos> aktor = new ArrayList<>();
        final List<Integer> ronde = new ArrayList<>();
        final List<StatusBattle> hasil = new ArrayList<>();

        @Override public void onNarasi(String n) { narasi.add(n); }
        @Override public void onGiliranBaru(KarakterMitos a, StatusBattle s) { aktor.add(a); }
        @Override public void onRondeBaru(int r) { ronde.add(r); }
        @Override public void onBattleSelesai(StatusBattle h) { hasil.add(h); }
    }
}
