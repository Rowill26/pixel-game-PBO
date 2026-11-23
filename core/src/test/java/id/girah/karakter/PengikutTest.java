package id.girah.karakter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Pengikut dan Pemimpin Pengikut (catatan revisi D1). */
class PengikutTest {

    private Senopati senopati;
    private Wiku wiku;
    private TelikSandi telikSandi;
    private Balian balian;

    @BeforeEach
    void siapkan() {
        senopati = new Senopati("Arya");
        wiku = new Wiku("Dang Hyang");
        telikSandi = new TelikSandi("Kebo Ijo");
        balian = new Balian("Jero Balian");
    }

    @Nested
    class PengikutBiasa {

        private final Pengikut pengikut = new Pengikut("Pengikut Girah");

        @Test
        void identitas() {
            assertEquals(Faksi.GIRAH, pengikut.getFaksi());
            assertEquals("Pengikut", pengikut.getPeran());
            assertEquals("pengikut", pengikut.getIdSprite());
            assertEquals("Asap Wabah", pengikut.getNamaJurus());
            assertEquals(JenisSasaran.SEMUA_LAWAN, pengikut.getSasaranJurus());
            assertEquals(JarakAksi.DEKAT, pengikut.getJarakSerang());
            assertEquals(JarakAksi.JAUH, pengikut.getJarakJurus());
            assertFalse(pengikut.isBos());
        }

        @Test
        @DisplayName("Serangan golok: 11 - pertahanan Senopati 8 = 3")
        void serang() {
            String narasi = pengikut.serang(senopati);
            assertEquals(117, senopati.getNyawa());
            assertTrue(narasi.startsWith("Pengikut Girah mengayunkan golok ke arah Arya!"), narasi);
        }

        @Test
        @DisplayName("Asap Wabah: 12 ke setiap pengelana, dikurangi pertahanannya masing-masing")
        void asapWabah() throws EnergiBatinHabisException {
            String narasi = pengikut.gunakanJurus(List.of(senopati, wiku, telikSandi, balian));

            assertEquals(120 - 4, senopati.getNyawa());
            assertEquals(85 - 8, wiku.getNyawa());
            assertEquals(90 - 7, telikSandi.getNyawa());
            assertEquals(95 - 6, balian.getNyawa());
            assertEquals(15, pengikut.getEnergiBatin());
            assertTrue(narasi.contains("Asap kehijauan menyebar"), narasi);
        }

        @Test
        @DisplayName("Energi 30 cukup untuk dua Asap Wabah; yang ketiga ditolak tanpa efek")
        void energiHabis() throws EnergiBatinHabisException {
            pengikut.gunakanJurus(List.of(senopati));
            pengikut.gunakanJurus(List.of(senopati));
            int nyawaSebelum = senopati.getNyawa();

            assertThrows(EnergiBatinHabisException.class, () -> pengikut.gunakanJurus(List.of(senopati)));
            assertEquals(0, pengikut.getEnergiBatin());
            assertEquals(nyawaSebelum, senopati.getNyawa());
        }

        @Test
        void tanpaSasaranHidupTidakMemakaiEnergi() throws EnergiBatinHabisException {
            senopati.terimaSerangan(10_000);
            pengikut.gunakanJurus(List.of(senopati));
            assertEquals(Pengikut.ENERGI_BATIN_AWAL, pengikut.getEnergiBatin());
        }

        @Test
        @DisplayName("Saat kalah, pengikut menyerah (manusia, bukan makhluk gaib)")
        void menyerah() {
            pengikut.terimaSerangan(70);                    // 66 kerusakan -> nyawa 4
            String narasi = senopati.serang(pengikut);
            assertFalse(pengikut.isHidup());
            assertTrue(narasi.endsWith("Pengikut Girah menjatuhkan goloknya dan berlutut, menyerah."), narasi);
        }

        @Test
        void tidakPernahBerucapSaatNyawaRendah() {
            pengikut.terimaSerangan(60);                    // 56 kerusakan -> nyawa 14 (20%)
            assertTrue(pengikut.isNyawaRendah());
            assertFalse(senopati.serang(pengikut).contains(": \""));
        }
    }

    @Nested
    class Pemimpin {

        private final PemimpinPengikut pemimpin = new PemimpinPengikut();

        @Test
        void identitas() {
            assertEquals("Pemimpin Pengikut", pemimpin.getNama());
            assertEquals(Faksi.GIRAH, pemimpin.getFaksi());
            assertEquals("Pemimpin Pengikut", pemimpin.getPeran());
            assertEquals("pemimpin_pengikut", pemimpin.getIdSprite());
            assertEquals("Kutukan Kabut Ungu", pemimpin.getNamaJurus());
            assertEquals(JenisSasaran.SATU_LAWAN, pemimpin.getSasaranJurus());
            assertEquals(JarakAksi.DEKAT, pemimpin.getJarakSerang());
            assertEquals(JarakAksi.JAUH, pemimpin.getJarakJurus());
            assertTrue(pemimpin.isBos());
        }

        @Test
        @DisplayName("Serangan tongkat: 17 - 8 = 9 ke Senopati")
        void serang() {
            pemimpin.serang(senopati);
            assertEquals(111, senopati.getNyawa());
        }

        @Test
        @DisplayName("Kutukan Kabut Ungu: 38 - pertahanan Wiku 4 = 34")
        void kutukan() throws EnergiBatinHabisException {
            String narasi = pemimpin.gunakanJurus(List.of(wiku));
            assertEquals(85 - 34, wiku.getNyawa());
            assertEquals(40, pemimpin.getEnergiBatin());
            assertTrue(narasi.contains("KUTUKAN KABUT UNGU!"), narasi);
        }

        @Test
        void kutukanButuhSatuSasaran() {
            assertThrows(IllegalArgumentException.class, () -> pemimpin.gunakanJurus(List.of(wiku, senopati)));
        }

        @Test
        @DisplayName("Berseru sekali saat nyawa jatuh ke 30% (66 dari 220)")
        void seruanNyawaRendah() {
            pemimpin.terimaSerangan(160);                   // 152 kerusakan -> 68
            String pertama = senopati.serang(pemimpin);     // 7 kerusakan  -> 61
            assertTrue(pertama.contains("Pemimpin Pengikut: \"Jangan mundur!"), pertama);

            String kedua = senopati.serang(pemimpin);       // -> 54
            assertFalse(kedua.contains("Jangan mundur!"), "Seruan tidak diulang");
        }

        @Test
        void narasiGugur() {
            pemimpin.terimaSerangan(220);                   // 212 kerusakan -> 8
            senopati.serang(pemimpin);                      // 7 kerusakan -> 1
            assertTrue(pemimpin.isHidup());
            String narasi = senopati.serang(pemimpin);
            assertFalse(pemimpin.isHidup());
            assertTrue(narasi.endsWith("para pengikut kehilangan arah."), narasi);
        }
    }

    @Test
    @DisplayName("Hanya Calon Arang dan Pemimpin Pengikut yang bos")
    void penandaBos() {
        assertTrue(new CalonArang().isBos());
        assertTrue(new PemimpinPengikut().isBos());
        assertFalse(new Leak("Leak").isBos());
        assertFalse(new Pengikut("Pengikut").isBos());
    }
}
