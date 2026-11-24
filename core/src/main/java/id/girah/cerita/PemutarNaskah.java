package id.girah.cerita;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Menjalankan satu {@link Naskah} langkah demi langkah untuk layar cerita.
 *
 * <pre>
 * PemutarNaskah p = new PemutarNaskah(naskah, statusCerita);
 * while (!p.isSelesai()) {
 *     if (p.isPilihan()) {
 *         List&lt;OpsiPilihan&gt; opsi = p.getOpsiTersedia();   // tampilkan sebagai tombol
 *         p.pilih(indeksYangDipilih);                       // efek diterapkan, balasan menyusul
 *     } else {
 *         BarisDialog b = p.getBaris();                      // tampilkan potret + teks
 *         p.lanjut();                                        // saat pemain menekan Enter
 *     }
 * }
 * String berikutnya = p.getLanjutan();                       // "then": arena/pertarungan berikutnya
 * </pre>
 *
 * <p>Langkah yang syaratnya tidak terpenuhi dilewati otomatis. Antrean langkah memakai
 * {@link LinkedList}: balasan sebuah pilihan disisipkan di <b>depan</b> antrean (O(1)),
 * sehingga tampil tepat setelah pilihan itu, sebelum sisa naskah.
 */
public class PemutarNaskah {

    private final Naskah naskah;
    private final StatusCerita status;
    private final LinkedList<LangkahNaskah> antrean;
    private LangkahNaskah sekarang;

    public PemutarNaskah(Naskah naskah, StatusCerita status) {
        this.naskah = Objects.requireNonNull(naskah, "Naskah tidak boleh null");
        this.status = Objects.requireNonNull(status, "Status cerita tidak boleh null");
        this.antrean = new LinkedList<>(naskah.getLangkah());
        majuKeLangkahBerlaku();
    }

    /** {@code true} jika semua langkah sudah dijalankan. */
    public boolean isSelesai() {
        return sekarang == null;
    }

    /** {@code true} jika langkah sekarang adalah pilihan (bukan baris dialog). */
    public boolean isPilihan() {
        return langkahSekarang() instanceof Pilihan;
    }

    /**
     * Baris dialog yang sedang ditampilkan.
     *
     * @throws IllegalStateException jika naskah selesai atau langkah sekarang adalah pilihan
     */
    public BarisDialog getBaris() {
        LangkahNaskah l = langkahSekarang();
        if (!(l instanceof BarisDialog)) {
            throw new IllegalStateException("Langkah sekarang adalah pilihan; pakai getOpsiTersedia()");
        }
        return (BarisDialog) l;
    }

    /**
     * Opsi yang ditampilkan untuk pilihan sekarang. Indeks daftar ini dipakai {@link #pilih(int)}.
     *
     * @throws IllegalStateException jika langkah sekarang bukan pilihan
     */
    public List<OpsiPilihan> getOpsiTersedia() {
        LangkahNaskah l = langkahSekarang();
        if (!(l instanceof Pilihan)) {
            throw new IllegalStateException("Langkah sekarang bukan pilihan");
        }
        return ((Pilihan) l).getOpsiTersedia(status);
    }

    /**
     * Melanjutkan ke langkah berikutnya setelah sebuah baris dialog dibaca.
     *
     * @throws IllegalStateException jika langkah sekarang adalah pilihan (harus {@link #pilih(int)})
     */
    public void lanjut() {
        getBaris();   // memastikan langkah sekarang adalah baris dialog
        majuKeLangkahBerlaku();
    }

    /**
     * Memilih opsi ke-{@code indeks} dari {@link #getOpsiTersedia()}: efeknya diterapkan ke
     * status cerita, lalu balasannya dijalankan sebelum sisa naskah.
     *
     * @throws IndexOutOfBoundsException jika indeks di luar daftar opsi tersedia
     */
    public void pilih(int indeks) {
        List<OpsiPilihan> opsi = getOpsiTersedia();
        if (indeks < 0 || indeks >= opsi.size()) {
            throw new IndexOutOfBoundsException("Opsi ke-" + indeks + " tidak ada; tersedia " + opsi.size());
        }
        OpsiPilihan dipilih = opsi.get(indeks);
        status.terapkan(dipilih.getEfek());
        List<LangkahNaskah> balasan = dipilih.getBalasan();
        for (int i = balasan.size() - 1; i >= 0; i--) {
            antrean.addFirst(balasan.get(i));
        }
        majuKeLangkahBerlaku();
    }

    public Naskah getNaskah() {
        return naskah;
    }

    public StatusCerita getStatus() {
        return status;
    }

    /** {@code "then"} naskah: dimuat setelah naskah selesai, atau {@code null}. */
    public String getLanjutan() {
        return naskah.getLanjutan();
    }

    private LangkahNaskah langkahSekarang() {
        if (sekarang == null) {
            throw new IllegalStateException("Naskah '" + naskah.getNama() + "' sudah selesai");
        }
        return sekarang;
    }

    /** Mengambil langkah berikutnya, melewati yang syaratnya tidak terpenuhi. */
    private void majuKeLangkahBerlaku() {
        sekarang = antrean.pollFirst();
        while (sekarang != null && !sekarang.berlakuUntuk(status)) {
            sekarang = antrean.pollFirst();
        }
    }
}
