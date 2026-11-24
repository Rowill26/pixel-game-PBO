package id.girah.cerita;

/**
 * Dilempar saat file naskah JSON tidak bisa dibaca atau isinya salah, mis. kunci yang
 * salah ketik atau penanda yang tidak dikenal. Pesannya menyebut nama naskah dan letak
 * kesalahannya (mis. {@code lines[6].efek[0]}), supaya penulis naskah langsung tahu
 * baris mana yang harus diperbaiki.
 */
public class NaskahTidakValidException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String namaNaskah;
    private final String letak;

    public NaskahTidakValidException(String namaNaskah, String letak, String masalah) {
        this(namaNaskah, letak, masalah, null);
    }

    public NaskahTidakValidException(String namaNaskah, String letak, String masalah, Throwable sebab) {
        super("Naskah '" + namaNaskah + "'" + (letak.isEmpty() ? "" : " di " + letak) + ": " + masalah, sebab);
        this.namaNaskah = namaNaskah;
        this.letak = letak;
    }

    public String getNamaNaskah() {
        return namaNaskah;
    }

    /** Letak kesalahan, mis. {@code "lines[6].replies[1][0].text"}; kosong untuk kesalahan seluruh file. */
    public String getLetak() {
        return letak;
    }
}
