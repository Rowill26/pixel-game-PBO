package id.girah.cerita;

import id.girah.battle.StatusBattle;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Semua yang diingat cerita sepanjang lima babak: nilai kepercayaan, jumlah warga selamat,
 * keputusan penting, dan hasil ritual Babak 5. Satu objek untuk satu permainan.
 *
 * <p>Status hanya berubah lewat {@link #terapkan(Efek)} (dari pilihan di naskah),
 * {@link #tambahNilai}, {@link #tandai}, dan {@link #catatHasilRitual}.
 */
public class StatusCerita {

    private final Map<NilaiCerita, Integer> nilai = new EnumMap<>(NilaiCerita.class);
    private final Set<PenandaCerita> penanda = EnumSet.noneOf(PenandaCerita.class);
    private Boolean ritualDihentikanTepatWaktu;   // null = pertarungan ritual belum selesai

    /** Nilai saat ini; 0 jika belum pernah berubah. */
    public int getNilai(NilaiCerita n) {
        return nilai.getOrDefault(Objects.requireNonNull(n, "Nilai tidak boleh null"), 0);
    }

    /** Menambah (atau mengurangi, jika negatif) sebuah nilai. */
    public void tambahNilai(NilaiCerita n, int jumlah) {
        Objects.requireNonNull(n, "Nilai tidak boleh null");
        nilai.merge(n, jumlah, Integer::sum);
    }

    public boolean punya(PenandaCerita p) {
        return penanda.contains(Objects.requireNonNull(p, "Penanda tidak boleh null"));
    }

    public void tandai(PenandaCerita p) {
        penanda.add(Objects.requireNonNull(p, "Penanda tidak boleh null"));
    }

    /** Menerapkan efek sebuah pilihan. */
    public void terapkan(Efek efek) {
        Objects.requireNonNull(efek, "Efek tidak boleh null");
        for (Map.Entry<NilaiCerita, Integer> e : efek.getTambahan().entrySet()) {
            tambahNilai(e.getKey(), e.getValue());
        }
        for (PenandaCerita p : efek.getTandai()) {
            tandai(p);
        }
    }

    /**
     * Mencatat hasil pertarungan fase terakhir Babak 5 (menghancurkan sumber ritual).
     *
     * @param hasil {@link StatusBattle#MENANG} = ritual dihentikan tepat waktu,
     *              {@link StatusBattle#WAKTU_HABIS} = batas ronde habis
     * @throws IllegalArgumentException untuk hasil lain; {@code KALAH} berarti pertarungan diulang
     */
    public void catatHasilRitual(StatusBattle hasil) {
        Objects.requireNonNull(hasil, "Hasil tidak boleh null");
        switch (hasil) {
            case MENANG:
                ritualDihentikanTepatWaktu = true;
                break;
            case WAKTU_HABIS:
                ritualDihentikanTepatWaktu = false;
                break;
            default:
                throw new IllegalArgumentException("Hasil ritual hanya MENANG atau WAKTU_HABIS, bukan " + hasil);
        }
    }

    public boolean isHasilRitualDiketahui() {
        return ritualDihentikanTepatWaktu != null;
    }

    /** @throws IllegalStateException jika pertarungan ritual belum selesai */
    public boolean isRitualDihentikanTepatWaktu() {
        if (ritualDihentikanTepatWaktu == null) {
            throw new IllegalStateException("Pertarungan ritual Babak 5 belum selesai");
        }
        return ritualDihentikanTepatWaktu;
    }

    /** Salinan semua nilai yang pernah berubah, untuk ditampilkan atau dicatat. */
    public Map<NilaiCerita, Integer> getSemuaNilai() {
        return Collections.unmodifiableMap(new EnumMap<>(nilai));
    }

    /** Salinan semua penanda yang sudah tercatat. */
    public Set<PenandaCerita> getSemuaPenanda() {
        return penanda.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(penanda));
    }

    @Override
    public String toString() {
        return "StatusCerita" + nilai + penanda
                + (ritualDihentikanTepatWaktu == null ? "" : ", ritual tepat waktu=" + ritualDihentikanTepatWaktu);
    }
}
