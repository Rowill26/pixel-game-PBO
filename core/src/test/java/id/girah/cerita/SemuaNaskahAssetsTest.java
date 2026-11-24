package id.girah.cerita;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Memeriksa SEMUA naskah di {@code assets/cerita/} setiap kali {@code ./gradlew test} dijalankan,
 * supaya kesalahan penulisan naskah ketahuan sebelum game dimainkan.
 * Dilewati jika folder itu belum ada (sebelum asset front end digabung).
 */
class SemuaNaskahAssetsTest {

    /** Gradle menjalankan test dari folder modul core, jadi assets ada satu tingkat di atas. */
    private static final Path FOLDER_NASKAH = Path.of("..", "assets", "cerita");

    @Test
    @DisplayName("Semua naskah di assets/cerita terbaca tanpa kesalahan")
    void semuaNaskahValid() throws IOException {
        assumeTrue(Files.isDirectory(FOLDER_NASKAH), "assets/cerita belum ada; pemeriksaan dilewati");

        List<Path> files;
        try (Stream<Path> isi = Files.list(FOLDER_NASKAH)) {
            files = isi.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }

        List<String> kesalahan = new ArrayList<>();
        for (Path f : files) {
            try {
                PembacaNaskah.bacaFile(f);
            } catch (NaskahTidakValidException e) {
                kesalahan.add(e.getMessage());
            }
        }
        assertTrue(kesalahan.isEmpty(), "Naskah yang perlu diperbaiki:\n  " + String.join("\n  ", kesalahan));
    }
}
