package id.girah.battle;

import static org.junit.jupiter.api.Assertions.*;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class AntreanDialogTest {

    private final AntreanDialog antrean = new AntreanDialog();

    @Test
    void firstInFirstOutPerBaris() {
        antrean.tambah("Baris satu\nBaris dua");
        antrean.tambah("Baris tiga");

        assertEquals(3, antrean.jumlah());
        assertEquals("Baris satu", antrean.lihatBerikutnya());
        assertEquals("Baris satu", antrean.ambilBerikutnya());
        assertEquals("Baris dua", antrean.ambilBerikutnya());
        assertEquals("Baris tiga", antrean.ambilBerikutnya());
        assertFalse(antrean.adaBaris());
    }

    @Test
    void barisKosongDanNullDibuang() {
        antrean.tambah("  \nIsi  \n\n");
        antrean.tambah(null);
        assertEquals(1, antrean.jumlah());
        assertEquals("Isi", antrean.ambilBerikutnya());
    }

    @Test
    void ambilDariAntreanKosongMelemparException() {
        assertNull(antrean.lihatBerikutnya());
        assertThrows(NoSuchElementException.class, antrean::ambilBerikutnya);
    }

    @Test
    void kosongkan() {
        antrean.tambah("a\nb");
        antrean.kosongkan();
        assertFalse(antrean.adaBaris());
    }
}
