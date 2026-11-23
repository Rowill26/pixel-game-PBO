package id.girah.ai;

import static org.junit.jupiter.api.Assertions.*;

import id.girah.karakter.Balian;
import id.girah.karakter.CalonArang;
import id.girah.karakter.EnergiBatinHabisException;
import id.girah.karakter.KarakterMitos;
import id.girah.karakter.Leak;
import id.girah.karakter.Senopati;
import id.girah.karakter.Wiku;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StrategiDasarTest {

    /** Random yang selalu mengembalikan angka yang sama. */
    private static Random tetap(double nilai) {
        return new Random() {
            @Override
            public double nextDouble() {
                return nilai;
            }
        };
    }

    private Senopati senopati;
    private Wiku wiku;

    @BeforeEach
    void siapkan() {
        senopati = new Senopati("Arya");
        wiku = new Wiku("Dang Hyang");
    }

    @Test
    @DisplayName("Calon Arang memakai Sebar Wabah jika ada minimal dua pengelana")
    void wabahKeBanyakLawan() {
        List<KarakterMitos> kelompok = List.of(senopati, wiku);
        KeputusanAksi k = new StrategiDasar().tentukan(new CalonArang(), kelompok, kelompok);
        assertEquals(KeputusanAksi.Jenis.JURUS, k.getJenis());
        assertNull(k.getTarget());
    }

    @Test
    @DisplayName("Tinggal satu pengelana: Calon Arang menyerang biasa")
    void satuLawanSerangBiasa() {
        KeputusanAksi k = new StrategiDasar().tentukan(new CalonArang(), List.of(wiku), List.of(wiku));
        assertEquals(KeputusanAksi.Jenis.SERANG, k.getJenis());
        assertSame(wiku, k.getTarget());
    }

    @Test
    @DisplayName("Energi batin habis: tidak memilih jurus")
    void tanpaEnergiTidakMemilihJurus() throws EnergiBatinHabisException {
        CalonArang calonArang = new CalonArang();
        List<KarakterMitos> kelompok = List.of(senopati, wiku);
        for (int i = 0; i < 4; i++) {                       // 120 - 30 x 4 = 0
            calonArang.gunakanJurus(List.of(new Senopati("Samsak " + i)));
        }
        KeputusanAksi k = new StrategiDasar().tentukan(calonArang, kelompok, kelompok);
        assertEquals(KeputusanAksi.Jenis.SERANG, k.getJenis());
    }

    @Test
    @DisplayName("Serangan diarahkan ke lawan dengan nyawa terendah")
    void incarYangTerlemah() {
        senopati.terimaSerangan(60);                        // 52 kerusakan -> 68, lebih rendah dari Wiku 85
        KeputusanAksi k = new StrategiDasar(tetap(0.99)).tentukan(new Leak("Leak"), List.of(wiku, senopati),
                List.of(wiku, senopati));
        assertEquals(KeputusanAksi.serang(senopati).toString(), k.toString());
    }

    @Test
    void nyawaSeriPilihYangPertama() {
        Senopati lain = new Senopati("Lain");
        assertSame(senopati, StrategiDasar.terlemah(List.of(senopati, lain)));
    }

    @Test
    @DisplayName("Leak memakai Jerat Kabut sesuai peluang")
    void peluangJurusTunggal() {
        List<KarakterMitos> lawan = List.of(senopati, wiku);
        KeputusanAksi pakai = new StrategiDasar(tetap(0.0)).tentukan(new Leak("Leak"), lawan, lawan);
        KeputusanAksi tidak = new StrategiDasar(tetap(0.99)).tentukan(new Leak("Leak"), lawan, lawan);

        assertEquals(KeputusanAksi.Jenis.JURUS, pakai.getJenis());
        assertSame(wiku, pakai.getTarget(), "Wiku nyawanya paling rendah (85)");
        assertEquals(KeputusanAksi.Jenis.SERANG, tidak.getJenis());
    }

    @Test
    @DisplayName("Penyembuh hanya menyembuhkan kawan yang nyawanya di bawah 50%")
    void penyembuhMenungguKawanKritis() {
        Balian balian = new Balian("Penyembuh");
        StrategiDasar strategi = new StrategiDasar();

        senopati.terimaSerangan(60);                        // 68/120, di atas 50%
        assertEquals(KeputusanAksi.Jenis.SERANG,
                strategi.tentukan(balian, List.of(wiku), List.of(senopati)).getJenis());

        senopati.terimaSerangan(18);                        // 10 kerusakan -> 58/120, di bawah 50%
        KeputusanAksi k = strategi.tentukan(balian, List.of(wiku), List.of(senopati));
        assertEquals(KeputusanAksi.Jenis.JURUS, k.getJenis());
        assertSame(senopati, k.getTarget());
    }

    @Test
    void tanpaSasaranDitolak() {
        assertThrows(IllegalArgumentException.class,
                () -> new StrategiDasar().tentukan(new Leak("Leak"), List.of(), List.of()));
    }
}
