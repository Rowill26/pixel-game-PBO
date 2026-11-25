package id.girah.alur;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import id.girah.ai.KeputusanAksi;
import id.girah.ai.StrategiDasar;
import id.girah.battle.BattleManager;
import id.girah.battle.StatusBattle;
import id.girah.cerita.Naskah;
import id.girah.cerita.PembacaNaskah;
import id.girah.cerita.PemutarNaskah;
import id.girah.cerita.StatusCerita;
import id.girah.karakter.EnergiBatinHabisException;
import id.girah.karakter.KarakterMitos;
import id.girah.pusaka.TasPusaka;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Memainkan Babak 1 dari file asli di {@code assets/}: mulai dari {@link AlurCerita#NASKAH_AWAL},
 * mengikuti "then" dan "menang", menjalankan pertarungan dengan aturan AI sungguhan.
 * Pemain di sini hanya memakai serangan biasa ke musuh dengan nyawa terendah: pertarungan
 * tutorial harus tetap bisa dimenangkan dengan cara bermain sesederhana itu.
 */
class Babak1MainTest {

    private static final Path ASSETS = Path.of("..", "assets");

    @ParameterizedTest(name = "acak {0}")
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20})
    @DisplayName("Babak 1 bisa dimainkan dari awal sampai akhir")
    void mainkanBabak1(int acak) throws Exception {
        assumeTrue(Files.isDirectory(ASSETS.resolve("cerita")), "assets/cerita belum ada");

        StatusCerita status = new StatusCerita();
        TasPusaka tas = new TasPusaka();
        List<String> jejak = new ArrayList<>();

        String langkah = AlurCerita.NASKAH_AWAL;
        while (langkah != null) {
            assertTrue(jejak.size() < 50, "Alur berputar-putar: " + jejak);
            jejak.add(langkah);
            Path fileNaskah = ASSETS.resolve("cerita").resolve(langkah + ".json");
            if (Files.isRegularFile(fileNaskah)) {
                langkah = mainkanNaskah(PembacaNaskah.bacaFile(fileNaskah), status);
            } else {
                RencanaPertarungan r = PembacaPertarungan.bacaFile(ASSETS.resolve("pertarungan").resolve(langkah + ".json"));
                BattleManager battle = r.buatBattle(tas);
                assertEquals(StatusBattle.MENANG, mainkanPertarungan(battle, new Random(acak)),
                        "Pertarungan " + r.getId() + " kalah; babad:\n" + battle.getBabad().getIsi());
                langkah = r.getLanjutanMenang();
            }
        }

        assertEquals(List.of("babak1_balai_daha", "babak1_tapak_wangi", "babak1_hutan", "babak1_penutup"), jejak);
    }

    /** Membaca semua baris; pilihan (jika ada) selalu opsi pertama. Mengembalikan "then". */
    private static String mainkanNaskah(Naskah naskah, StatusCerita status) {
        PemutarNaskah p = new PemutarNaskah(naskah, status);
        for (int i = 0; !p.isSelesai(); i++) {
            assertTrue(i < 500, "Naskah tidak selesai: " + naskah.getNama());
            if (p.isPilihan()) {
                p.pilih(0);
            } else {
                assertFalse(p.getBaris().getTeks().isBlank());
                p.lanjut();
            }
        }
        return p.getLanjutan();
    }

    /** Pemain: serang lawan dengan nyawa terendah. Musuh: StrategiDasar, dijalankan seperti PengendaliMusuh. */
    private static StatusBattle mainkanPertarungan(BattleManager battle, Random acak) {
        StrategiDasar strategi = new StrategiDasar(acak);
        battle.mulai();
        for (int i = 0; !battle.getStatus().isSelesai(); i++) {
            assertTrue(i < 1000, "Pertarungan tidak selesai");
            KarakterMitos aktor = battle.getAktor();
            if (battle.getStatus() == StatusBattle.GILIRAN_PEMAIN) {
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
