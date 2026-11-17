package id.girah.pusaka;

import id.girah.karakter.KarakterMitos;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Tas Pusaka milik kelompok pengelana: pusaka -> jumlah.
 *
 * <p>Memakai {@link HashMap} karena pola aksesnya adalah "berapa jumlah pusaka X?",
 * yaitu pencarian berdasarkan kunci, O(1) rata-rata. Pusaka yang habis dihapus dari map,
 * sehingga isi tas hanya berisi pusaka yang benar-benar ada.
 */
public class TasPusaka {

    private final Map<Pusaka, Integer> isi = new HashMap<>();

    public void tambah(Pusaka pusaka, int jumlah) {
        Objects.requireNonNull(pusaka, "Pusaka tidak boleh null");
        if (jumlah <= 0) {
            throw new IllegalArgumentException("Jumlah pusaka yang ditambah harus > 0");
        }
        isi.merge(pusaka, jumlah, Integer::sum);
    }

    public int jumlah(Pusaka pusaka) {
        return isi.getOrDefault(pusaka, 0);
    }

    public boolean punya(Pusaka pusaka) {
        return jumlah(pusaka) > 0;
    }

    public boolean isKosong() {
        return isi.isEmpty();
    }

    /**
     * Memakai satu pusaka dari tas.
     *
     * @throws IllegalStateException    jika pusaka tidak ada di tas
     * @throws IllegalArgumentException jika pusaka tidak berguna bagi target (tas tidak berkurang)
     */
    public String gunakan(Pusaka pusaka, KarakterMitos pengguna, KarakterMitos target) {
        Objects.requireNonNull(pusaka, "Pusaka tidak boleh null");
        Objects.requireNonNull(pengguna, "Pengguna tidak boleh null");
        Objects.requireNonNull(target, "Target tidak boleh null");
        int sisa = jumlah(pusaka);
        if (sisa == 0) {
            throw new IllegalStateException(pusaka.getNama() + " tidak ada di Tas Pusaka");
        }
        if (!pusaka.bisaDipakaiPada(target)) {
            throw new IllegalArgumentException(pusaka.getNama() + " tidak berguna bagi " + target.getNama());
        }

        if (sisa == 1) {
            isi.remove(pusaka);
        } else {
            isi.put(pusaka, sisa - 1);
        }
        return pusaka.gunakan(pengguna, target);
    }

    /** Pusaka yang ada di tas, urut nama, untuk ditampilkan di menu. */
    public List<Pusaka> getDaftarPusaka() {
        List<Pusaka> daftar = new ArrayList<>(isi.keySet());
        daftar.sort(Comparator.comparing(Pusaka::getNama));
        return daftar;
    }

    /** Salinan isi tas yang tidak bisa diubah. */
    public Map<Pusaka, Integer> getIsi() {
        return Map.copyOf(isi);
    }
}
