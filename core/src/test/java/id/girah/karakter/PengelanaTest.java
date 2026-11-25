package id.girah.karakter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Wiku, Telik Sandi, dan Balian. */
class PengelanaTest {

    @Nested
    class WikuTest {

        private final Wiku wiku = new Wiku("Dang Hyang");

        @Test
        @DisplayName("Mantra Lingkar Suci mengenai semua lawan hidup: 22 - 3 = 19 per Leak")
        void jurusArea() throws EnergiBatinHabisException {
            Leak a = new Leak("Leak A");
            Leak b = new Leak("Leak B");
            Leak gugur = new Leak("Leak Gugur");
            gugur.terimaSerangan(1000);

            String narasi = wiku.gunakanJurus(List.of(a, b, gugur));

            assertEquals(41, a.getNyawa());
            assertEquals(41, b.getNyawa());
            assertEquals(55, wiku.getEnergiBatin());
            assertFalse(narasi.contains("Leak Gugur"), "Lawan yang sudah gugur tidak disebut");
            assertEquals(JenisSasaran.SEMUA_LAWAN, wiku.getSasaranJurus());
        }

        @Test
        void tanpaLawanHidupTidakMemakaiEnergi() throws EnergiBatinHabisException {
            Leak gugur = new Leak("Leak Gugur");
            gugur.terimaSerangan(1000);
            wiku.gunakanJurus(List.of(gugur));
            assertEquals(Wiku.ENERGI_BATIN_AWAL, wiku.getEnergiBatin());
        }
    }

    @Nested
    class TelikSandiTest {

        private final TelikSandi telikSandi = new TelikSandi("Kebo Ijo");
        private final CalonArang calonArang = new CalonArang();

        @Test
        void tercepatDiKelompok() {
            int tercepatLain = List.of(new Senopati("a"), new Wiku("b"), new Balian("c"))
                    .stream().mapToInt(KarakterMitos::getKecepatan).max().orElseThrow();
            assertTrue(telikSandi.getKecepatan() > tercepatLain);
        }

        @Test
        @DisplayName("Langkah Bayangan: menyerang (kekuatan - pertahanan), lalu memberi tepat satu giliran tambahan")
        void langkahBayangan() throws EnergiBatinHabisException {
            String narasi = telikSandi.gunakanJurus(List.of(calonArang));

            assertEquals(CalonArang.NYAWA_AWAL - (TelikSandi.KEKUATAN_AWAL - CalonArang.PERTAHANAN_AWAL),
                    calonArang.getNyawa());
            assertEquals(28, telikSandi.getEnergiBatin());
            assertTrue(narasi.contains("siap bergerak sekali lagi"), narasi);
            assertTrue(telikSandi.ambilGiliranTambahan());
            assertFalse(telikSandi.ambilGiliranTambahan(), "Giliran tambahan hanya diambil sekali");
        }

        @Test
        void seranganBiasaTidakMemberiGiliranTambahan() {
            telikSandi.serang(calonArang);
            assertFalse(telikSandi.ambilGiliranTambahan());
        }

        @Test
        @DisplayName("Hanya sekali per ronde, lalu tersedia lagi setelah awalRonde()")
        void sekaliPerRonde() throws EnergiBatinHabisException {
            telikSandi.gunakanJurus(List.of(calonArang));
            assertFalse(telikSandi.bisaMemakaiJurus());
            assertThrows(IllegalStateException.class, () -> telikSandi.gunakanJurus(List.of(calonArang)));
            assertEquals(28, telikSandi.getEnergiBatin(), "Percobaan yang ditolak tidak memakai energi");

            telikSandi.awalRonde();
            assertTrue(telikSandi.bisaMemakaiJurus());
        }

        @Test
        @DisplayName("Energi habis: exception, dan tidak ada giliran tambahan")
        void energiHabis() throws EnergiBatinHabisException {
            for (int ronde = 0; ronde < 3; ronde++) {   // 40 - 12 x 3 = 4
                telikSandi.awalRonde();
                telikSandi.gunakanJurus(List.of(calonArang));
                telikSandi.ambilGiliranTambahan();
            }
            telikSandi.awalRonde();
            assertThrows(EnergiBatinHabisException.class, () -> telikSandi.gunakanJurus(List.of(calonArang)));
            assertFalse(telikSandi.ambilGiliranTambahan());
        }

        @Test
        void gugurSebelumGiliranTambahanTidakMendapatGiliran() throws EnergiBatinHabisException {
            telikSandi.gunakanJurus(List.of(calonArang));
            telikSandi.terimaSerangan(10_000);
            assertFalse(telikSandi.ambilGiliranTambahan());
        }
    }

    @Nested
    class BalianTest {

        private final Balian balian = new Balian("Jero Balian");
        private final Senopati senopati = new Senopati("Arya");

        @Test
        @DisplayName("Ramuan Penawar memulihkan nyawa kawan, dibatasi maksimum")
        void menyembuhkan() throws EnergiBatinHabisException {
            new CalonArang().serang(senopati);              // 20 - 8 = 12 -> 108
            String narasi = balian.gunakanJurus(List.of(senopati));

            assertEquals(Senopati.NYAWA_AWAL, senopati.getNyawa());
            assertEquals(45, balian.getEnergiBatin());
            assertTrue(narasi.contains("Arya pulih 12 nyawa (nyawa 120/120)."), narasi);
            assertEquals(JenisSasaran.SATU_KAWAN, balian.getSasaranJurus());
        }

        @Test
        void kawanSehatTidakMemakaiEnergi() throws EnergiBatinHabisException {
            balian.gunakanJurus(List.of(senopati));
            assertEquals(Balian.ENERGI_BATIN_AWAL, balian.getEnergiBatin());
        }

        @Test
        void kawanGugurTidakBisaDisembuhkan() throws EnergiBatinHabisException {
            senopati.terimaSerangan(10_000);
            String narasi = balian.gunakanJurus(List.of(senopati));
            assertFalse(senopati.isHidup());
            assertEquals(Balian.ENERGI_BATIN_AWAL, balian.getEnergiBatin());
            assertTrue(narasi.contains("sudah terlambat"), narasi);
        }
    }
}
