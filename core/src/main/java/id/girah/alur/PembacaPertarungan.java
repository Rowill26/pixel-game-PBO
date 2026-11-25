package id.girah.alur;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.SerializationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Membaca {@code assets/pertarungan/<id>.json} menjadi {@link RencanaPertarungan}.
 *
 * <pre>
 * {
 *   "arena": "hutan",                         wajib: folder di assets/arena
 *   "mode": "night",                          opsional
 *   "musuh": [                                wajib, minimal satu
 *     {"jenis": "leak", "nama": "Leak 1"},    jenis = kunci sprite; nama opsional
 *     {"jenis": "pemimpin_pengikut"}          bos bernama tetap: tanpa nama
 *   ],
 *   "batasRonde": 8,                          opsional, bawaan 0 (tanpa batas)
 *   "menang": "babak1_penutup"                opsional: naskah setelah menang
 * }
 * </pre>
 *
 * <p>Ketat seperti {@link id.girah.cerita.PembacaNaskah}: kunci tak dikenal, jenis musuh salah
 * ketik, atau nama musuh ganda ditolak dengan letak kesalahannya.
 */
public final class PembacaPertarungan {

    private static final Set<String> KUNCI_AKAR = Set.of(
            "arena", "mode", "musuh", "batasRonde", "menang", "ritual", "waktuHabis");
    private static final Set<String> KUNCI_MUSUH = Set.of("jenis", "nama");

    private final String id;

    private PembacaPertarungan(String id) {
        this.id = id;
    }

    public static RencanaPertarungan baca(String id, String json) throws PertarunganTidakValidException {
        Objects.requireNonNull(id, "Id pertarungan tidak boleh null");
        Objects.requireNonNull(json, "Isi file tidak boleh null");
        return new PembacaPertarungan(id).bacaAkar(json);
    }

    /** Membaca dari file UTF-8. Id = nama file tanpa {@code .json}. */
    public static RencanaPertarungan bacaFile(Path file) throws IOException, PertarunganTidakValidException {
        Objects.requireNonNull(file, "File tidak boleh null");
        String namaFile = file.getFileName().toString();
        String id = namaFile.endsWith(".json") ? namaFile.substring(0, namaFile.length() - 5) : namaFile;
        return baca(id, Files.readString(file, StandardCharsets.UTF_8));
    }

    private RencanaPertarungan bacaAkar(String json) throws PertarunganTidakValidException {
        JsonValue akar;
        try {
            akar = new JsonReader().parse(json);
        } catch (SerializationException e) {
            throw salah("", "bukan JSON yang sah (" + e.getMessage() + ")", e);
        }
        if (akar == null || !akar.isObject()) {
            throw salah("", "isi file harus berupa objek JSON { ... }");
        }
        periksaKunci(akar, KUNCI_AKAR, "");

        String arena = teks(akar, "arena", true);
        String mode = teks(akar, "mode", false);
        String menang = teks(akar, "menang", false);
        String waktuHabis = teks(akar, "waktuHabis", false);
        boolean ritual = false;
        JsonValue r = akar.get("ritual");
        if (r != null && !r.isNull()) {
            if (!r.isBoolean()) {
                throw salah("ritual", "harus true atau false");
            }
            ritual = r.asBoolean();
        }

        int batasRonde = 0;
        JsonValue batas = akar.get("batasRonde");
        if (batas != null && !batas.isNull()) {
            if (!batas.isLong() || batas.asLong() < 0 || batas.asLong() > 1000) {
                throw salah("batasRonde", "harus bilangan bulat 0 sampai 1000 (0 = tanpa batas)");
            }
            batasRonde = (int) batas.asLong();
        }

        JsonValue daftar = akar.get("musuh");
        if (daftar == null || !daftar.isArray() || daftar.size == 0) {
            throw salah("musuh", "wajib ada dan berisi minimal satu musuh");
        }
        List<RencanaPertarungan.Musuh> musuh = new ArrayList<>();
        Set<String> namaDipakai = new HashSet<>();
        int i = 0;
        for (JsonValue m = daftar.child; m != null; m = m.next, i++) {
            String l = "musuh[" + i + "]";
            if (!m.isObject()) {
                throw salah(l, "setiap musuh harus berupa objek {\"jenis\": ...}");
            }
            periksaKunci(m, KUNCI_MUSUH, l);
            String jenis = teks(m, "jenis", true, l);
            if (!PabrikKarakter.JENIS_MUSUH.contains(jenis)) {
                throw salah(l + ".jenis", "jenis tidak dikenal: \"" + jenis + "\". Yang dikenal: "
                        + new TreeSet<>(PabrikKarakter.JENIS_MUSUH));
            }
            String nama = teks(m, "nama", false, l);
            if (nama != null && PabrikKarakter.JENIS_BERNAMA_TETAP.contains(jenis)) {
                throw salah(l + ".nama", "\"" + jenis + "\" bernama tetap; hapus \"nama\"");
            }
            String namaTampil = nama != null ? nama : PabrikKarakter.buatMusuh(jenis, null).getNama();
            if (!namaDipakai.add(namaTampil)) {
                throw salah(l + ".nama", "nama \"" + namaTampil
                        + "\" sudah dipakai musuh lain; beri nama berbeda, mis. \"Leak 1\" dan \"Leak 2\"");
            }
            musuh.add(new RencanaPertarungan.Musuh(jenis, nama));
        }
        if (batasRonde == 0 && ritual) {
            throw salah("ritual", "pertarungan ritual butuh \"batasRonde\" lebih dari 0");
        }
        if (batasRonde == 0 && waktuHabis != null) {
            throw salah("waktuHabis", "hanya bisa dipakai jika \"batasRonde\" lebih dari 0");
        }
        return new RencanaPertarungan(id, arena, mode, musuh, batasRonde, menang, ritual, waktuHabis);
    }

    private String teks(JsonValue objek, String kunci, boolean wajib) throws PertarunganTidakValidException {
        return teks(objek, kunci, wajib, "");
    }

    private String teks(JsonValue objek, String kunci, boolean wajib, String letak)
            throws PertarunganTidakValidException {
        String l = letak.isEmpty() ? kunci : letak + "." + kunci;
        JsonValue v = objek.get(kunci);
        if (v == null || v.isNull()) {
            if (wajib) {
                throw salah(l, "wajib diisi");
            }
            return null;
        }
        if (!v.isString() || v.asString().isBlank()) {
            throw salah(l, "harus berupa teks yang tidak kosong");
        }
        return v.asString();
    }

    private void periksaKunci(JsonValue objek, Set<String> boleh, String letak) throws PertarunganTidakValidException {
        Set<String> sudahAda = new HashSet<>();
        for (JsonValue c = objek.child; c != null; c = c.next) {
            String l = letak.isEmpty() ? c.name : letak + "." + c.name;
            if (!boleh.contains(c.name)) {
                throw salah(l, "kunci tidak dikenal. Yang boleh: " + new TreeSet<>(boleh));
            }
            if (!sudahAda.add(c.name)) {
                throw salah(l, "kunci ditulis dua kali");
            }
        }
    }

    private PertarunganTidakValidException salah(String letak, String masalah) {
        return new PertarunganTidakValidException(id, letak, masalah);
    }

    private PertarunganTidakValidException salah(String letak, String masalah, Throwable sebab) {
        return new PertarunganTidakValidException(id, letak, masalah, sebab);
    }
}
