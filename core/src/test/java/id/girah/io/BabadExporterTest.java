package id.girah.io;

import static org.junit.jupiter.api.Assertions.*;

import id.girah.battle.BattleManager;
import id.girah.battle.StatusBattle;
import id.girah.karakter.Leak;
import id.girah.karakter.Senopati;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BabadExporterTest {

    private static final Clock JAM_TETAP = Clock.fixed(Instant.parse("2026-11-17T14:30:05Z"), ZoneOffset.UTC);

    @TempDir
    Path folderSementara;

    private Path folderBabad;
    private BabadExporter exporter;

    @BeforeEach
    void siapkan() {
        folderBabad = folderSementara.resolve("babad");     // belum ada, harus dibuat otomatis
        exporter = new BabadExporter(folderBabad, JAM_TETAP);
    }

    /** Pertarungan satu ronde yang dimenangkan Senopati. */
    private static BattleManager pertarunganSelesai(String namaSenopati) {
        Leak leak = new Leak("Leak Setra");
        leak.terimaSerangan(62);                            // -> nyawa 1
        BattleManager battle = new BattleManager(List.of(new Senopati(namaSenopati)), List.of(leak));
        battle.mulai();
        battle.serang(leak);
        assertEquals(StatusBattle.MENANG, battle.getStatus());
        return battle;
    }

    @Test
    @DisplayName("Babad ditulis ke file baru bernama tanggal-waktu, lengkap dengan kepala dan isi")
    void eksporBerhasil() throws IOException {
        BattleManager battle = pertarunganSelesai("Arya");

        Path file = exporter.ekspor(battle);

        assertEquals(folderBabad.resolve("babad_2026-11-17_14-30-05.txt"), file);
        List<String> baris = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals("BABAD PERTARUNGAN", baris.get(0));
        assertTrue(baris.contains("Dicatat : 17-11-2026 14:30:05"), baris.toString());
        assertTrue(baris.contains("Hasil   : Menang (ronde ke-1)"), baris.toString());
        assertTrue(baris.contains("  - Arya (Pendekar), nyawa 120/120"), baris.toString());
        assertTrue(baris.contains("  - Leak Setra (Leak), nyawa 0/60, gugur"), baris.toString());
        assertTrue(baris.contains("=== Ronde 1 ==="), baris.toString());
        assertTrue(baris.contains("Arya menghunus keris dan menebas Leak Setra!"), baris.toString());
        assertEquals("Babad ini mencatat 2 peristiwa.", baris.get(baris.size() - 1));
    }

    @Test
    @DisplayName("Ekspor kedua di detik yang sama tidak menimpa file pertama")
    void tidakMenimpaFileLama() throws IOException {
        Path pertama = exporter.ekspor(pertarunganSelesai("Arya"));
        String isiPertama = Files.readString(pertama, StandardCharsets.UTF_8);

        Path kedua = exporter.ekspor(pertarunganSelesai("Wiraraja"));

        assertEquals(folderBabad.resolve("babad_2026-11-17_14-30-05_2.txt"), kedua);
        assertEquals(isiPertama, Files.readString(pertama, StandardCharsets.UTF_8));
        assertTrue(Files.readString(kedua, StandardCharsets.UTF_8).contains("Wiraraja"));
    }

    @Test
    @DisplayName("Huruf di luar ASCII tersimpan utuh (UTF-8)")
    void utf8() throws IOException {
        Path file = exporter.ekspor(pertarunganSelesai("Gusti Ngurah Ré ✦"));
        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("Gusti Ngurah Ré ✦"));
    }

    @Test
    void pertarunganBelumSelesaiDitolak() throws IOException {
        BattleManager battle = new BattleManager(List.of(new Senopati("Arya")), List.of(new Leak("Leak")));
        battle.mulai();
        assertThrows(IllegalStateException.class, () -> exporter.ekspor(battle));
        assertFalse(Files.exists(folderBabad), "Tidak ada yang ditulis");
    }

    @Test
    @DisplayName("Folder tidak bisa dibuat: IOException untuk ditangkap pemanggil")
    void folderTidakBisaDibuat() throws IOException {
        Path bukanFolder = Files.writeString(folderSementara.resolve("ini-file.txt"), "x");
        BabadExporter salah = new BabadExporter(bukanFolder.resolve("babad"), JAM_TETAP);
        assertThrows(IOException.class, () -> salah.ekspor(pertarunganSelesai("Arya")));
    }

    @Test
    void labelHasilHanyaUntukStatusAkhir() {
        assertEquals("Kalah", BabadExporter.labelHasil(StatusBattle.KALAH));
        assertEquals("Waktu habis, ritual tuntas", BabadExporter.labelHasil(StatusBattle.WAKTU_HABIS));
        assertThrows(IllegalArgumentException.class, () -> BabadExporter.labelHasil(StatusBattle.GILIRAN_PEMAIN));
    }

    @Test
    void hanyaSatuFileDibuat() throws IOException {
        exporter.ekspor(pertarunganSelesai("Arya"));
        try (Stream<Path> isi = Files.list(folderBabad)) {
            assertEquals(1, isi.count());
        }
    }
}
