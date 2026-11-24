package id.girah.cerita;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Syarat agar sebuah baris dialog tampil atau sebuah opsi pilihan tersedia.
 * Semua bagian harus terpenuhi (DAN). Bagian yang tidak diisi tidak diperiksa. Immutable.
 *
 * <p>Di naskah JSON, semua kunci opsional:
 * <pre>
 * {
 *   "kepercayaanRatna": 3,                 nilai minimal (&gt;=)
 *   "kurangDari": {"kepercayaanRatna": 3}, nilai harus &lt; angka ini
 *   "penanda": ["bahula_jujur"],           penanda yang harus ada
 *   "tanpaPenanda": ["kitab_diambil_diam_diam"],
 *   "jalanPemulihanTerbuka": true,         dihitung oleh PenentuEnding
 *   "weningSelamat": true,                 dihitung oleh PenentuEnding
 *   "ending": "JALAN_PEMULIHAN"            ending yang sudah tercapai
 * }
 * </pre>
 */
public final class Syarat {

    /** Syarat yang selalu terpenuhi. */
    public static final Syarat SELALU = new Syarat(Map.of(), Map.of(), Set.of(), Set.of(), null, null, null);

    private final Map<NilaiCerita, Integer> minimal;
    private final Map<NilaiCerita, Integer> kurangDari;
    private final Set<PenandaCerita> penanda;
    private final Set<PenandaCerita> tanpaPenanda;
    private final Boolean jalanPemulihanTerbuka;
    private final Boolean weningSelamat;
    private final Ending ending;

    /**
     * @param jalanPemulihanTerbuka {@code null} = tidak diperiksa
     * @param weningSelamat         {@code null} = tidak diperiksa
     * @param ending                {@code null} = tidak diperiksa
     */
    public Syarat(Map<NilaiCerita, Integer> minimal, Map<NilaiCerita, Integer> kurangDari,
                  Set<PenandaCerita> penanda, Set<PenandaCerita> tanpaPenanda,
                  Boolean jalanPemulihanTerbuka, Boolean weningSelamat, Ending ending) {
        this.minimal = salinNilai(minimal);
        this.kurangDari = salinNilai(kurangDari);
        this.penanda = salinPenanda(penanda);
        this.tanpaPenanda = salinPenanda(tanpaPenanda);
        this.jalanPemulihanTerbuka = jalanPemulihanTerbuka;
        this.weningSelamat = weningSelamat;
        this.ending = ending;
    }

    /** {@code true} jika semua bagian syarat terpenuhi oleh {@code status}. */
    public boolean terpenuhi(StatusCerita status) {
        for (Map.Entry<NilaiCerita, Integer> e : minimal.entrySet()) {
            if (status.getNilai(e.getKey()) < e.getValue()) {
                return false;
            }
        }
        for (Map.Entry<NilaiCerita, Integer> e : kurangDari.entrySet()) {
            if (status.getNilai(e.getKey()) >= e.getValue()) {
                return false;
            }
        }
        for (PenandaCerita p : penanda) {
            if (!status.punya(p)) {
                return false;
            }
        }
        for (PenandaCerita p : tanpaPenanda) {
            if (status.punya(p)) {
                return false;
            }
        }
        if (jalanPemulihanTerbuka != null && PenentuEnding.jalanPemulihanTerbuka(status) != jalanPemulihanTerbuka) {
            return false;
        }
        if (weningSelamat != null && PenentuEnding.weningSelamat(status) != weningSelamat) {
            return false;
        }
        if (ending != null) {
            // ending baru diketahui setelah pertarungan ritual selesai
            return status.isHasilRitualDiketahui() && PenentuEnding.tentukan(status) == ending;
        }
        return true;
    }

    public boolean isSelalu() {
        return minimal.isEmpty() && kurangDari.isEmpty() && penanda.isEmpty() && tanpaPenanda.isEmpty()
                && jalanPemulihanTerbuka == null && weningSelamat == null && ending == null;
    }

    @Override
    public String toString() {
        return isSelalu() ? "Syarat[selalu]"
                : "Syarat[min=" + minimal + ", kurangDari=" + kurangDari + ", penanda=" + penanda
                  + ", tanpa=" + tanpaPenanda + ", pemulihan=" + jalanPemulihanTerbuka
                  + ", wening=" + weningSelamat + ", ending=" + ending + "]";
    }

    private static Map<NilaiCerita, Integer> salinNilai(Map<NilaiCerita, Integer> m) {
        return m.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(new EnumMap<>(m));
    }

    private static Set<PenandaCerita> salinPenanda(Set<PenandaCerita> s) {
        return s.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(EnumSet.copyOf(s));
    }
}
