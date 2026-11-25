package id.girah.alur;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import id.girah.ai.KeputusanAksi;
import id.girah.ai.StrategiDasar;
import id.girah.battle.BattleManager;
import id.girah.battle.StatusBattle;
import id.girah.cerita.Ending;
import id.girah.cerita.NilaiCerita;
import id.girah.cerita.Naskah;
import id.girah.cerita.PembacaNaskah;
import id.girah.cerita.PemutarNaskah;
import id.girah.cerita.PenandaCerita;
import id.girah.cerita.PenentuEnding;
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
 * tanpa bos harus tetap bisa dimenangkan dengan cara bermain sesederhana itu; pertarungan bos dan
 * ritual dimainkan dengan jurus.
 */
class AlurMainTest {

    private static final Path ASSETS = Path.of("..", "assets");

    /** Urutan langkah dari awal sampai epilog (sama untuk semua pilihan dan ending). */
    private static final List<String> ALUR = List.of(
            "babak1_balai_daha", "babak1_tapak_wangi", "babak1_hutan", "babak1_penutup",
            "babak2_girah", "babak2_penawar",
            "babak3_istana", "babak3_lemah_tulis", "babak3_pertapaan", "babak3_penangkal", "babak3_girah",
            "babak4_rumah", "babak4_lemah_tulis", "babak4_serangan", "babak4_desa", "babak4_penutup",
            "babak5_pemakaman", "babak5_penjaga", "babak5_konfrontasi", "babak5_tanding",
            "babak5_ritual_terakhir", "babak5_ritual", "babak5_akhir", "epilog");

    /** Hasil satu permainan: langkah yang dilalui, status cerita, dan baris yang tampil. */
    private record Hasil(List<String> jejak, StatusCerita status, List<String> teks) {
    }

    /** Cara memilih: opsi pertama, atau opsi terakhir yang tersedia. */
    private enum Pilih { PERTAMA, TERAKHIR }

    @ParameterizedTest(name = "acak {0}")
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20})
    @DisplayName("Seluruh alur bisa dimainkan sampai epilog, ritual dihentikan tepat waktu")
    void mainkanSemuaBabak(int acak) throws Exception {
        butuhAssets();
        Hasil h = mainkan(new Random(acak), Pilih.PERTAMA, Pilih.PERTAMA, false);
        assertEquals(ALUR, h.jejak());
        assertTrue(h.status().isRitualDihentikanTepatWaktu(), "Ritual tidak dihentikan dalam batas ronde");
    }

    @Test
    @DisplayName("Ending 2: semua pilihan mendukung Ratna, lalu memilih jalan pemulihan")
    void endingJalanPemulihan() throws Exception {
        butuhAssets();
        Hasil h = mainkan(new Random(1), Pilih.PERTAMA, Pilih.TERAKHIR, false);
        StatusCerita s = h.status();
        assertTrue(s.punya(PenandaCerita.PENAWAR_GIRAH_DULU));
        assertTrue(s.punya(PenandaCerita.DUKUNG_BAHULA_JUJUR));
        assertTrue(s.punya(PenandaCerita.BAHULA_JUJUR));
        assertTrue(s.punya(PenandaCerita.RATNA_MEMBANTU_KITAB));
        assertFalse(s.punya(PenandaCerita.KITAB_DIAMBIL_DIAM_DIAM));
        assertEquals(5, s.getNilai(NilaiCerita.KEPERCAYAAN_RATNA));       // +1 Girah, +1 Bahula, +2 jujur, +1 luka
        assertEquals(3, s.getNilai(NilaiCerita.WARGA_SELAMAT));            // Ratna ikut membantu evakuasi
        assertEquals(Ending.JALAN_PEMULIHAN, PenentuEnding.tentukan(s));
        assertTrue(h.teks().stream().anyMatch(t -> t.contains("menanam obat")), "Epilog Jalan Pemulihan tidak tampil");
        assertTrue(h.teks().stream().anyMatch(t -> t.contains("Rumah Janda Wening kini sunyi")));
    }

    @Test
    @DisplayName("Ending 1: kitab dicuri, jalan pemulihan tertutup, Calon Arang diserahkan")
    void endingPengadilanKerajaan() throws Exception {
        butuhAssets();
        Hasil h = mainkan(new Random(1), Pilih.TERAKHIR, Pilih.TERAKHIR, false);
        StatusCerita s = h.status();
        assertTrue(s.punya(PenandaCerita.PENAWAR_TAPAK_WANGI_DULU));
        assertTrue(s.punya(PenandaCerita.KITAB_DIAMBIL_DIAM_DIAM));
        assertFalse(PenentuEnding.jalanPemulihanTerbuka(s));
        assertEquals(-3, s.getNilai(NilaiCerita.KEPERCAYAAN_RATNA));      // -1 Tapak Wangi, -2 diam-diam
        assertEquals(2, s.getNilai(NilaiCerita.WARGA_SELAMAT));            // jalan timur
        assertTrue(s.punya(PenandaCerita.SERAHKAN_KE_KERAJAAN), "Opsi pemulihan seharusnya tersembunyi");
        assertEquals(Ending.PENGADILAN_KERAJAAN, PenentuEnding.tentukan(s));
        assertTrue(h.teks().stream().anyMatch(t -> t.contains("kami bisa memulai lagi")), "Wening seharusnya selamat");
    }

    @Test
    @DisplayName("Ending 3: batas ronde ritual habis, tanpa pilihan akhir")
    void endingAkhirTragis() throws Exception {
        butuhAssets();
        Hasil h = mainkan(new Random(1), Pilih.PERTAMA, Pilih.PERTAMA, true);
        StatusCerita s = h.status();
        assertEquals(ALUR, h.jejak());
        assertEquals(Ending.AKHIR_TRAGIS, PenentuEnding.tentukan(s));
        assertFalse(s.punya(PenandaCerita.SERAHKAN_KE_KERAJAAN));
        assertFalse(s.punya(PenandaCerita.PILIH_JALAN_PEMULIHAN));
        assertTrue(h.teks().stream().anyMatch(t -> t.contains("Gelombang terakhir wabah")));
    }

    private static void butuhAssets() {
        assumeTrue(Files.isDirectory(ASSETS.resolve("cerita")), "assets/cerita belum ada");
    }

    /**
     * @param pilih       cara memilih di semua naskah kecuali babak5_akhir
     * @param pilihAkhir  cara memilih di babak5_akhir (serahkan = pertama, pemulihan = terakhir)
     * @param ritualGagal {@code true}: pertarungan ritual dianggap WAKTU_HABIS tanpa dimainkan
     */
    private static Hasil mainkan(Random acak, Pilih pilih, Pilih pilihAkhir, boolean ritualGagal) throws Exception {
        StatusCerita status = new StatusCerita();
        TasPusaka tas = new TasPusaka();
        List<String> jejak = new ArrayList<>();
        List<String> teks = new ArrayList<>();
        String langkah = AlurCerita.NASKAH_AWAL;
        while (langkah != null) {
            assertTrue(jejak.size() < 100, "Alur berputar-putar: " + jejak);
            jejak.add(langkah);
            Path fileNaskah = ASSETS.resolve("cerita").resolve(langkah + ".json");
            if (Files.isRegularFile(fileNaskah)) {
                Pilih p = langkah.equals("babak5_akhir") ? pilihAkhir : pilih;
                langkah = mainkanNaskah(PembacaNaskah.bacaFile(fileNaskah), status, p, teks);
                continue;
            }
            RencanaPertarungan r = PembacaPertarungan.bacaFile(ASSETS.resolve("pertarungan").resolve(langkah + ".json"));
            StatusBattle hasil;
            if (r.isRitual() && ritualGagal) {
                hasil = StatusBattle.WAKTU_HABIS;
            } else {
                BattleManager battle = r.buatBattle(tas);
                boolean pakaiJurus = r.isRitual()
                        || battle.getLawan().stream().anyMatch(k -> ((MakhlukGirah) k).isBos());
                hasil = mainkanPertarungan(battle, acak, pakaiJurus);
                assertEquals(StatusBattle.MENANG, hasil,
                        "Pertarungan " + r.getId() + " " + hasil + "; babad:\n" + battle.getBabad().getIsi());
            }
            if (r.isRitual()) {
                status.catatHasilRitual(hasil);
            }
            langkah = hasil == StatusBattle.MENANG ? r.getLanjutanMenang() : r.getLanjutanWaktuHabis();
        }
        return new Hasil(jejak, status, teks);
    }

    /** Membaca semua baris (teksnya dikumpulkan); pilihan menurut {@code pilih}. Mengembalikan "then". */
    private static String mainkanNaskah(Naskah naskah, StatusCerita status, Pilih pilih, List<String> teks) {
        PemutarNaskah p = new PemutarNaskah(naskah, status);
        for (int i = 0; !p.isSelesai(); i++) {
            assertTrue(i < 500, "Naskah tidak selesai: " + naskah.getNama());
            if (p.isPilihan()) {
                p.pilih(pilih == Pilih.TERAKHIR ? p.getOpsiTersedia().size() - 1 : 0);
            } else {
                assertFalse(p.getBaris().getTeks().isBlank());
                teks.add(p.getBaris().getTeks());
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
