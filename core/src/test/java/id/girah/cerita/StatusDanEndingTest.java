package id.girah.cerita;

import static org.junit.jupiter.api.Assertions.*;

import id.girah.battle.StatusBattle;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** StatusCerita, Syarat, dan PenentuEnding. */
class StatusDanEndingTest {

    private StatusCerita status;

    @BeforeEach
    void siapkan() {
        status = new StatusCerita();
    }

    /** Pilihan A di Babak 4 (+2, Ratna membantu) ditambah Girah dulu di Babak 2 (+1) = 3. */
    private void jalurPemulihanTerbuka() {
        status.terapkan(new Efek(Map.of(NilaiCerita.KEPERCAYAAN_RATNA, 1), Set.of(PenandaCerita.PENAWAR_GIRAH_DULU)));
        status.terapkan(new Efek(Map.of(NilaiCerita.KEPERCAYAAN_RATNA, 2),
                Set.of(PenandaCerita.BAHULA_JUJUR, PenandaCerita.RATNA_MEMBANTU_KITAB)));
    }

    @Nested
    class Status {

        @Test
        void nilaiAwalNolDanBisaNegatif() {
            assertEquals(0, status.getNilai(NilaiCerita.KEPERCAYAAN_RATNA));
            status.tambahNilai(NilaiCerita.KEPERCAYAAN_RATNA, -2);
            status.tambahNilai(NilaiCerita.KEPERCAYAAN_RATNA, 1);
            assertEquals(-1, status.getNilai(NilaiCerita.KEPERCAYAAN_RATNA));
        }

        @Test
        void terapkanEfek() {
            status.terapkan(new Efek(Map.of(NilaiCerita.WARGA_SELAMAT, 30, NilaiCerita.KEPERCAYAAN_GIRAH, 1),
                    Set.of(PenandaCerita.DUKUNG_BAHULA_JUJUR)));
            assertEquals(30, status.getNilai(NilaiCerita.WARGA_SELAMAT));
            assertEquals(1, status.getNilai(NilaiCerita.KEPERCAYAAN_GIRAH));
            assertTrue(status.punya(PenandaCerita.DUKUNG_BAHULA_JUJUR));
            assertFalse(status.punya(PenandaCerita.BAHULA_JUJUR));
        }

        @Test
        void efekKosongTidakMengubahApaPun() {
            status.terapkan(Efek.KOSONG);
            assertTrue(status.getSemuaNilai().isEmpty());
            assertTrue(status.getSemuaPenanda().isEmpty());
        }

        @Test
        void salinanTidakBisaDiubah() {
            status.tandai(PenandaCerita.BAHULA_JUJUR);
            status.tambahNilai(NilaiCerita.KEPERCAYAAN_RATNA, 1);
            assertThrows(UnsupportedOperationException.class, () -> status.getSemuaPenanda().clear());
            assertThrows(UnsupportedOperationException.class, () -> status.getSemuaNilai().clear());
        }

        @Test
        @DisplayName("Hasil ritual: MENANG = tepat waktu, WAKTU_HABIS = gagal, lainnya ditolak")
        void hasilRitual() {
            assertFalse(status.isHasilRitualDiketahui());
            assertThrows(IllegalStateException.class, status::isRitualDihentikanTepatWaktu);
            assertThrows(IllegalArgumentException.class, () -> status.catatHasilRitual(StatusBattle.KALAH));
            assertThrows(IllegalArgumentException.class, () -> status.catatHasilRitual(StatusBattle.GILIRAN_PEMAIN));

            status.catatHasilRitual(StatusBattle.MENANG);
            assertTrue(status.isRitualDihentikanTepatWaktu());
            status.catatHasilRitual(StatusBattle.WAKTU_HABIS);
            assertFalse(status.isRitualDihentikanTepatWaktu());
        }
    }

    @Nested
    @DisplayName("Aturan ending dari dokumen cerita")
    class AturanEnding {

        @Test
        void belumAdaHasilRitual() {
            assertThrows(IllegalStateException.class, () -> PenentuEnding.tentukan(status));
        }

        @Test
        @DisplayName("Ending 3: ritual tidak dihentikan tepat waktu, apa pun pilihannya")
        void akhirTragis() {
            jalurPemulihanTerbuka();
            status.tandai(PenandaCerita.PILIH_JALAN_PEMULIHAN);
            status.catatHasilRitual(StatusBattle.WAKTU_HABIS);
            assertEquals(Ending.AKHIR_TRAGIS, PenentuEnding.tentukan(status));
        }

        @Test
        @DisplayName("Ending 1: ritual dihentikan, lalu menyerahkan Calon Arang")
        void pengadilan() {
            jalurPemulihanTerbuka();
            status.tandai(PenandaCerita.SERAHKAN_KE_KERAJAAN);
            status.catatHasilRitual(StatusBattle.MENANG);
            assertEquals(Ending.PENGADILAN_KERAJAAN, PenentuEnding.tentukan(status));
        }

        @Test
        @DisplayName("Ending 2: kepercayaan tinggi, Ratna membantu, ritual dihentikan, memilih pemulihan")
        void pemulihan() {
            jalurPemulihanTerbuka();
            assertTrue(PenentuEnding.jalanPemulihanTerbuka(status));
            status.tandai(PenandaCerita.PILIH_JALAN_PEMULIHAN);
            status.catatHasilRitual(StatusBattle.MENANG);
            assertEquals(Ending.JALAN_PEMULIHAN, PenentuEnding.tentukan(status));
        }

        @Test
        @DisplayName("Hanya pilihan A di Babak 4 (kepercayaan 2): jalan pemulihan belum terbuka")
        void kepercayaanKurang() {
            status.terapkan(new Efek(Map.of(NilaiCerita.KEPERCAYAAN_RATNA, 2), Set.of(PenandaCerita.RATNA_MEMBANTU_KITAB)));
            assertFalse(PenentuEnding.jalanPemulihanTerbuka(status));
            status.tandai(PenandaCerita.PILIH_JALAN_PEMULIHAN);
            status.catatHasilRitual(StatusBattle.MENANG);
            assertEquals(Ending.PENGADILAN_KERAJAAN, PenentuEnding.tentukan(status),
                    "Pilihan pemulihan tanpa syarat tidak berlaku");
        }

        @Test
        @DisplayName("Kitab diambil diam-diam menutup jalan pemulihan")
        void kitabDicuri() {
            status.tambahNilai(NilaiCerita.KEPERCAYAAN_RATNA, 5);
            status.tandai(PenandaCerita.RATNA_MEMBANTU_KITAB);
            status.tandai(PenandaCerita.KITAB_DIAMBIL_DIAM_DIAM);
            assertFalse(PenentuEnding.jalanPemulihanTerbuka(status));
        }

        @Test
        void tanpaBantuanRatnaTertutup() {
            status.tambahNilai(NilaiCerita.KEPERCAYAAN_RATNA, 5);
            assertFalse(PenentuEnding.jalanPemulihanTerbuka(status));
        }

        @Test
        @DisplayName("Janda Wening selamat jika penawar Tapak Wangi didahulukan")
        void wening() {
            assertFalse(PenentuEnding.weningSelamat(status));
            status.tandai(PenandaCerita.PENAWAR_TAPAK_WANGI_DULU);
            assertTrue(PenentuEnding.weningSelamat(status));
        }
    }

    @Nested
    class SyaratCerita {

        private Syarat syarat(Map<NilaiCerita, Integer> min, Map<NilaiCerita, Integer> kurang,
                              Set<PenandaCerita> ada, Set<PenandaCerita> tanpa,
                              Boolean pemulihan, Boolean wening, Ending ending) {
            return new Syarat(min, kurang, ada, tanpa, pemulihan, wening, ending);
        }

        @Test
        void selaluTerpenuhi() {
            assertTrue(Syarat.SELALU.isSelalu());
            assertTrue(Syarat.SELALU.terpenuhi(status));
        }

        @Test
        @DisplayName("Nilai minimal (>=) dan kurangDari (<)")
        void batasNilai() {
            Syarat tinggi = syarat(Map.of(NilaiCerita.KEPERCAYAAN_RATNA, 3), Map.of(), Set.of(), Set.of(), null, null, null);
            Syarat rendah = syarat(Map.of(), Map.of(NilaiCerita.KEPERCAYAAN_RATNA, 3), Set.of(), Set.of(), null, null, null);
            status.tambahNilai(NilaiCerita.KEPERCAYAAN_RATNA, 2);
            assertFalse(tinggi.terpenuhi(status));
            assertTrue(rendah.terpenuhi(status));
            status.tambahNilai(NilaiCerita.KEPERCAYAAN_RATNA, 1);   // tepat 3
            assertTrue(tinggi.terpenuhi(status));
            assertFalse(rendah.terpenuhi(status));
        }

        @Test
        void penandaAdaDanTidakAda() {
            Syarat s = syarat(Map.of(), Map.of(), Set.of(PenandaCerita.BAHULA_JUJUR),
                    Set.of(PenandaCerita.KITAB_DIAMBIL_DIAM_DIAM), null, null, null);
            assertFalse(s.terpenuhi(status));
            status.tandai(PenandaCerita.BAHULA_JUJUR);
            assertTrue(s.terpenuhi(status));
            status.tandai(PenandaCerita.KITAB_DIAMBIL_DIAM_DIAM);
            assertFalse(s.terpenuhi(status));
        }

        @Test
        void jalanPemulihanDanWening() {
            Syarat terbuka = syarat(Map.of(), Map.of(), Set.of(), Set.of(), true, null, null);
            Syarat tertutup = syarat(Map.of(), Map.of(), Set.of(), Set.of(), false, null, null);
            Syarat weningMati = syarat(Map.of(), Map.of(), Set.of(), Set.of(), null, false, null);
            assertFalse(terbuka.terpenuhi(status));
            assertTrue(tertutup.terpenuhi(status));
            assertTrue(weningMati.terpenuhi(status));
            jalurPemulihanTerbuka();
            assertTrue(terbuka.terpenuhi(status));
            assertFalse(tertutup.terpenuhi(status));
        }

        @Test
        @DisplayName("Syarat ending: tidak terpenuhi sebelum ritual selesai, lalu cocok dengan ending")
        void syaratEnding() {
            Syarat tragis = syarat(Map.of(), Map.of(), Set.of(), Set.of(), null, null, Ending.AKHIR_TRAGIS);
            Syarat pengadilan = syarat(Map.of(), Map.of(), Set.of(), Set.of(), null, null, Ending.PENGADILAN_KERAJAAN);
            assertFalse(tragis.terpenuhi(status), "Belum ada hasil ritual: tidak terpenuhi, tidak melempar exception");
            status.catatHasilRitual(StatusBattle.WAKTU_HABIS);
            assertTrue(tragis.terpenuhi(status));
            assertFalse(pengadilan.terpenuhi(status));
        }
    }
}
