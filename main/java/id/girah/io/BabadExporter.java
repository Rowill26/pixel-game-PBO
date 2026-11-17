package id.girah.io;

import id.girah.battle.BattleManager;
import id.girah.battle.StatusBattle;
import id.girah.karakter.KarakterMitos;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Mengekspor Babad Pertarungan ke file teks.
 *
 * <h2>Rantai stream</h2>
 * <pre>
 *  Files.newOutputStream   byte stream   (menulis byte mentah ke disk)
 *   -> OutputStreamWriter  character stream (mengubah karakter jadi byte UTF-8)
 *     -> BufferedWriter    menampung tulisan di memori, menulis ke disk sekaligus
 * </pre>
 *
 * <h2>Keamanan file sederhana</h2>
 * <ul>
 *   <li>File hanya ditulis di dalam folder yang ditentukan program, dan namanya
 *       dibuat program (tanggal-waktu), bukan dari input pemain.</li>
 *   <li>File lama tidak pernah ditimpa: dibuka dengan {@code CREATE_NEW}, dan jika nama
 *       sudah dipakai, diberi akhiran {@code _2}, {@code _3}, dan seterusnya.</li>
 * </ul>
 *
 * <p>Di LibGDX: {@code new BabadExporter(Gdx.files.local("babad").file().toPath())}.
 */
public class BabadExporter {

    private static final DateTimeFormatter FORMAT_NAMA_FILE = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter FORMAT_TAMPILAN = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final String GARIS = "----------------------------------------";
    private static final int BATAS_PERCOBAAN_NAMA = 1000;

    private final Path folder;
    private final Clock jam;

    public BabadExporter(Path folder) {
        this(folder, Clock.systemDefaultZone());
    }

    /** Clock disuntikkan agar nama file dan waktu bisa diuji secara pasti. */
    public BabadExporter(Path folder, Clock jam) {
        this.folder = Objects.requireNonNull(folder, "Folder tidak boleh null");
        this.jam = Objects.requireNonNull(jam, "Clock tidak boleh null");
    }

    /**
     * Menulis babad pertarungan yang sudah selesai ke file baru.
     *
     * @return lokasi file yang ditulis
     * @throws IllegalStateException jika pertarungan belum selesai
     * @throws IOException           jika folder atau file tidak bisa ditulis; pemanggil (layar
     *                               battle) menangkapnya dan memberi tahu pemain
     */
    public Path ekspor(BattleManager battle) throws IOException {
        Objects.requireNonNull(battle, "Battle tidak boleh null");
        if (!battle.getStatus().isSelesai()) {
            throw new IllegalStateException("Babad hanya diekspor setelah pertarungan selesai");
        }
        Files.createDirectories(folder);
        LocalDateTime waktu = LocalDateTime.now(jam);
        String namaDasar = "babad_" + waktu.format(FORMAT_NAMA_FILE);

        for (int nomor = 1; nomor <= BATAS_PERCOBAAN_NAMA; nomor++) {
            Path file = folder.resolve(namaDasar + (nomor == 1 ? "" : "_" + nomor) + ".txt");
            try (BufferedWriter penulis = new BufferedWriter(new OutputStreamWriter(
                    Files.newOutputStream(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE),
                    StandardCharsets.UTF_8))) {
                tulis(penulis, battle, waktu);
                return file;
            } catch (FileAlreadyExistsException e) {
                // Nama sudah dipakai: coba akhiran berikutnya, jangan timpa file lama.
            }
        }
        throw new IOException("Terlalu banyak babad dengan waktu yang sama di " + folder);
    }

    private void tulis(BufferedWriter penulis, BattleManager battle, LocalDateTime waktu) throws IOException {
        tulisBaris(penulis, "BABAD PERTARUNGAN");
        tulisBaris(penulis, "Bayang-Bayang Girah");
        tulisBaris(penulis, GARIS);
        tulisBaris(penulis, "Dicatat : " + waktu.format(FORMAT_TAMPILAN));
        tulisBaris(penulis, "Hasil   : " + labelHasil(battle.getStatus()) + " (ronde ke-" + battle.getRonde() + ")");
        tulisBaris(penulis, "");
        tulisDaftar(penulis, "Kelompok Pengelana", battle.getKelompok());
        tulisDaftar(penulis, "Kekuatan Girah", battle.getLawan());
        tulisBaris(penulis, GARIS);

        for (String baris : battle.getBabad().getIsi().split("\n", -1)) {
            tulisBaris(penulis, baris);
        }
        tulisBaris(penulis, GARIS);
        tulisBaris(penulis, "Babad ini mencatat " + battle.getBabad().getJumlahPeristiwa() + " peristiwa.");
    }

    private static void tulisDaftar(BufferedWriter penulis, String judul, List<KarakterMitos> daftar)
            throws IOException {
        tulisBaris(penulis, judul + ":");
        for (KarakterMitos k : daftar) {
            tulisBaris(penulis, "  - " + k.getNama() + " (" + k.getPeran() + "), nyawa "
                    + k.getNyawa() + "/" + k.getNyawaMaks() + (k.isHidup() ? "" : ", gugur"));
        }
    }

    /** {@code newLine()} memakai pemisah baris sistem operasi, jadi file rapi di Notepad Windows. */
    private static void tulisBaris(BufferedWriter penulis, String baris) throws IOException {
        penulis.write(baris);
        penulis.newLine();
    }

    static String labelHasil(StatusBattle hasil) {
        switch (hasil) {
            case MENANG:
                return "Menang";
            case KALAH:
                return "Kalah";
            case WAKTU_HABIS:
                return "Waktu habis, ritual tuntas";
            default:
                throw new IllegalArgumentException("Bukan status akhir: " + hasil);
        }
    }
}
