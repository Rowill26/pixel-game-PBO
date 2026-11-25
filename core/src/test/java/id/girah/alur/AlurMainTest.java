package id.girah.alur;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import id.girah.ai.KeputusanAksi;
import id.girah.ai.StrategiDasar;
import id.girah.battle.BattleManager;
import id.girah.battle.StatusBattle;
import id.girah.cerita.NilaiCerita;
import id.girah.cerita.Naskah;
import id.girah.cerita.PembacaNaskah;
import id.girah.cerita.PemutarNaskah;
import id.girah.cerita.PenandaCerita;
import id.girah.cerita.StatusCerita;
import id.girah.karakter.EnergiBatinHabisException;
import id.girah.karakter.KarakterMitos;
import id.girah.karakter.MakhlukGirah;
import id.girah.pusaka.TasPusaka;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Memainkan alur cerita dari file asli di {@code assets/}: mulai dari {@link AlurCerita#NASKAH_AWAL},
 * mengikuti "then" dan "menang", menjalankan pertarungan dengan aturan AI sungguhan.
 * Pemain di sini hanya memakai serangan biasa ke musuh dengan nyawa terendah: setiap pertarungan
 * sebelum Babak 5 harus tetap bisa dimenangkan dengan cara bermain sesederhana itu.
 */
class AlurMainTest {

    private static final Path ASSETS = Path.of("..", "assets");

    /** Urutan langkah Babak 1-4 (sama untuk semua pilihan). */
    private static final List<String> ALUR = List.of(
            "babak1_balai_daha", "babak1_tapak_wangi", "babak1_hutan", "babak1_penutup",
            "babak2_girah", "babak2_penawar",
            "babak3_istana", "babak3_lemah_tulis", "babak3_pertapaan", "babak3_penangkal", "babak3_girah",
            "babak4_rumah", "babak4_lemah_tulis", "babak4_serangan", "babak4_desa", "babak4_penutup");

    /** Hasil satu permainan: langkah yang dilalui dan status cerita di akhir. */
    private record Hasil(List<String> jejak, StatusCerita status) {
    }

    @ParameterizedTest(name = "acak {0}")
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20})
    @DisplayName("Babak 1-4 bisa dimainkan dari awal sampai akhir")
    void mainkanSemuaBabak(int acak) throws Exception {
        assumeTrue(Files.isDirectory(ASSETS.resolve("cerita")), "assets/cerita belum ada");
        Hasil h = mainkan(new Random(acak), false);
        assertEquals(ALUR, h.jejak());
    }

    @Test
    @DisplayName("Selalu opsi pertama: jalur yang membuka Jalan Pemulihan")
    void opsiPertamaMembukaJalanPemulihan() throws Exception {
        assumeTrue(Files.isDirectory(ASSETS.resolve("cerita")), "assets/cerita belum ada");
        StatusCerita s = mainkan(new Random(1), false).status();
        assertTrue(s.punya(PenandaCerita.PENAWAR_GIRAH_DULU));
        assertTrue(s.punya(PenandaCerita.DUKUNG_BAHULA_JUJUR));
        assertTrue(s.punya(PenandaCerita.BAHULA_JUJUR));
        assertTrue(s.punya(PenandaCerita.RATNA_MEMBANTU_KITAB));
        assertFalse(s.punya(PenandaCerita.KITAB_DIAMBIL_DIAM_DIAM));
        assertEquals(4, s.getNilai(NilaiCerita.KEPERCAYAAN_RATNA));       // +1 Girah, +1 Bahula, +2 jujur
        assertEquals(3, s.getNilai(NilaiCerita.WARGA_SELAMAT));            // Ratna ikut membantu evakuasi
    }

    @Test
    @DisplayName("Selalu opsi terakhir: Wening selamat, kitab dicuri, Jalan Pemulihan tertutup")
    void opsiTerakhirMenutupJalanPemulihan() throws Exception {
        assumeTrue(Files.isDirectory(ASSETS.resolve("cerita")), "assets/cerita belum ada");
        StatusCerita s = mainkan(new Random(1), true).status();
        assertTrue(s.punya(PenandaCerita.PENAWAR_TAPAK_WANGI_DULU));
        assertFalse(s.punya(PenandaCerita.DUKUNG_BAHULA_JUJUR));
        assertTrue(s.punya(PenandaCerita.KITAB_DIAMBIL_DIAM_DIAM));
        assertFalse(s.punya(PenandaCerita.RATNA_MEMBANTU_KITAB));
        assertEquals(-3, s.getNilai(NilaiCerita.KEPERCAYAAN_RATNA));      // -1 Tapak Wangi, -2 diam-diam
        assertEquals(2, s.getNilai(NilaiCerita.WARGA_SELAMAT));            // jalan timur
    }

    private static Hasil mainkan(Random acak, boolean opsiTerakhir) throws Exception {
        StatusCerita status = new StatusCerita();
        TasPusaka tas = new TasPusaka();
        List<String> jejak = new ArrayList<>();
        String langkah = AlurCerita.NASKAH_AWAL;
        while (langkah != null) {
            assertTrue(jejak.size() < 100, "Alur berputar-putar: " + jejak);
            jejak.add(langkah);
            Path fileNaskah = ASSETS.resolve("cerita").resolve(langkah + ".json");
            if (Files.isRegularFile(fileNaskah)) {
                langkah = mainkanNaskah(PembacaNaskah.bacaFile(fileNaskah), status, opsiTerakhir);
            } else {
                RencanaPertarungan r = PembacaPertarungan.bacaFile(ASSETS.resolve("pertarungan").resolve(langkah + ".json"));
                BattleManager battle = r.buatBattle(tas);
                boolean adaBos = battle.getLawan().stream().anyMatch(k -> ((MakhlukGirah) k).isBos());
                assertEquals(StatusBattle.MENANG, mainkanPertarungan(battle, acak, adaBos),
                        "Pertarungan " + r.getId() + " kalah; babad:\n" + battle.getBabad().getIsi());
                langkah = r.getLanjutanMenang();
            }
        }
        return new Hasil(jejak, status);
    }

    /** Membaca semua baris; pilihan selalu opsi pertama atau terakhir yang tersedia. Mengembalikan "then". */
    private static String mainkanNaskah(Naskah naskah, StatusCerita status, boolean opsiTerakhir) {
        PemutarNaskah p = new PemutarNaskah(naskah, status);
        for (int i = 0; !p.isSelesai(); i++) {
            assertTrue(i < 500, "Naskah tidak selesai: " + naskah.getNama());
            if (p.isPilihan()) {
                p.pilih(opsiTerakhir ? p.getOpsiTersedia().size() - 1 : 0);
            } else {
                assertFalse(p.getBaris().getTeks().isBlank());
                p.lanjut();
            }
        }
        return p.getLanjutan();
    }

    /**
     * Pemain: serang lawan dengan nyawa terendah. Melawan bos ({@code pakaiJurus}), jurus dipakai
     * selama energi cukup: ke kawan dengan nyawa terendah jika jurusnya menyasar kawan, selain itu ke
     * lawan dengan nyawa terendah. Musuh: StrategiDasar, dijalankan seperti PengendaliMusuh.
     */
    private static StatusBattle mainkanPertarungan(BattleManager battle, Random acak, boolean pakaiJurus) {
        StrategiDasar strategi = new StrategiDasar(acak);
        battle.mulai();
        for (int i = 0; !battle.getStatus().isSelesai(); i++) {
            assertTrue(i < 1000, "Pertarungan tidak selesai");
            KarakterMitos aktor = battle.getAktor();
            if (battle.getStatus() == StatusBattle.GILIRAN_PEMAIN) {
                if (pakaiJurus && aktor.bisaMemakaiJurus() && !battle.getSasaranJurus().isEmpty()) {
                    try {
                        battle.gunakanJurus(battle.getSasaranJurus().stream()
                                .min(Comparator.comparingInt(k -> k.getNyawa() * 100 / k.getNyawaMaks())).orElseThrow());
                        continue;
                    } catch (EnergiBatinHabisException e) {
                        // jatuh ke serangan biasa
                    }
                }
                battle.serang(battle.getSasaranSerang().stream()
                        .min(Comparator.comparingInt(KarakterMitos::getNyawa)).orElseThrow());
                continue;
            }
            KeputusanAksi k = strategi.tentukan(aktor, battle.getSasaranSerang(), battle.getSasaranJurus());
            try {
                if (k.getJenis() == KeputusanAksi.Jenis.JURUS) {
                    battle.gunakanJurus(k.getTarget());
                } else {
                    battle.serang(k.getTarget());
                }
            } catch (EnergiBatinHabisException | IllegalArgumentException e) {
                battle.serang(battle.getSasaranSerang().get(0));
            }
        }
        return battle.getStatus();
    }
}
