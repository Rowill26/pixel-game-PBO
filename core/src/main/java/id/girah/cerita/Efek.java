package id.girah.cerita;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Akibat sebuah pilihan terhadap {@link StatusCerita}: nilai yang bertambah/berkurang dan
 * penanda yang dicatat. Immutable.
 *
 * <p>Di naskah JSON: {@code {"kepercayaanRatna": 1, "tandai": ["penawar_girah_dulu"]}}.
 */
public final class Efek {

    /** Efek yang tidak mengubah apa pun. */
    public static final Efek KOSONG = new Efek(Map.of(), Set.of());

    private final Map<NilaiCerita, Integer> tambahan;
    private final Set<PenandaCerita> tandai;

    public Efek(Map<NilaiCerita, Integer> tambahan, Set<PenandaCerita> tandai) {
        this.tambahan = tambahan.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new EnumMap<>(tambahan));
        this.tandai = tandai.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(tandai));
    }

    /** Nilai yang ditambahkan (boleh negatif). */
    public Map<NilaiCerita, Integer> getTambahan() {
        return tambahan;
    }

    /** Penanda yang dicatat. */
    public Set<PenandaCerita> getTandai() {
        return tandai;
    }

    public boolean isKosong() {
        return tambahan.isEmpty() && tandai.isEmpty();
    }

    @Override
    public String toString() {
        return "Efek" + tambahan + tandai;
    }
}
