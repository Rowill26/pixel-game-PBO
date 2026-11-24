package id.girah.cerita;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.SerializationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Membaca naskah JSON menjadi {@link Naskah}.
 *
 * <p>Format dasarnya sama persis dengan yang dibaca {@code StoryLayer} di prototipe front end
 * ({@code title}, {@code mission}, {@code arena}, {@code mode}, {@code lines}, {@code then};
 * baris {@code who}/{@code name}/{@code face}/{@code text}; pilihan {@code choice}/{@code replies}),
 * jadi naskah lama tetap terbaca. Tambahan yang opsional: {@code efek} dan {@code syarat}
 * (lihat {@link Pilihan}, {@link Efek}, {@link Syarat}).
 *
 * <p>Pembaca ini ketat: kunci yang tidak dikenal, penanda yang salah ketik, panjang daftar
 * yang tidak sejajar, atau teks kosong langsung ditolak dengan {@link NaskahTidakValidException}
 * yang menyebut letaknya. Kesalahan naskah lebih baik ketahuan saat dimuat daripada membuat
 * sebuah ending diam-diam tidak bisa dicapai.
 *
 * <p>Hanya memakai {@code JsonReader} dari LibGDX (pustaka biasa, tanpa grafis), jadi bisa
 * diuji dengan JUnit dan dipakai di mode teks.
 */
public final class PembacaNaskah {

    private static final Set<String> KUNCI_NASKAH = Set.of("title", "mission", "arena", "mode", "lines", "then");
    private static final Set<String> KUNCI_BARIS = Set.of("who", "name", "face", "text", "syarat");
    private static final Set<String> KUNCI_PILIHAN = Set.of("choice", "replies", "efek", "syarat");

    private static final String KUNCI_TANDAI = "tandai";
    private static final String KUNCI_KURANG_DARI = "kurangDari";
    private static final String KUNCI_PENANDA = "penanda";
    private static final String KUNCI_TANPA_PENANDA = "tanpaPenanda";
    private static final String KUNCI_PEMULIHAN = "jalanPemulihanTerbuka";
    private static final String KUNCI_WENING = "weningSelamat";
    private static final String KUNCI_ENDING = "ending";

    private final String namaNaskah;

    private PembacaNaskah(String namaNaskah) {
        this.namaNaskah = namaNaskah;
    }

    /**
     * Membaca naskah dari teks JSON.
     *
     * @param nama nama naskah untuk pesan kesalahan dan {@link Naskah#getNama()}, mis. "babak2"
     */
    public static Naskah baca(String nama, String json) throws NaskahTidakValidException {
        Objects.requireNonNull(nama, "Nama naskah tidak boleh null");
        Objects.requireNonNull(json, "Isi naskah tidak boleh null");
        return new PembacaNaskah(nama).bacaAkar(json);
    }

    /** Membaca naskah dari file UTF-8. Nama naskah = nama file tanpa {@code .json}. */
    public static Naskah bacaFile(Path file) throws IOException, NaskahTidakValidException {
        Objects.requireNonNull(file, "File tidak boleh null");
        String namaFile = file.getFileName().toString();
        String nama = namaFile.endsWith(".json") ? namaFile.substring(0, namaFile.length() - 5) : namaFile;
        return baca(nama, Files.readString(file, StandardCharsets.UTF_8));
    }

    // ================================================================

    private Naskah bacaAkar(String json) throws NaskahTidakValidException {
        JsonValue akar;
        try {
            akar = new JsonReader().parse(json);
        } catch (SerializationException e) {
            throw salah("", "bukan JSON yang sah (" + e.getMessage() + ")", e);
        }
        if (akar == null || !akar.isObject()) {
            throw salah("", "isi file harus berupa objek JSON { ... }");
        }
        periksaKunci(akar, KUNCI_NASKAH, "");

        JsonValue lines = akar.get("lines");
        if (lines == null || !lines.isArray()) {
            throw salah("lines", "wajib ada dan berupa daftar [ ... ]");
        }
        return new Naskah(namaNaskah,
                teksOpsional(akar, "title", "", ""),
                teksOpsional(akar, "mission", "", ""),
                teksAtauNull(akar, "arena", ""),
                teksAtauNull(akar, "mode", ""),
                bacaDaftarLangkah(lines, "lines"),
                teksAtauNull(akar, "then", ""));
    }

    private List<LangkahNaskah> bacaDaftarLangkah(JsonValue daftar, String letak) throws NaskahTidakValidException {
        List<LangkahNaskah> hasil = new ArrayList<>();
        int i = 0;
        for (JsonValue v = daftar.child; v != null; v = v.next, i++) {
            hasil.add(bacaLangkah(v, letak + "[" + i + "]"));
        }
        return hasil;
    }

    private LangkahNaskah bacaLangkah(JsonValue v, String letak) throws NaskahTidakValidException {
        if (!v.isObject()) {
            throw salah(letak, "setiap baris harus berupa objek { ... }");
        }
        return v.has("choice") ? bacaPilihan(v, letak) : bacaBaris(v, letak);
    }

    private BarisDialog bacaBaris(JsonValue v, String letak) throws NaskahTidakValidException {
        periksaKunci(v, KUNCI_BARIS, letak);
        String teks = teksOpsional(v, "text", null, letak);
        if (teks == null || teks.isBlank()) {
            throw salah(letak + ".text", "teks dialog wajib diisi");
        }
        String pembicara = teksOpsional(v, "who", BarisDialog.NARATOR, letak);
        if (pembicara.isBlank()) {
            throw salah(letak + ".who", "tidak boleh kosong; pakai \"narator\" untuk narasi");
        }
        JsonValue syarat = v.get("syarat");
        return new BarisDialog(pembicara,
                teksOpsional(v, "name", "", letak),
                teksOpsional(v, "face", "netral", letak),
                teks,
                syarat == null || syarat.isNull() ? Syarat.SELALU : bacaSyarat(syarat, letak + ".syarat"));
    }

    private Pilihan bacaPilihan(JsonValue v, String letak) throws NaskahTidakValidException {
        periksaKunci(v, KUNCI_PILIHAN, letak);
        JsonValue choice = v.get("choice");
        if (!choice.isArray() || choice.size == 0) {
            throw salah(letak + ".choice", "harus berupa daftar teks opsi yang tidak kosong");
        }
        int n = choice.size;
        JsonValue replies = daftarSejajar(v, "replies", n, letak);
        JsonValue efek = daftarSejajar(v, "efek", n, letak);
        JsonValue syarat = daftarSejajar(v, "syarat", n, letak);

        List<OpsiPilihan> opsi = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            JsonValue teks = choice.get(i);
            if (!teks.isString() || teks.asString().isBlank()) {
                throw salah(letak + ".choice[" + i + "]", "teks opsi wajib diisi");
            }
            List<LangkahNaskah> balasan = List.of();
            if (replies != null) {
                JsonValue r = replies.get(i);
                if (!r.isArray()) {
                    throw salah(letak + ".replies[" + i + "]", "harus berupa daftar baris [ ... ]");
                }
                balasan = bacaDaftarLangkah(r, letak + ".replies[" + i + "]");
            }
            Efek e = Efek.KOSONG;
            if (efek != null && !efek.get(i).isNull()) {
                e = bacaEfek(efek.get(i), letak + ".efek[" + i + "]");
            }
            Syarat s = Syarat.SELALU;
            if (syarat != null && !syarat.get(i).isNull()) {
                s = bacaSyarat(syarat.get(i), letak + ".syarat[" + i + "]");
            }
            opsi.add(new OpsiPilihan(teks.asString(), balasan, e, s));
        }
        return new Pilihan(opsi);
    }

    /** Daftar opsional yang panjangnya harus sama dengan jumlah opsi; {@code null} jika tidak ada. */
    private JsonValue daftarSejajar(JsonValue pilihan, String kunci, int jumlahOpsi, String letak)
            throws NaskahTidakValidException {
        JsonValue d = pilihan.get(kunci);
        if (d == null || d.isNull()) {
            return null;
        }
        if (!d.isArray() || d.size != jumlahOpsi) {
            throw salah(letak + "." + kunci, "harus berupa daftar berisi " + jumlahOpsi
                    + " item, sejajar dengan \"choice\" (pakai null untuk opsi tanpa " + kunci + ")");
        }
        return d;
    }

    private Efek bacaEfek(JsonValue v, String letak) throws NaskahTidakValidException {
        if (!v.isObject()) {
            throw salah(letak, "harus berupa objek { ... } atau null");
        }
        Set<String> boleh = new HashSet<>(kunciNilai());
        boleh.add(KUNCI_TANDAI);
        periksaKunci(v, boleh, letak);

        Map<NilaiCerita, Integer> tambahan = new EnumMap<>(NilaiCerita.class);
        Set<PenandaCerita> tandai = EnumSet.noneOf(PenandaCerita.class);
        for (JsonValue c = v.child; c != null; c = c.next) {
            if (KUNCI_TANDAI.equals(c.name)) {
                tandai.addAll(bacaDaftarPenanda(c, letak + "." + KUNCI_TANDAI));
            } else {
                tambahan.put(NilaiCerita.dariKunci(c.name), bacaBulat(c, letak + "." + c.name));
            }
        }
        return new Efek(tambahan, tandai);
    }

    private Syarat bacaSyarat(JsonValue v, String letak) throws NaskahTidakValidException {
        if (!v.isObject()) {
            throw salah(letak, "harus berupa objek { ... } atau null");
        }
        Set<String> boleh = new HashSet<>(kunciNilai());
        boleh.addAll(Set.of(KUNCI_KURANG_DARI, KUNCI_PENANDA, KUNCI_TANPA_PENANDA,
                KUNCI_PEMULIHAN, KUNCI_WENING, KUNCI_ENDING));
        periksaKunci(v, boleh, letak);

        Map<NilaiCerita, Integer> minimal = new EnumMap<>(NilaiCerita.class);
        Map<NilaiCerita, Integer> kurangDari = new EnumMap<>(NilaiCerita.class);
        Set<PenandaCerita> penanda = EnumSet.noneOf(PenandaCerita.class);
        Set<PenandaCerita> tanpaPenanda = EnumSet.noneOf(PenandaCerita.class);
        Boolean pemulihan = null;
        Boolean wening = null;
        Ending ending = null;

        for (JsonValue c = v.child; c != null; c = c.next) {
            String l = letak + "." + c.name;
            switch (c.name) {
                case KUNCI_KURANG_DARI:
                    if (!c.isObject()) {
                        throw salah(l, "harus berupa objek, mis. {\"kepercayaanRatna\": 3}");
                    }
                    periksaKunci(c, kunciNilai(), l);
                    for (JsonValue k = c.child; k != null; k = k.next) {
                        kurangDari.put(NilaiCerita.dariKunci(k.name), bacaBulat(k, l + "." + k.name));
                    }
                    break;
                case KUNCI_PENANDA:
                    penanda.addAll(bacaDaftarPenanda(c, l));
                    break;
                case KUNCI_TANPA_PENANDA:
                    tanpaPenanda.addAll(bacaDaftarPenanda(c, l));
                    break;
                case KUNCI_PEMULIHAN:
                    pemulihan = bacaBoolean(c, l);
                    break;
                case KUNCI_WENING:
                    wening = bacaBoolean(c, l);
                    break;
                case KUNCI_ENDING:
                    if (!c.isString() || Ending.dariNama(c.asString()) == null) {
                        throw salah(l, "harus salah satu dari " + Arrays.toString(Ending.values()));
                    }
                    ending = Ending.dariNama(c.asString());
                    break;
                default:
                    minimal.put(NilaiCerita.dariKunci(c.name), bacaBulat(c, l));
            }
        }
        return new Syarat(minimal, kurangDari, penanda, tanpaPenanda, pemulihan, wening, ending);
    }

    // ================================================================
    // Pembantu
    // ================================================================

    private Set<PenandaCerita> bacaDaftarPenanda(JsonValue v, String letak) throws NaskahTidakValidException {
        if (!v.isArray()) {
            throw salah(letak, "harus berupa daftar penanda, mis. [\"bahula_jujur\"]");
        }
        Set<PenandaCerita> hasil = EnumSet.noneOf(PenandaCerita.class);
        int i = 0;
        for (JsonValue c = v.child; c != null; c = c.next, i++) {
            PenandaCerita p = c.isString() ? PenandaCerita.dariKunci(c.asString()) : null;
            if (p == null) {
                String ditulis = c.isString() ? "\"" + c.asString() + "\"" : String.valueOf(c);
                throw salah(letak + "[" + i + "]", "penanda tidak dikenal: " + ditulis
                        + ". Yang dikenal: " + daftarKunciPenanda());
            }
            hasil.add(p);
        }
        return hasil;
    }

    private int bacaBulat(JsonValue v, String letak) throws NaskahTidakValidException {
        if (!v.isLong()) {
            throw salah(letak, "harus berupa bilangan bulat");
        }
        long n = v.asLong();
        if (n < Integer.MIN_VALUE || n > Integer.MAX_VALUE) {
            throw salah(letak, "angka terlalu besar");
        }
        return (int) n;
    }

    private boolean bacaBoolean(JsonValue v, String letak) throws NaskahTidakValidException {
        if (!v.isBoolean()) {
            throw salah(letak, "harus true atau false");
        }
        return v.asBoolean();
    }

    /** Teks opsional; {@code bawaan} jika kunci tidak ada atau bernilai null. */
    private String teksOpsional(JsonValue objek, String kunci, String bawaan, String letak)
            throws NaskahTidakValidException {
        JsonValue v = objek.get(kunci);
        if (v == null || v.isNull()) {
            return bawaan;
        }
        if (!v.isString()) {
            throw salah(sambung(letak, kunci), "harus berupa teks \"...\"");
        }
        return v.asString();
    }

    private String teksAtauNull(JsonValue objek, String kunci, String letak) throws NaskahTidakValidException {
        String t = teksOpsional(objek, kunci, null, letak);
        return t == null || t.isBlank() ? null : t;
    }

    /** Menolak kunci yang tidak dikenal atau ditulis dua kali. */
    private void periksaKunci(JsonValue objek, Set<String> boleh, String letak) throws NaskahTidakValidException {
        Set<String> sudahAda = new HashSet<>();
        for (JsonValue c = objek.child; c != null; c = c.next) {
            if (!boleh.contains(c.name)) {
                throw salah(sambung(letak, c.name), "kunci tidak dikenal. Yang boleh: " + new TreeSet<>(boleh));
            }
            if (!sudahAda.add(c.name)) {
                throw salah(sambung(letak, c.name), "kunci ditulis dua kali");
            }
        }
    }

    private static Set<String> kunciNilai() {
        Set<String> kunci = new HashSet<>();
        for (NilaiCerita n : NilaiCerita.values()) {
            kunci.add(n.getKunciJson());
        }
        return kunci;
    }

    private static String daftarKunciPenanda() {
        List<String> kunci = new ArrayList<>();
        for (PenandaCerita p : PenandaCerita.values()) {
            kunci.add(p.getKunciJson());
        }
        return kunci.toString();
    }

    private static String sambung(String letak, String kunci) {
        return letak.isEmpty() ? kunci : letak + "." + kunci;
    }

    private NaskahTidakValidException salah(String letak, String masalah) {
        return new NaskahTidakValidException(namaNaskah, letak, masalah);
    }

    private NaskahTidakValidException salah(String letak, String masalah, Throwable sebab) {
        return new NaskahTidakValidException(namaNaskah, letak, masalah, sebab);
    }
}
