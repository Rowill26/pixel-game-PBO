package id.girah.karakter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class KarakterMitosTest {

    /** Karakter uji minimal untuk menguji perilaku kelas dasar. */
    static class KarakterUji extends KarakterMitos {
        KarakterUji(String nama, int nyawa, int energi, int kekuatan, int pertahanan, int kecepatan) {
            super(nama, nyawa, energi, kekuatan, pertahanan, kecepatan);
        }

        @Override
        public String serang(KarakterMitos target) {
            return laporanKerusakan(target, target.terimaSerangan(getKekuatan()));
        }

        @Override
        public String gunakanJurus(List<KarakterMitos> sasaran) throws EnergiBatinHabisException {
            pakaiEnergiBatin(getBiayaJurus());
            return "jurus";
        }

        @Override public String getNamaJurus() { return "Jurus Uji"; }
        @Override public int getBiayaJurus() { return 10; }
        @Override public JenisSasaran getSasaranJurus() { return JenisSasaran.SATU_LAWAN; }
        @Override public String getPeran() { return "Penguji"; }
        @Override public Faksi getFaksi() { return Faksi.GIRAH; }

        void pakai(int biaya) throws EnergiBatinHabisException {
            pakaiEnergiBatin(biaya);
        }
    }

    private KarakterUji karakter;

    @BeforeEach
    void siapkan() {
        karakter = new KarakterUji("Uji", 100, 40, 10, 5, 7);
    }

    @Test
    @DisplayName("Karakter baru mulai dengan nyawa dan energi penuh")
    void statusAwalPenuh() {
        assertEquals(100, karakter.getNyawa());
        assertEquals(100, karakter.getNyawaMaks());
        assertEquals(40, karakter.getEnergiBatin());
        assertEquals(40, karakter.getEnergiBatinMaks());
        assertEquals(10, karakter.getKekuatan());
        assertEquals(5, karakter.getPertahanan());
        assertEquals(7, karakter.getKecepatan());
        assertTrue(karakter.isHidup());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void namaKosongDitolak(String nama) {
        assertThrows(IllegalArgumentException.class, () -> new KarakterUji(nama, 100, 10, 1, 0, 1));
    }

    @Test
    void namaNullDitolak() {
        assertThrows(IllegalArgumentException.class, () -> new KarakterUji(null, 100, 10, 1, 0, 1));
    }

    @Test
    void statistikTidakValidDitolak() {
        assertThrows(IllegalArgumentException.class, () -> new KarakterUji("X", 0, 10, 1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new KarakterUji("X", 10, -1, 1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new KarakterUji("X", 10, 10, -1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new KarakterUji("X", 10, 10, 1, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new KarakterUji("X", 10, 10, 1, 0, -1));
    }

    @Test
    @DisplayName("Kerusakan dikurangi pertahanan")
    void kerusakanDikurangiPertahanan() {
        assertEquals(15, karakter.terimaSerangan(20));
        assertEquals(85, karakter.getNyawa());
    }

    @Test
    @DisplayName("Serangan lemah tetap memberi minimal 1 kerusakan")
    void kerusakanMinimalSatu() {
        assertEquals(1, karakter.terimaSerangan(3));
        assertEquals(99, karakter.getNyawa());
    }

    @Test
    void seranganNolTidakMelukai() {
        assertEquals(0, karakter.terimaSerangan(0));
        assertEquals(100, karakter.getNyawa());
    }

    @Test
    void seranganNegatifDitolak() {
        assertThrows(IllegalArgumentException.class, () -> karakter.terimaSerangan(-1));
    }

    @Test
    @DisplayName("Nyawa tidak pernah di bawah nol dan karakter gugur")
    void nyawaTidakNegatif() {
        assertEquals(100, karakter.terimaSerangan(500));
        assertEquals(0, karakter.getNyawa());
        assertFalse(karakter.isHidup());
        assertEquals(0, karakter.terimaSerangan(50), "Karakter gugur tidak menerima kerusakan lagi");
    }

    @Test
    @DisplayName("Laporan kerusakan menyebut jumlah kerusakan dan sisa nyawa")
    void laporanKerusakan() {
        KarakterUji penyerang = new KarakterUji("Penyerang", 50, 0, 25, 0, 1);
        String narasi = penyerang.serang(karakter);
        assertEquals("Uji menerima 20 kerusakan (nyawa 80/100).", narasi);
    }

    @Test
    @DisplayName("Laporan kerusakan menambahkan narasi gugur")
    void laporanKerusakanGugur() {
        KarakterUji penyerang = new KarakterUji("Penyerang", 50, 0, 500, 0, 1);
        String narasi = penyerang.serang(karakter);
        assertTrue(narasi.endsWith("Uji gugur."), narasi);
    }

    @Test
    void pakaiEnergiMengurangiEnergi() throws EnergiBatinHabisException {
        karakter.pakai(15);
        assertEquals(25, karakter.getEnergiBatin());
    }

    @Test
    void pakaiSeluruhEnergiDiperbolehkan() throws EnergiBatinHabisException {
        karakter.pakai(40);
        assertEquals(0, karakter.getEnergiBatin());
    }

    @Test
    @DisplayName("Energi tidak cukup melempar exception dan energi tidak berubah")
    void energiKurangMelemparException() {
        EnergiBatinHabisException ex =
                assertThrows(EnergiBatinHabisException.class, () -> karakter.pakai(41));
        assertEquals(40, karakter.getEnergiBatin());
        assertEquals("Uji", ex.getNamaKarakter());
        assertEquals(41, ex.getDibutuhkan());
        assertEquals(40, ex.getTersedia());
    }

    @Test
    void bisaMemakaiJurusMengikutiEnergi() throws EnergiBatinHabisException {
        assertTrue(karakter.bisaMemakaiJurus());
        karakter.pakai(35);
        assertFalse(karakter.bisaMemakaiJurus(), "Sisa 5, biaya jurus 10");
    }

    @Test
    void pemulihanEnergiDibatasiMaksimum() throws EnergiBatinHabisException {
        karakter.pakai(30);
        assertEquals(30, karakter.pulihkanEnergiBatin(100));
        assertEquals(40, karakter.getEnergiBatin());
    }

    @Test
    void penyembuhanDibatasiMaksimum() {
        karakter.terimaSerangan(25); // -20
        assertEquals(20, karakter.sembuhkan(50));
        assertEquals(100, karakter.getNyawa());
    }

    @Test
    void karakterGugurTidakBisaDipulihkan() {
        karakter.terimaSerangan(1000);
        assertEquals(0, karakter.sembuhkan(50));
        assertEquals(0, karakter.pulihkanEnergiBatin(50));
        assertFalse(karakter.isHidup());
    }

    @Test
    void bawaanTidakAdaGiliranTambahan() {
        assertFalse(karakter.ambilGiliranTambahan());
    }
}
