package id.girah.karakter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SenopatiTest {

    private Senopati senopati;
    private Leak leak;

    @BeforeEach
    void siapkan() {
        senopati = new Senopati("Arya");
        leak = new Leak("Leak Setra");
    }

    @Test
    void identitas() {
        assertEquals(Faksi.PENGELANA, senopati.getFaksi());
        assertEquals("Pendekar", senopati.getPeran());
        assertEquals("Tebasan Keris Pusaka", senopati.getNamaJurus());
        assertEquals(JenisSasaran.SATU_LAWAN, senopati.getSasaranJurus());
    }

    @Test
    @DisplayName("Serangan dasar: 15 - pertahanan Leak 3 = 12, tanpa energi batin")
    void seranganDasar() {
        String narasi = senopati.serang(leak);
        assertEquals(48, leak.getNyawa());
        assertEquals(Senopati.ENERGI_BATIN_AWAL, senopati.getEnergiBatin());
        assertTrue(narasi.startsWith("Arya menghunus keris dan menebas Leak Setra!"), narasi);
        assertTrue(narasi.contains("Leak Setra menerima 12 kerusakan (nyawa 48/60)."), narasi);
    }

    @Test
    @DisplayName("Jurus: 15 x 3 - 3 = 42 kerusakan, energi 50 - 20 = 30")
    void jurus() throws EnergiBatinHabisException {
        String narasi = senopati.gunakanJurus(List.of(leak));
        assertEquals(18, leak.getNyawa());
        assertEquals(30, senopati.getEnergiBatin());
        assertTrue(narasi.contains("TEBASAN KERIS PUSAKA!"), narasi);
    }

    @Test
    @DisplayName("Jurus ketiga gagal (50 - 20 - 20 = 10) dan tidak mengubah apa pun")
    void jurusGagalSaatEnergiHabis() throws EnergiBatinHabisException {
        CalonArang calonArang = new CalonArang();
        senopati.gunakanJurus(List.of(calonArang));
        senopati.gunakanJurus(List.of(calonArang));
        int nyawaSebelum = calonArang.getNyawa();

        EnergiBatinHabisException ex = assertThrows(EnergiBatinHabisException.class,
                () -> senopati.gunakanJurus(List.of(calonArang)));

        assertEquals(10, senopati.getEnergiBatin());
        assertEquals(nyawaSebelum, calonArang.getNyawa());
        assertEquals(10, ex.getKekurangan());
        assertFalse(senopati.bisaMemakaiJurus());
    }

    @Test
    void jurusKeTargetGugurTidakMemakaiEnergi() throws EnergiBatinHabisException {
        leak.terimaSerangan(1000);
        String narasi = senopati.gunakanJurus(List.of(leak));
        assertEquals(Senopati.ENERGI_BATIN_AWAL, senopati.getEnergiBatin());
        assertTrue(narasi.contains("sudah gugur"), narasi);
    }

    @Test
    void senopatiGugurTidakBisaBertindak() throws EnergiBatinHabisException {
        senopati.terimaSerangan(10_000);
        assertEquals("Arya sudah gugur dan tidak bisa bertindak.", senopati.serang(leak));
        senopati.gunakanJurus(List.of(leak));
        assertEquals(Leak.NYAWA_AWAL, leak.getNyawa());
    }

    @Test
    void narasiGugurKhasSenopati() {
        String narasi = new CalonArang().serang(senopati);
        assertFalse(narasi.contains("roboh"));
        senopati.terimaSerangan(10_000);
        assertTrue(senopati.narasiGugur().contains("kerisnya tertancap"));
    }

    @Test
    void jurusButuhTepatSatuSasaran() {
        assertThrows(IllegalArgumentException.class, () -> senopati.gunakanJurus(List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> senopati.gunakanJurus(List.of(leak, new Leak("Leak Lain"))));
    }

    @Test
    void targetNullDitolak() {
        assertThrows(NullPointerException.class, () -> senopati.serang(null));
        assertThrows(NullPointerException.class, () -> senopati.gunakanJurus(null));
    }
}
