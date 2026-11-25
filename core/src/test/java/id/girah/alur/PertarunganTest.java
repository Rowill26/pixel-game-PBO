package id.girah.alur;

import static org.junit.jupiter.api.Assertions.*;

import id.girah.battle.BattleManager;
import id.girah.karakter.Balian;
import id.girah.karakter.CalonArang;
import id.girah.karakter.KarakterMitos;
import id.girah.karakter.Leak;
import id.girah.karakter.MakhlukGirah;
import id.girah.karakter.PemimpinPengikut;
import id.girah.karakter.Pengelana;
import id.girah.karakter.Pengikut;
import id.girah.karakter.Senopati;
import id.girah.karakter.TelikSandi;
import id.girah.karakter.Wiku;
import id.girah.pusaka.TasPusaka;
import id.girah.pusaka.TirtaSuci;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** PabrikKarakter, PembacaPertarungan, dan RencanaPertarungan. */
class PertarunganTest {

    @Nested
    class Pabrik {

        @Test
        @DisplayName("Kelompok: empat pengelana bernama peran, urut seperti HUD, nyawa penuh")
        void kelompok() {
            List<Pengelana> k = PabrikKarakter.buatKelompok();
            assertEquals(4, k.size());
            assertInstanceOf(Senopati.class, k.get(0));
            assertInstanceOf(Wiku.class, k.get(1));
            assertInstanceOf(Balian.class, k.get(2));
            assertInstanceOf(TelikSandi.class, k.get(3));
            for (Pengelana p : k) {
                assertEquals(p.getPeran(), p.getNama());
                assertEquals(p.getNyawaMaks(), p.getNyawa());
            }
            assertNotSame(k.get(0), PabrikKarakter.buatKelompok().get(0), "Setiap panggilan membuat objek baru");
        }

        @Test
        @DisplayName("Jenis musuh = kunci sprite")
        void musuh() {
            for (String jenis : PabrikKarakter.JENIS_MUSUH) {
                assertEquals(jenis, PabrikKarakter.buatMusuh(jenis, null).getIdSprite());
            }
            assertInstanceOf(Leak.class, PabrikKarakter.buatMusuh("leak", "Leak 2"));
            assertEquals("Leak 2", PabrikKarakter.buatMusuh("leak", "Leak 2").getNama());
            assertEquals("Leak", PabrikKarakter.buatMusuh("leak", null).getNama());
            assertInstanceOf(Pengikut.class, PabrikKarakter.buatMusuh("pengikut", null));
            assertInstanceOf(PemimpinPengikut.class, PabrikKarakter.buatMusuh("pemimpin_pengikut", null));
            assertInstanceOf(CalonArang.class, PabrikKarakter.buatMusuh("calon_arang", null));
        }

        @Test
        void musuhTidakSah() {
            assertThrows(IllegalArgumentException.class, () -> PabrikKarakter.buatMusuh("rangda", null));
            assertThrows(IllegalArgumentException.class, () -> PabrikKarakter.buatMusuh("calon_arang", "Ibu"));
            assertThrows(NullPointerException.class, () -> PabrikKarakter.buatMusuh(null, null));
        }
    }

    @Nested
    class Pembaca {

        private RencanaPertarungan baca(String json) throws PertarunganTidakValidException {
            return PembacaPertarungan.baca("uji", json);
        }

        private PertarunganTidakValidException gagal(String json) {
            return assertThrows(PertarunganTidakValidException.class, () -> baca(json));
        }

        @Test
        void lengkap() throws PertarunganTidakValidException {
            RencanaPertarungan r = baca("{\"arena\": \"pemakaman\", \"mode\": \"night\", \"batasRonde\": 8,"
                    + " \"musuh\": [{\"jenis\": \"calon_arang\"}, {\"jenis\": \"leak\", \"nama\": \"Leak Setra\"}],"
                    + " \"menang\": \"babak5_akhir\"}");
            assertEquals("uji", r.getId());
            assertEquals("pemakaman", r.getArena());
            assertEquals("night", r.getMode());
            assertEquals(8, r.getBatasRonde());
            assertEquals("babak5_akhir", r.getLanjutanMenang());
            assertEquals("calon_arang", r.getMusuh().get(0).getJenis());
            assertNull(r.getMusuh().get(0).getNama());
            assertEquals("Leak Setra", r.getMusuh().get(1).getNama());
        }

        @Test
        void bawaan() throws PertarunganTidakValidException {
            RencanaPertarungan r = baca("{\"arena\": \"hutan\", \"musuh\": [{\"jenis\": \"leak\"}]}");
            assertNull(r.getMode());
            assertEquals(BattleManager.TANPA_BATAS_RONDE, r.getBatasRonde());
            assertNull(r.getLanjutanMenang());
        }

        @Test
        void kesalahan() {
            assertTrue(gagal("[]").getMessage().contains("objek JSON"));
            assertTrue(gagal("{\"arena\": \"hutan\"").getMessage().contains("bukan JSON"));
            assertEquals("arena", gagal("{\"musuh\": [{\"jenis\": \"leak\"}]}").getLetak());
            assertEquals("musuh", gagal("{\"arena\": \"hutan\", \"musuh\": []}").getLetak());
            assertEquals("lawan", gagal("{\"arena\": \"hutan\", \"lawan\": []}").getLetak());
            assertEquals("musuh[0].jenis",
                    gagal("{\"arena\": \"hutan\", \"musuh\": [{\"jenis\": \"leek\"}]}").getLetak());
            assertEquals("musuh[0].nyawa",
                    gagal("{\"arena\": \"hutan\", \"musuh\": [{\"jenis\": \"leak\", \"nyawa\": 5}]}").getLetak());
            assertEquals("musuh[0].nama",
                    gagal("{\"arena\": \"hutan\", \"musuh\": [{\"jenis\": \"calon_arang\", \"nama\": \"Ibu\"}]}").getLetak());
            assertEquals("batasRonde",
                    gagal("{\"arena\": \"hutan\", \"batasRonde\": -1, \"musuh\": [{\"jenis\": \"leak\"}]}").getLetak());
            assertEquals("batasRonde",
                    gagal("{\"arena\": \"hutan\", \"batasRonde\": 2.5, \"musuh\": [{\"jenis\": \"leak\"}]}").getLetak());
        }

        @Test
        @DisplayName("Nama musuh ganda ditolak, termasuk nama bawaan yang sama")
        void namaGanda() {
            PertarunganTidakValidException e = gagal("{\"arena\": \"hutan\","
                    + " \"musuh\": [{\"jenis\": \"leak\"}, {\"jenis\": \"leak\"}]}");
            assertEquals("musuh[1].nama", e.getLetak());
            assertTrue(e.getMessage().contains("\"Leak\" sudah dipakai"), e.getMessage());
        }
    }

    @Test
    @DisplayName("buatBattle: karakter baru setiap kali, Tas Pusaka yang sama, batas ronde terbawa")
    void buatBattle() throws PertarunganTidakValidException {
        RencanaPertarungan r = PembacaPertarungan.baca("uji", "{\"arena\": \"hutan\", \"batasRonde\": 5,"
                + " \"musuh\": [{\"jenis\": \"leak\", \"nama\": \"Leak 1\"}, {\"jenis\": \"pengikut\"}]}");
        TasPusaka tas = new TasPusaka();
        tas.tambah(new TirtaSuci(), 1);

        BattleManager pertama = r.buatBattle(tas);
        BattleManager ulang = r.buatBattle(tas);

        assertEquals(4, pertama.getKelompok().size());
        List<KarakterMitos> lawan = pertama.getLawan();
        assertEquals(List.of("Leak 1", "Pengikut"), List.of(lawan.get(0).getNama(), lawan.get(1).getNama()));
        assertEquals(5, pertama.getBatasRonde());
        assertSame(tas, pertama.getTasPusaka());
        assertSame(tas, ulang.getTasPusaka());
        assertNotSame(pertama.getLawan().get(0), ulang.getLawan().get(0), "Mengulang = musuh baru dengan nyawa penuh");
        assertNotSame(pertama.getKelompok().get(0), ulang.getKelompok().get(0));

        List<MakhlukGirah> musuh = r.buatMusuh();
        assertEquals(2, musuh.size());
    }
}
