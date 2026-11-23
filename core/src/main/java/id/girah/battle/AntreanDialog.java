package id.girah.battle;

import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * Antrean baris narasi yang menunggu ditampilkan di kotak dialog, satu baris per klik.
 *
 * <p>Memakai {@link LinkedList} karena polanya murni antrean (FIFO): baris baru selalu
 * masuk di belakang dan baris yang ditampilkan selalu diambil dari depan. Kedua operasi
 * itu O(1) di LinkedList, sedangkan {@code ArrayList.remove(0)} harus menggeser semua elemen.
 */
public class AntreanDialog {

    private final LinkedList<String> baris = new LinkedList<>();

    /** Memecah narasi per baris lalu memasukkannya ke belakang antrean. Baris kosong dibuang. */
    public void tambah(String narasi) {
        if (narasi == null) {
            return;
        }
        for (String satuBaris : narasi.split("\n")) {
            if (!satuBaris.isBlank()) {
                baris.addLast(satuBaris.strip());
            }
        }
    }

    /**
     * Mengambil dan membuang baris paling depan.
     *
     * @throws NoSuchElementException jika antrean kosong; cek {@link #adaBaris()} dulu
     */
    public String ambilBerikutnya() {
        return baris.removeFirst();
    }

    /** Melihat baris paling depan tanpa membuangnya, atau {@code null} jika kosong. */
    public String lihatBerikutnya() {
        return baris.peekFirst();
    }

    public boolean adaBaris() {
        return !baris.isEmpty();
    }

    public int jumlah() {
        return baris.size();
    }

    /** Dipakai saat pemain menekan "lewati dialog". */
    public void kosongkan() {
        baris.clear();
    }
}
