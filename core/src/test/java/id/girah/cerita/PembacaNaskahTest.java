package id.girah.cerita;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PembacaNaskahTest {

    /** File asli dari prototipe front end (assets/cerita), disalin apa adanya ke test resources. */
    static Naskah contoh(String nama) throws Exception {
        return PembacaNaskah.bacaFile(Path.of(PembacaNaskahTest.class.getResource("/cerita/" + nama + ".json").toURI()));
    }

    static Naskah baca(String json) throws NaskahTidakValidException {
        return PembacaNaskah.baca("uji", json);
    }

    @Nested
    @DisplayName("Naskah front end yang sudah ada tetap terbaca")
    class KompatibelFrontEnd {

        @Test
        void babak1() throws Exception {
            Naskah n = contoh("contoh_babak1");
            assertEquals("contoh_babak1", n.getNama());
            assertEquals("BABAK 1 - BISIK-BISIK GIRAH", n.getJudul());
            assertEquals("Misi: menghadap Raja", n.getMisi());
            assertEquals("balai_daha", n.getArena());
            assertNull(n.getMode());
            assertEquals("hutan", n.getLanjutan());
            assertEquals(4, n.getLangkah().size());

            BarisDialog narator = (BarisDialog) n.getLangkah().get(0);
            assertTrue(narator.isNarator());
            assertEquals("", narator.getNama());
            assertEquals("netral", narator.getEkspresi(), "Bawaan sama dengan StoryLayer");

            BarisDialog juruMantra = (BarisDialog) n.getLangkah().get(1);
            assertEquals("juru_mantra", juruMantra.getIdPembicara());
            assertEquals("Juru Mantra", juruMantra.getNama());
            assertTrue(juruMantra.getSyarat().isSelalu());
        }

        @Test
        void babak2DenganPilihan() throws Exception {
            Naskah n = contoh("contoh_babak2");
            assertEquals(7, n.getLangkah().size());
            assertNull(n.getLanjutan());

            Pilihan p = (Pilihan) n.getLangkah().get(6);
            assertEquals(2, p.getOpsi().size());
            assertEquals("Mendahulukan Girah", p.getOpsi().get(0).getTeks());
            assertEquals(2, p.getOpsi().get(0).getBalasan().size());
            assertEquals(3, p.getOpsi().get(1).getBalasan().size());
            assertTrue(p.getOpsi().get(0).getEfek().isKosong(), "Tanpa \"efek\" = tidak mengubah status");
            assertTrue(p.getOpsi().get(1).getSyarat().isSelalu());
        }

        @Test
        void girahTanpaLanjutan() throws Exception {
            Naskah n = contoh("contoh_girah");
            assertEquals(2, n.getLangkah().size());
            assertEquals("girah", n.getArena());
            assertNull(n.getLanjutan());
        }
    }

    @Nested
    @DisplayName("Tambahan efek dan syarat")
    class EfekDanSyarat {

        @Test
        void efekPilihan() throws NaskahTidakValidException {
            Naskah n = baca("{\"lines\": [{\"choice\": [\"Girah\", \"Tapak Wangi\"],"
                    + " \"efek\": [{\"kepercayaanRatna\": 1, \"tandai\": [\"penawar_girah_dulu\"]},"
                    + "            {\"kepercayaanRatna\": -1, \"kepercayaanTapakWangi\": 2, \"tandai\": [\"penawar_tapak_wangi_dulu\"]}]}]}");
            Pilihan p = (Pilihan) n.getLangkah().get(0);

            Efek a = p.getOpsi().get(0).getEfek();
            assertEquals(Map.of(NilaiCerita.KEPERCAYAAN_RATNA, 1), a.getTambahan());
            assertEquals(Set.of(PenandaCerita.PENAWAR_GIRAH_DULU), a.getTandai());

            Efek b = p.getOpsi().get(1).getEfek();
            assertEquals(Map.of(NilaiCerita.KEPERCAYAAN_RATNA, -1, NilaiCerita.KEPERCAYAAN_TAPAK_WANGI, 2), b.getTambahan());
            assertTrue(p.getOpsi().get(0).getBalasan().isEmpty(), "\"replies\" boleh tidak ada");
        }

        @Test
        void syaratOpsiDanBaris() throws NaskahTidakValidException {
            Naskah n = baca("{\"lines\": ["
                    + "{\"who\": \"ratna\", \"text\": \"Aku percaya kalian.\", \"syarat\": {\"kepercayaanRatna\": 3}},"
                    + "{\"choice\": [\"Serahkan\", \"Pulihkan\"], \"efek\": [null, {\"tandai\": [\"pilih_jalan_pemulihan\"]}],"
                    + " \"syarat\": [null, {\"jalanPemulihanTerbuka\": true, \"tanpaPenanda\": [\"kitab_diambil_diam_diam\"],"
                    + "                    \"kurangDari\": {\"wargaSelamat\": 100}, \"penanda\": [\"bahula_jujur\"],"
                    + "                    \"weningSelamat\": false, \"ending\": \"JALAN_PEMULIHAN\"}]}]}");

            assertFalse(n.getLangkah().get(0).getSyarat().isSelalu());
            Pilihan p = (Pilihan) n.getLangkah().get(1);
            assertTrue(p.getOpsi().get(0).getSyarat().isSelalu(), "null = selalu tersedia");
            assertTrue(p.getOpsi().get(0).getEfek().isKosong(), "null = tanpa efek");
            assertFalse(p.getOpsi().get(1).getSyarat().isSelalu());
        }

        @Test
        void pilihanBersarangDiDalamBalasan() throws NaskahTidakValidException {
            Naskah n = baca("{\"lines\": [{\"choice\": [\"A\"], \"replies\": [[{\"choice\": [\"A1\", \"A2\"]}]]}]}");
            Pilihan luar = (Pilihan) n.getLangkah().get(0);
            assertInstanceOf(Pilihan.class, luar.getOpsi().get(0).getBalasan().get(0));
        }
    }

    @Nested
    @DisplayName("Kesalahan naskah ditolak dengan letak yang jelas")
    class Kesalahan {

        private NaskahTidakValidException gagal(String json) {
            return assertThrows(NaskahTidakValidException.class, () -> baca(json));
        }

        @Test
        void bukanJson() {
            NaskahTidakValidException e = gagal("{\"lines\": [ {\"text\": \"a\" ");
            assertEquals("uji", e.getNamaNaskah());
            assertTrue(e.getMessage().contains("bukan JSON yang sah"), e.getMessage());
        }

        @Test
        void akarBukanObjek() {
            assertTrue(gagal("[1, 2]").getMessage().contains("objek JSON"));
        }

        @Test
        void tanpaLines() {
            assertEquals("lines", gagal("{\"title\": \"X\"}").getLetak());
        }

        @Test
        void kunciAkarSalahKetik() {
            NaskahTidakValidException e = gagal("{\"titel\": \"X\", \"lines\": []}");
            assertEquals("titel", e.getLetak());
            assertTrue(e.getMessage().contains("kunci tidak dikenal"), e.getMessage());
        }

        @Test
        void kunciBarisSalahKetik() {
            assertEquals("lines[0].txt", gagal("{\"lines\": [{\"who\": \"ratna\", \"txt\": \"Halo\"}]}").getLetak());
        }

        @Test
        void teksKosong() {
            assertEquals("lines[1].text", gagal("{\"lines\": [{\"text\": \"a\"}, {\"text\": \"   \"}]}").getLetak());
        }

        @Test
        void pembicaraKosong() {
            assertEquals("lines[0].who", gagal("{\"lines\": [{\"who\": \"\", \"text\": \"a\"}]}").getLetak());
        }

        @Test
        @DisplayName("Penanda salah ketik ditolak beserta daftar penanda yang dikenal")
        void penandaSalahKetik() {
            NaskahTidakValidException e = gagal(
                    "{\"lines\": [{\"choice\": [\"A\"], \"efek\": [{\"tandai\": [\"bahula_jujr\"]}]}]}");
            assertEquals("lines[0].efek[0].tandai[0]", e.getLetak());
            assertTrue(e.getMessage().contains("\"bahula_jujr\""), e.getMessage());
            assertTrue(e.getMessage().contains("bahula_jujur"), e.getMessage());
        }

        @Test
        void nilaiTidakDikenal() {
            assertEquals("lines[0].efek[0].kepercayaanBahula",
                    gagal("{\"lines\": [{\"choice\": [\"A\"], \"efek\": [{\"kepercayaanBahula\": 1}]}]}").getLetak());
        }

        @Test
        void nilaiBukanBilanganBulat() {
            assertEquals("lines[0].efek[0].kepercayaanRatna",
                    gagal("{\"lines\": [{\"choice\": [\"A\"], \"efek\": [{\"kepercayaanRatna\": 1.5}]}]}").getLetak());
            assertEquals("lines[0].efek[0].kepercayaanRatna",
                    gagal("{\"lines\": [{\"choice\": [\"A\"], \"efek\": [{\"kepercayaanRatna\": \"1\"}]}]}").getLetak());
        }

        @Test
        @DisplayName("efek, syarat, dan replies harus sejajar dengan choice")
        void panjangTidakSejajar() {
            assertEquals("lines[0].efek",
                    gagal("{\"lines\": [{\"choice\": [\"A\", \"B\"], \"efek\": [{}]}]}").getLetak());
            assertEquals("lines[0].syarat",
                    gagal("{\"lines\": [{\"choice\": [\"A\", \"B\"], \"syarat\": [null, null, null]}]}").getLetak());
            assertEquals("lines[0].replies",
                    gagal("{\"lines\": [{\"choice\": [\"A\", \"B\"], \"replies\": [[]]}]}").getLetak());
        }

        @Test
        void pilihanKosong() {
            assertEquals("lines[0].choice", gagal("{\"lines\": [{\"choice\": []}]}").getLetak());
            assertEquals("lines[0].choice[1]", gagal("{\"lines\": [{\"choice\": [\"A\", \"\"]}]}").getLetak());
        }

        @Test
        void kunciGanda() {
            NaskahTidakValidException e = gagal("{\"lines\": [{\"text\": \"a\", \"text\": \"b\"}]}");
            assertEquals("lines[0].text", e.getLetak());
            assertTrue(e.getMessage().contains("dua kali"), e.getMessage());
        }

        @Test
        void endingTidakDikenal() {
            assertEquals("lines[0].syarat.ending",
                    gagal("{\"lines\": [{\"text\": \"a\", \"syarat\": {\"ending\": \"BAHAGIA\"}}]}").getLetak());
        }

        @Test
        void kurangDariHarusObjek() {
            assertEquals("lines[0].syarat.kurangDari",
                    gagal("{\"lines\": [{\"text\": \"a\", \"syarat\": {\"kurangDari\": 3}}]}").getLetak());
        }

        @Test
        void syaratBooleanHarusBoolean() {
            assertEquals("lines[0].syarat.weningSelamat",
                    gagal("{\"lines\": [{\"text\": \"a\", \"syarat\": {\"weningSelamat\": \"ya\"}}]}").getLetak());
        }

        @Test
        void kesalahanDiDalamBalasanMenyebutLetakLengkap() {
            assertEquals("lines[0].replies[1][0].text",
                    gagal("{\"lines\": [{\"choice\": [\"A\", \"B\"], \"replies\": [[], [{\"who\": \"ratna\"}]]}]}").getLetak());
        }
    }

    @Test
    void bacaFileMemakaiNamaFile(@TempDir Path folder) throws IOException, NaskahTidakValidException {
        Path file = Files.writeString(folder.resolve("babak3.json"), "{\"lines\": [{\"text\": \"Lemah Tulis.\"}]}");
        Naskah n = PembacaNaskah.bacaFile(file);
        assertEquals("babak3", n.getNama());
        assertEquals("", n.getJudul());
        assertEquals("Lemah Tulis.", ((BarisDialog) n.getLangkah().get(0)).getTeks());
    }

    @Test
    void naskahYangSalahDariFileMenyebutNamaFile(@TempDir Path folder) throws IOException {
        Path file = Files.writeString(folder.resolve("babak4.json"), "{\"lines\": [{\"txt\": \"a\"}]}");
        NaskahTidakValidException e = assertThrows(NaskahTidakValidException.class, () -> PembacaNaskah.bacaFile(file));
        assertTrue(e.getMessage().startsWith("Naskah 'babak4' di lines[0].txt"), e.getMessage());
    }

}
