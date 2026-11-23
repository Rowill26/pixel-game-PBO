package id.girah.pusaka;

import static org.junit.jupiter.api.Assertions.*;

import id.girah.karakter.CalonArang;
import id.girah.karakter.EnergiBatinHabisException;
import id.girah.karakter.Senopati;
import id.girah.karakter.Wiku;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PusakaTest {

    private final Senopati senopati = new Senopati("Arya");
    private final Wiku wiku = new Wiku("Dang Hyang");

    @Test
    @DisplayName("Tirta Suci memulihkan nyawa, dibatasi maksimum")
    void tirtaSuci() {
        senopati.terimaSerangan(38);                        // 30 kerusakan -> 90
        TirtaSuci tirta = new TirtaSuci();
        assertTrue(tirta.bisaDipakaiPada(senopati));

        String narasi = tirta.gunakan(wiku, senopati);

        assertEquals(120, senopati.getNyawa());
        assertEquals("Dang Hyang memercikkan Tirta Suci kepada Arya.\nArya pulih 30 nyawa (nyawa 120/120).", narasi);
    }

    @Test
    void tirtaSuciTidakBergunaUntukYangSehatAtauGugur() {
        TirtaSuci tirta = new TirtaSuci();
        assertFalse(tirta.bisaDipakaiPada(senopati), "Nyawa masih penuh");
        senopati.terimaSerangan(10_000);
        assertFalse(tirta.bisaDipakaiPada(senopati), "Sudah gugur");
    }

    @Test
    void tirtaSuciKeDiriSendiri() {
        senopati.terimaSerangan(18);
        assertTrue(new TirtaSuci().gunakan(senopati, senopati).startsWith("Arya memercikkan Tirta Suci kepada dirinya sendiri."));
    }

    @Test
    @DisplayName("Kembang Kantil memulihkan energi batin")
    void kembangKantil() throws EnergiBatinHabisException {
        KembangKantil kantil = new KembangKantil();
        assertFalse(kantil.bisaDipakaiPada(wiku), "Energi batin masih penuh");

        wiku.gunakanJurus(List.of(new CalonArang()));      // 80 - 25 = 55
        assertTrue(kantil.bisaDipakaiPada(wiku));
        String narasi = kantil.gunakan(senopati, wiku);

        assertEquals(80, wiku.getEnergiBatin());
        assertTrue(narasi.endsWith("Dang Hyang memulihkan 25 energi batin (energi batin 80/80)."), narasi);
    }

    @Test
    @DisplayName("equals/hashCode: dua objek Tirta Suci adalah pusaka yang sama")
    void kesamaan() {
        assertEquals(new TirtaSuci(), new TirtaSuci());
        assertEquals(new TirtaSuci().hashCode(), new TirtaSuci().hashCode());
        assertNotEquals(new TirtaSuci(), new KembangKantil());
        assertNotEquals(new TirtaSuci(), null);
    }
}
