package id.girah.cerita;

import static org.junit.jupiter.api.Assertions.*;

import id.girah.battle.StatusBattle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PemutarNaskahTest {

    private StatusCerita status;

    @BeforeEach
    void siapkan() {
        status = new StatusCerita();
    }

    private PemutarNaskah pemutar(String json) throws NaskahTidakValidException {
        return new PemutarNaskah(PembacaNaskah.baca("uji", json), status);
    }

    /** Membaca semua baris dialog sampai bertemu pilihan atau naskah selesai. */
    private static List<String> bacaSampaiPilihan(PemutarNaskah p) {
        List<String> teks = new ArrayList<>();
        while (!p.isSelesai() && !p.isPilihan()) {
            teks.add(p.getBaris().getTeks());
            p.lanjut();
        }
        return teks;
    }

    @Test
    @DisplayName("Naskah Babak 1 front end: empat baris, lalu lanjut ke arena hutan")
    void naskahTanpaPilihan() throws Exception {
        PemutarNaskah p = new PemutarNaskah(PembacaNaskahTest.contoh("contoh_babak1"), status);
        List<String> teks = bacaSampaiPilihan(p);
        assertEquals(4, teks.size());
        assertTrue(teks.get(0).startsWith("Malam di Balai Kerajaan Daha."), teks.get(0));
        assertTrue(p.isSelesai());
        assertEquals("hutan", p.getLanjutan());
    }

    @Test
    @DisplayName("Naskah Babak 2 front end: balasan opsi yang dipilih tampil, lalu naskah selesai")
    void naskahDenganPilihan() throws Exception {
        PemutarNaskah p = new PemutarNaskah(PembacaNaskahTest.contoh("contoh_babak2"), status);
        assertEquals(6, bacaSampaiPilihan(p).size());
        assertTrue(p.isPilihan());
        assertEquals(2, p.getOpsiTersedia().size());

        p.pilih(1);                                         // Mendahulukan Tapak Wangi

        List<String> balasan = bacaSampaiPilihan(p);
        assertEquals(3, balasan.size());
        assertEquals("Kepercayaan Ratna berkurang.", balasan.get(2));
        assertTrue(p.isSelesai());
    }

    @Test
    @DisplayName("Efek pilihan diterapkan ke status cerita")
    void efekDiterapkan() throws NaskahTidakValidException {
        PemutarNaskah p = pemutar("{\"lines\": [{\"choice\": [\"Girah\", \"Tapak Wangi\"],"
                + " \"efek\": [{\"kepercayaanRatna\": 1, \"tandai\": [\"penawar_girah_dulu\"]},"
                + "            {\"kepercayaanRatna\": -1, \"tandai\": [\"penawar_tapak_wangi_dulu\"]}]}]}");
        p.pilih(1);
        assertEquals(-1, status.getNilai(NilaiCerita.KEPERCAYAAN_RATNA));
        assertTrue(status.punya(PenandaCerita.PENAWAR_TAPAK_WANGI_DULU));
        assertFalse(status.punya(PenandaCerita.PENAWAR_GIRAH_DULU));
        assertTrue(p.isSelesai());
    }

    @Test
    @DisplayName("Balasan tampil sebelum sisa naskah, termasuk pilihan bersarang")
    void balasanSebelumSisaNaskah() throws NaskahTidakValidException {
        PemutarNaskah p = pemutar("{\"lines\": ["
                + "{\"choice\": [\"A\"], \"replies\": [[{\"text\": \"balas 1\"}, {\"choice\": [\"A1\", \"A2\"],"
                + "   \"replies\": [[{\"text\": \"balas A1\"}], [{\"text\": \"balas A2\"}]]}, {\"text\": \"balas 2\"}]]},"
                + "{\"text\": \"sisa naskah\"}]}");
        p.pilih(0);
        assertEquals(List.of("balas 1"), bacaSampaiPilihan(p));
        p.pilih(1);
        assertEquals(List.of("balas A2", "balas 2", "sisa naskah"), bacaSampaiPilihan(p));
        assertTrue(p.isSelesai());
    }

    @Test
    @DisplayName("Baris dengan syarat yang tidak terpenuhi dilewati")
    void barisBersyarat() throws NaskahTidakValidException {
        String json = "{\"lines\": ["
                + "{\"text\": \"awal\"},"
                + "{\"who\": \"ratna\", \"text\": \"Aku percaya kalian.\", \"syarat\": {\"kepercayaanRatna\": 3}},"
                + "{\"who\": \"ratna\", \"text\": \"Aku belum yakin.\", \"syarat\": {\"kurangDari\": {\"kepercayaanRatna\": 3}}},"
                + "{\"text\": \"akhir\"}]}";

        assertEquals(List.of("awal", "Aku belum yakin.", "akhir"), bacaSampaiPilihan(pemutar(json)));

        status.tambahNilai(NilaiCerita.KEPERCAYAAN_RATNA, 3);
        assertEquals(List.of("awal", "Aku percaya kalian.", "akhir"), bacaSampaiPilihan(pemutar(json)));
    }

    @Test
    @DisplayName("Efek pilihan langsung memengaruhi syarat baris berikutnya")
    void efekMemengaruhiBarisBerikutnya() throws NaskahTidakValidException {
        PemutarNaskah p = pemutar("{\"lines\": ["
                + "{\"choice\": [\"Jujur\", \"Diam\"], \"efek\": [{\"tandai\": [\"bahula_jujur\"]}, null]},"
                + "{\"text\": \"Ratna mendengarkan.\", \"syarat\": {\"penanda\": [\"bahula_jujur\"]}},"
                + "{\"text\": \"Ratna tidak tahu apa-apa.\", \"syarat\": {\"tanpaPenanda\": [\"bahula_jujur\"]}}]}");
        p.pilih(0);
        assertEquals(List.of("Ratna mendengarkan."), bacaSampaiPilihan(p));
    }

    @Test
    @DisplayName("Opsi dengan syarat tidak terpenuhi disembunyikan; indeks mengikuti opsi tersedia")
    void opsiBersyarat() throws NaskahTidakValidException {
        PemutarNaskah p = pemutar("{\"lines\": [{\"choice\": [\"Serahkan\", \"Pulihkan\", \"Diam\"],"
                + " \"syarat\": [null, {\"jalanPemulihanTerbuka\": true}, null],"
                + " \"replies\": [[{\"text\": \"serahkan\"}], [{\"text\": \"pulihkan\"}], [{\"text\": \"diam\"}]]}]}");
        List<OpsiPilihan> opsi = p.getOpsiTersedia();
        assertEquals(List.of("Serahkan", "Diam"), List.of(opsi.get(0).getTeks(), opsi.get(1).getTeks()));

        p.pilih(1);
        assertEquals(List.of("diam"), bacaSampaiPilihan(p), "Indeks 1 = opsi tersedia kedua (Diam)");
    }

    @Test
    void pilihanTanpaOpsiTersediaDilewati() throws NaskahTidakValidException {
        PemutarNaskah p = pemutar("{\"lines\": ["
                + "{\"choice\": [\"Pulihkan\"], \"syarat\": [{\"jalanPemulihanTerbuka\": true}]},"
                + "{\"text\": \"lanjut\"}]}");
        assertFalse(p.isPilihan());
        assertEquals(List.of("lanjut"), bacaSampaiPilihan(p));
    }

    @Test
    void penggunaanYangSalahDitolak() throws NaskahTidakValidException {
        PemutarNaskah p = pemutar("{\"lines\": [{\"choice\": [\"A\", \"B\"]}, {\"text\": \"x\"}]}");
        assertThrows(IllegalStateException.class, p::getBaris);
        assertThrows(IllegalStateException.class, p::lanjut);
        assertThrows(IndexOutOfBoundsException.class, () -> p.pilih(2));
        assertThrows(IndexOutOfBoundsException.class, () -> p.pilih(-1));
        assertTrue(p.isPilihan(), "Pilihan yang gagal tidak mengubah posisi");

        p.pilih(0);
        assertThrows(IllegalStateException.class, p::getOpsiTersedia);
        p.lanjut();
        assertTrue(p.isSelesai());
        assertThrows(IllegalStateException.class, p::getBaris);
        assertThrows(IllegalStateException.class, p::isPilihan);
    }

    @Test
    void naskahKosongLangsungSelesai() throws NaskahTidakValidException {
        assertTrue(pemutar("{\"lines\": []}").isSelesai());
    }

    @Test
    @DisplayName("Skenario Babak 5: pilihan akhir dan epilog mengikuti ending")
    void skenarioAkhir() throws NaskahTidakValidException {
        String akhir = "{\"title\": \"BABAK 5\", \"lines\": ["
                + "{\"choice\": [\"Serahkan kepada kerajaan\", \"Tempuh jalan pemulihan\"],"
                + " \"efek\": [{\"tandai\": [\"serahkan_ke_kerajaan\"]}, {\"tandai\": [\"pilih_jalan_pemulihan\"]}],"
                + " \"syarat\": [null, {\"jalanPemulihanTerbuka\": true}]},"
                + "{\"text\": \"Calon Arang dibawa ke Daha.\", \"syarat\": {\"ending\": \"PENGADILAN_KERAJAAN\"}},"
                + "{\"text\": \"Pemulihan dimulai perlahan.\", \"syarat\": {\"ending\": \"JALAN_PEMULIHAN\"}},"
                + "{\"text\": \"Janda Wening membuka pintunya.\", \"syarat\": {\"weningSelamat\": true}}]}";

        // Jalur pemulihan: Girah dulu (+1), Bahula jujur dan Ratna membantu (+2)
        status.terapkan(new Efek(Map.of(NilaiCerita.KEPERCAYAAN_RATNA, 3),
                Set.of(PenandaCerita.RATNA_MEMBANTU_KITAB)));
        status.catatHasilRitual(StatusBattle.MENANG);

        PemutarNaskah p = pemutar(akhir);
        assertEquals(2, p.getOpsiTersedia().size(), "Jalan pemulihan terbuka");
        p.pilih(1);
        assertEquals(Ending.JALAN_PEMULIHAN, PenentuEnding.tentukan(status));
        assertEquals(List.of("Pemulihan dimulai perlahan."), bacaSampaiPilihan(p));

        // Jalur lain: kepercayaan rendah -> hanya bisa menyerahkan
        status = new StatusCerita();
        status.tandai(PenandaCerita.PENAWAR_TAPAK_WANGI_DULU);
        status.catatHasilRitual(StatusBattle.MENANG);
        p = pemutar(akhir);
        assertEquals(1, p.getOpsiTersedia().size());
        p.pilih(0);
        assertEquals(List.of("Calon Arang dibawa ke Daha.", "Janda Wening membuka pintunya."), bacaSampaiPilihan(p));
    }
}
