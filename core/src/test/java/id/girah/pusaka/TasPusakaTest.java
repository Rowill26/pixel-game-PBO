package id.girah.pusaka;

import static org.junit.jupiter.api.Assertions.*;

import id.girah.karakter.Senopati;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TasPusakaTest {

    private TasPusaka tas;
    private Senopati senopati;

    @BeforeEach
    void siapkan() {
        tas = new TasPusaka();
        senopati = new Senopati("Arya");
    }

    @Test
    void tasBaruKosong() {
        assertTrue(tas.isKosong());
        assertEquals(0, tas.jumlah(new TirtaSuci()));
        assertFalse(tas.punya(new TirtaSuci()));
    }

    @Test
    @DisplayName("Menambah pusaka yang sama menjumlahkan, meski objeknya berbeda")
    void tambahMenjumlahkan() {
        tas.tambah(new TirtaSuci(), 2);
        tas.tambah(new TirtaSuci(), 3);
        tas.tambah(new KembangKantil(), 1);

        assertEquals(5, tas.jumlah(new TirtaSuci()));
        assertEquals(1, tas.jumlah(new KembangKantil()));
        assertEquals(2, tas.getIsi().size());
    }

    @Test
    void tambahTidakSahDitolak() {
        assertThrows(IllegalArgumentException.class, () -> tas.tambah(new TirtaSuci(), 0));
        assertThrows(IllegalArgumentException.class, () -> tas.tambah(new TirtaSuci(), -1));
        assertThrows(NullPointerException.class, () -> tas.tambah(null, 1));
    }

    @Test
    @DisplayName("Memakai pusaka mengurangi jumlah; yang habis dihapus dari map")
    void gunakanMengurangiLaluMenghapus() {
        tas.tambah(new TirtaSuci(), 2);
        senopati.terimaSerangan(40);                 // 32 kerusakan, tetap hidup

        tas.gunakan(new TirtaSuci(), senopati, senopati);
        assertEquals(1, tas.jumlah(new TirtaSuci()));

        senopati.terimaSerangan(40);                 // 32 kerusakan, tetap hidup
        tas.gunakan(new TirtaSuci(), senopati, senopati);
        assertFalse(tas.getIsi().containsKey(new TirtaSuci()));
        assertTrue(tas.isKosong());
    }

    @Test
    void pusakaYangTidakAdaDitolak() {
        senopati.terimaSerangan(100);
        assertThrows(IllegalStateException.class, () -> tas.gunakan(new TirtaSuci(), senopati, senopati));
    }

    @Test
    @DisplayName("Pusaka yang tidak berguna ditolak dan tidak berkurang")
    void tidakBergunaTidakBerkurang() {
        tas.tambah(new TirtaSuci(), 1);
        assertThrows(IllegalArgumentException.class, () -> tas.gunakan(new TirtaSuci(), senopati, senopati));
        assertEquals(1, tas.jumlah(new TirtaSuci()));
    }

    @Test
    void daftarUrutNamaDanIsiTidakBisaDiubah() {
        tas.tambah(new TirtaSuci(), 1);
        tas.tambah(new KembangKantil(), 1);

        assertEquals(List.of(new KembangKantil(), new TirtaSuci()), tas.getDaftarPusaka());
        Map<Pusaka, Integer> isi = tas.getIsi();
        assertThrows(UnsupportedOperationException.class, () -> isi.put(new TirtaSuci(), 99));
    }
}
