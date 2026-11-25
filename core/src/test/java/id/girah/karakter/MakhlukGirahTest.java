package id.girah.karakter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Leak dan Calon Arang, plus uji polymorphism lintas semua kelas. */
class MakhlukGirahTest {

    private Senopati senopati;
    private Wiku wiku;
    private TelikSandi telikSandi;
    private Balian balian;
    private CalonArang calonArang;

    @BeforeEach
    void siapkan() {
        senopati = new Senopati("Arya");
        wiku = new Wiku("Dang Hyang");
        telikSandi = new TelikSandi("Kebo Ijo");
        balian = new Balian("Jero Balian");
        calonArang = new CalonArang();
    }

    @Test
    @DisplayName("Jerat Kabut Leak: 13 x 2 - 8 = 18 ke Senopati")
    void jeratKabut() throws EnergiBatinHabisException {
        Leak leak = new Leak("Leak Setra");
        leak.gunakanJurus(List.of(senopati));
        assertEquals(102, senopati.getNyawa());
        assertEquals(10, leak.getEnergiBatin());
        assertEquals(Faksi.GIRAH, leak.getFaksi());
    }

    @Test
    @DisplayName("Sebar Wabah mengenai seluruh kelompok, masing-masing dikurangi pertahanannya")
    void sebarWabah() throws EnergiBatinHabisException {
        String narasi = calonArang.gunakanJurus(List.of(senopati, wiku, telikSandi, balian));

        assertEquals(120 - 14, senopati.getNyawa());
        assertEquals(85 - 18, wiku.getNyawa());
        assertEquals(90 - 17, telikSandi.getNyawa());
        assertEquals(95 - 16, balian.getNyawa());
        assertEquals(90, calonArang.getEnergiBatin());
        assertTrue(narasi.contains("SEBAR WABAH!"), narasi);
    }

    @Test
    @DisplayName("Ucapan nyawa rendah muncul tepat sekali saat nyawa <= 30% (84 dari 280)")
    void ucapanNyawaRendahSekali() {
        calonArang.terimaSerangan(193);                 // 187 kerusakan -> 93
        String belum = senopati.serang(calonArang);     // 9 kerusakan  -> 84
        assertTrue(calonArang.isNyawaRendah());
        assertTrue(belum.contains("Apa lagi yang tersisa untukku?"), belum);

        String sesudah = senopati.serang(calonArang);   // -> 75
        assertFalse(sesudah.contains("Apa lagi yang tersisa untukku?"), "Ucapan tidak diulang");
    }

    @Test
    void diAtasAmbangTidakBerucap() {
        calonArang.terimaSerangan(190);                 // -> 96
        String narasi = senopati.serang(calonArang);    // -> 87
        assertFalse(calonArang.isNyawaRendah());
        assertFalse(narasi.contains("Calon Arang: \""), narasi);
    }

    @Test
    void pukulanTerakhirMenampilkanNarasiGugurBukanUcapan() {
        calonArang.terimaSerangan(281);                 // 275 kerusakan -> 5, belum sempat berucap
        String narasi = senopati.serang(calonArang);    // 9 kerusakan  -> 0
        assertFalse(calonArang.isHidup());
        assertTrue(narasi.contains("Cahayanya meredup"), narasi);
        assertFalse(narasi.contains("Apa lagi yang tersisa untukku?"), narasi);
    }

    @Test
    @DisplayName("Polymorphism: satu daftar KarakterMitos, setiap serang() bernarasi sesuai kelasnya")
    void polymorphism() {
        List<KarakterMitos> semua = new ArrayList<>(
                List.of(senopati, wiku, telikSandi, balian, new Leak("Leak Setra"), calonArang,
                        new Pengikut("Pengikut Girah"), new PemimpinPengikut()));
        KarakterMitos samsak = new CalonArang();

        List<String> narasi = new ArrayList<>();
        for (KarakterMitos k : semua) {
            narasi.add(k.serang(samsak));
        }

        for (int i = 0; i < semua.size(); i++) {
            assertTrue(narasi.get(i).startsWith(semua.get(i).getNama()),
                    "Narasi harus dibuka nama pelaku: " + narasi.get(i));
        }
        assertEquals(narasi.size(), narasi.stream().distinct().count(),
                "Setiap kelas punya kalimat serangan sendiri");
    }
}
