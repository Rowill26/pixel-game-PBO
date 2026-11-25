package id.girah.alur;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import id.girah.cerita.BarisDialog;
import id.girah.cerita.LangkahNaskah;
import id.girah.cerita.Naskah;
import id.girah.cerita.NaskahTidakValidException;
import id.girah.cerita.OpsiPilihan;
import id.girah.cerita.PembacaNaskah;
import id.girah.cerita.Pilihan;
import id.girah.karakter.MakhlukGirah;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Memeriksa seluruh jaringan cerita di {@code assets/} setiap kali {@code ./gradlew test} dijalankan:
 * setiap naskah dan pertarungan terbaca, setiap tautan menunjuk ke file yang ada, setiap
 * ekspresi potret tersedia, dan semua bagian cerita bisa dicapai dari naskah awal.
 * Dilewati jika folder {@code assets/cerita} belum ada.
 */
class AsetCeritaTest {

    /** Gradle menjalankan test dari folder modul core, jadi assets ada satu tingkat di atas. */
    private static final Path ASSETS = Path.of("..", "assets");
    /** Naskah contoh dari prototipe; tidak wajib terhubung ke alur cerita. */
    private static final String AWALAN_CONTOH = "contoh_";

    private static final Map<String, Naskah> naskah = new TreeMap<>();
    private static final Map<String, RencanaPertarungan> pertarungan = new TreeMap<>();
    private static final List<String> kesalahanBaca = new ArrayList<>();

    @BeforeAll
    static void bacaSemua() throws IOException {
        if (!Files.isDirectory(ASSETS.resolve("cerita"))) {
            return;
        }
        for (Path f : jsonDi("cerita")) {
            try {
                Naskah n = PembacaNaskah.bacaFile(f);
                naskah.put(n.getNama(), n);
            } catch (NaskahTidakValidException e) {
                kesalahanBaca.add(e.getMessage());
            }
        }
        for (Path f : jsonDi("pertarungan")) {
            try {
                RencanaPertarungan p = PembacaPertarungan.bacaFile(f);
                pertarungan.put(p.getId(), p);
            } catch (PertarunganTidakValidException e) {
                kesalahanBaca.add(e.getMessage());
            }
        }
    }

    private static List<Path> jsonDi(String folder) throws IOException {
        Path dir = ASSETS.resolve(folder);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> isi = Files.list(dir)) {
            return isi.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
    }

    private static void butuhAssets() {
        assumeTrue(Files.isDirectory(ASSETS.resolve("cerita")), "assets/cerita belum ada; pemeriksaan dilewati");
    }

    private static void tidakAdaKesalahan(List<String> kesalahan, String judul) {
        assertTrue(kesalahan.isEmpty(), judul + ":\n  " + String.join("\n  ", kesalahan));
    }

    // ================================================================

    @Test
    @DisplayName("Semua naskah dan pertarungan terbaca tanpa kesalahan")
    void semuaFileValid() {
        butuhAssets();
        tidakAdaKesalahan(kesalahanBaca, "File yang perlu diperbaiki");
    }

    @Test
    @DisplayName("Nama naskah dan pertarungan tidak ada yang sama")
    void namaTidakBentrok() {
        butuhAssets();
        Set<String> sama = new TreeSet<>(naskah.keySet());
        sama.retainAll(pertarungan.keySet());
        assertTrue(sama.isEmpty(), "Ada naskah dan pertarungan dengan nama sama: " + sama);
    }

    @Test
    @DisplayName("Setiap arena dan waktu (mode) yang dipakai ada di assets/arena")
    void arenaAda() throws IOException {
        butuhAssets();
        List<String> kesalahan = new ArrayList<>();
        for (Naskah n : naskah.values()) {
            if (n.getArena() != null) {
                periksaArena(n.getArena(), n.getMode(), "naskah '" + n.getNama() + "'", kesalahan);
            }
        }
        for (RencanaPertarungan p : pertarungan.values()) {
            periksaArena(p.getArena(), p.getMode(), "pertarungan '" + p.getId() + "'", kesalahan);
        }
        tidakAdaKesalahan(kesalahan, "Arena yang tidak ada");
    }

    private static void periksaArena(String arena, String mode, String pemakai, List<String> kesalahan)
            throws IOException {
        Path file = ASSETS.resolve("arena").resolve(arena).resolve("arena.json");
        if (!Files.isRegularFile(file)) {
            kesalahan.add(pemakai + ": arena \"" + arena + "\" tidak ada di assets/arena");
            return;
        }
        if (mode == null) {
            return;
        }
        JsonValue modes = new JsonReader().parse(Files.readString(file, StandardCharsets.UTF_8)).get("modes");
        if (modes == null || !modes.has(mode)) {
            kesalahan.add(pemakai + ": arena \"" + arena + "\" tidak punya mode \"" + mode + "\"");
        }
    }

    @Test
    @DisplayName("Setiap \"then\", \"menang\", dan \"waktuHabis\" menunjuk ke file yang ada")
    void tautanAda() {
        butuhAssets();
        List<String> kesalahan = new ArrayList<>();
        for (Naskah n : naskah.values()) {
            String then = n.getLanjutan();
            if (then != null && !naskah.containsKey(then) && !pertarungan.containsKey(then)
                    && !Files.isDirectory(ASSETS.resolve("arena").resolve(then))) {
                kesalahan.add("naskah '" + n.getNama() + "': \"then\": \"" + then
                        + "\" bukan naskah, pertarungan, atau arena");
            }
        }
        for (RencanaPertarungan p : pertarungan.values()) {
            String menang = p.getLanjutanMenang();
            if (menang != null && !naskah.containsKey(menang)) {
                kesalahan.add("pertarungan '" + p.getId() + "': \"menang\": \"" + menang + "\" bukan naskah");
            }
            String waktuHabis = p.getLanjutanWaktuHabis();
            if (waktuHabis != null && !naskah.containsKey(waktuHabis)) {
                kesalahan.add("pertarungan '" + p.getId() + "': \"waktuHabis\": \"" + waktuHabis + "\" bukan naskah");
            }
        }
        tidakAdaKesalahan(kesalahan, "Tautan yang putus");
    }

    @Test
    @DisplayName("Arena setiap pertarungan punya posisi untuk setiap musuh (sprite sama di arena.json)")
    void posisiMusuhAda() throws IOException {
        butuhAssets();
        List<String> kesalahan = new ArrayList<>();
        for (RencanaPertarungan p : pertarungan.values()) {
            Path file = ASSETS.resolve("arena").resolve(p.getArena()).resolve("arena.json");
            if (!Files.isRegularFile(file)) {
                continue;                                   // arena yang hilang dilaporkan test lain
            }
            Map<String, Integer> tersedia = new TreeMap<>();
            JsonValue akar = new JsonReader().parse(Files.readString(file, StandardCharsets.UTF_8));
            for (JsonValue a = akar.get("actors").child; a != null; a = a.next) {
                if ("girah".equals(a.getString("side", ""))) {
                    tersedia.merge(a.getString("sprite"), 1, Integer::sum);
                }
            }
            Map<String, Integer> dibutuhkan = new TreeMap<>();
            for (MakhlukGirah m : p.buatMusuh()) {
                dibutuhkan.merge(m.getIdSprite(), 1, Integer::sum);
            }
            for (Map.Entry<String, Integer> e : dibutuhkan.entrySet()) {
                int ada = tersedia.getOrDefault(e.getKey(), 0);
                if (ada < e.getValue()) {
                    kesalahan.add("pertarungan '" + p.getId() + "': arena '" + p.getArena() + "' punya " + ada
                            + " posisi sprite '" + e.getKey() + "', dibutuhkan " + e.getValue());
                }
            }
        }
        tidakAdaKesalahan(kesalahan, "Posisi musuh yang kurang");
    }

    @Test
    @DisplayName("Ekspresi potret yang dipakai tersedia di assets/potret")
    void ekspresiPotretAda() {
        butuhAssets();
        List<String> kesalahan = new ArrayList<>();
        for (Naskah n : naskah.values()) {
            for (BarisDialog b : semuaBaris(n.getLangkah())) {
                String who = b.getIdPembicara();
                boolean punyaPotret = Files.isRegularFile(ASSETS.resolve("potret").resolve(who + "_netral.png"));
                if (punyaPotret && !Files.isRegularFile(ASSETS.resolve("potret").resolve(who + "_" + b.getEkspresi() + ".png"))) {
                    kesalahan.add("naskah '" + n.getNama() + "': potret " + who + "_" + b.getEkspresi()
                            + ".png tidak ada (\"" + b.getTeks() + "\")");
                }
            }
        }
        tidakAdaKesalahan(kesalahan, "Ekspresi potret yang tidak ada");
    }

    /** Semua baris dialog, termasuk yang ada di dalam balasan pilihan. */
    private static List<BarisDialog> semuaBaris(List<LangkahNaskah> langkah) {
        List<BarisDialog> hasil = new ArrayList<>();
        for (LangkahNaskah l : langkah) {
            if (l instanceof BarisDialog) {
                hasil.add((BarisDialog) l);
            } else {
                for (OpsiPilihan o : ((Pilihan) l).getOpsi()) {
                    hasil.addAll(semuaBaris(o.getBalasan()));
                }
            }
        }
        return hasil;
    }

    @Test
    @DisplayName("Semua naskah dan pertarungan bisa dicapai dari naskah awal")
    void semuaBisaDicapai() {
        butuhAssets();
        assertTrue(naskah.containsKey(AlurCerita.NASKAH_AWAL), "Naskah awal tidak ada: " + AlurCerita.NASKAH_AWAL);

        Set<String> tercapai = new HashSet<>();
        Deque<String> antrean = new ArrayDeque<>(List.of(AlurCerita.NASKAH_AWAL));
        while (!antrean.isEmpty()) {
            String id = antrean.poll();
            if (!tercapai.add(id)) {
                continue;
            }
            if (naskah.containsKey(id) && naskah.get(id).getLanjutan() != null) {
                antrean.add(naskah.get(id).getLanjutan());
            }
            if (pertarungan.containsKey(id) && pertarungan.get(id).getLanjutanMenang() != null) {
                antrean.add(pertarungan.get(id).getLanjutanMenang());
            }
            if (pertarungan.containsKey(id) && pertarungan.get(id).getLanjutanWaktuHabis() != null) {
                antrean.add(pertarungan.get(id).getLanjutanWaktuHabis());
            }
        }

        Set<String> yatim = new TreeSet<>();
        for (String id : naskah.keySet()) {
            if (!id.startsWith(AWALAN_CONTOH) && !tercapai.contains(id)) {
                yatim.add("naskah " + id);
            }
        }
        for (String id : pertarungan.keySet()) {
            if (!tercapai.contains(id)) {
                yatim.add("pertarungan " + id);
            }
        }
        assertTrue(yatim.isEmpty(), "Tidak bisa dicapai dari " + AlurCerita.NASKAH_AWAL + ": " + yatim);
    }
}
