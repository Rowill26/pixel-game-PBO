package id.girah.karakter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Data yang dipakai front end: kunci sprite (B3) dan jarak aksi (B7). */
class DataFrontEndTest {

    private final Senopati senopati = new Senopati("Arya");
    private final Wiku wiku = new Wiku("Dang Hyang");
    private final TelikSandi telikSandi = new TelikSandi("Kebo Ijo");
    private final Balian balian = new Balian("Jero Balian");
    private final Leak leak = new Leak("Leak Setra");
    private final CalonArang calonArang = new CalonArang();

    @Test
    @DisplayName("Kunci sprite sama dengan nama file sprite di assets/sprites front end")
    void idSprite() {
        assertEquals("pendekar", senopati.getIdSprite());
        assertEquals("juru_mantra", wiku.getIdSprite());
        assertEquals("pengintai", telikSandi.getIdSprite());
        assertEquals("peramu", balian.getIdSprite());
        assertEquals("leak", leak.getIdSprite());
        assertEquals("calon_arang", calonArang.getIdSprite());
    }

    @Test
    @DisplayName("Nama peran yang ditampilkan mengikuti naskah cerita")
    void namaPeranNaskah() {
        assertEquals("Pendekar", senopati.getPeran());
        assertEquals("Juru Mantra", wiku.getPeran());
        assertEquals("Pengintai", telikSandi.getPeran());
        assertEquals("Peramu", balian.getPeran());
        assertEquals("Leak", leak.getPeran());
        assertEquals("Janda Sakti dari Girah", calonArang.getPeran());
        assertTrue(senopati.toString().startsWith("Pendekar Arya "), senopati.toString());
    }

    @Test
    void idSpriteUnikPerKelas() {
        List<KarakterMitos> semua = List.of(senopati, wiku, telikSandi, balian, leak, calonArang,
                new Pengikut("Pengikut"), new PemimpinPengikut());
        Set<String> id = new HashSet<>();
        for (KarakterMitos k : semua) {
            assertTrue(id.add(k.getIdSprite()), "Kunci sprite ganda: " + k.getIdSprite());
        }
    }

    @Test
    @DisplayName("Dua Leak berbeda nama, tetapi kunci sprite-nya sama")
    void karakterSejenisBerbagiSprite() {
        Leak lain = new Leak("Leak Pangkung");
        assertNotEquals(leak.getNama(), lain.getNama());
        assertEquals(leak.getIdSprite(), lain.getIdSprite());
    }

    @Test
    @DisplayName("Jarak serangan dasar mengikuti narasinya")
    void jarakSerang() {
        assertEquals(JarakAksi.DEKAT, senopati.getJarakSerang(), "menebas dengan keris");
        assertEquals(JarakAksi.DEKAT, wiku.getJarakSerang(), "mengayunkan tongkat");
        assertEquals(JarakAksi.DEKAT, telikSandi.getJarakSerang(), "menikam");
        assertEquals(JarakAksi.JAUH, balian.getJarakSerang(), "melempar kantung getah");
        assertEquals(JarakAksi.DEKAT, leak.getJarakSerang(), "mencakar");
        assertEquals(JarakAksi.JAUH, calonArang.getJarakSerang(), "asap dupa dari lingkaran ritual");
    }

    @Test
    @DisplayName("Jarak jurus mengikuti narasinya")
    void jarakJurus() {
        assertEquals(JarakAksi.DEKAT, senopati.getJarakJurus(), "Tebasan Keris Pusaka");
        assertEquals(JarakAksi.JAUH, wiku.getJarakJurus(), "Mantra Lingkar Suci");
        assertEquals(JarakAksi.DEKAT, telikSandi.getJarakJurus(), "Langkah Bayangan");
        assertEquals(JarakAksi.JAUH, balian.getJarakJurus(), "Ramuan Penawar");
        assertEquals(JarakAksi.JAUH, leak.getJarakJurus(), "Jerat Kabut");
        assertEquals(JarakAksi.JAUH, calonArang.getJarakJurus(), "Sebar Wabah");
    }
}
