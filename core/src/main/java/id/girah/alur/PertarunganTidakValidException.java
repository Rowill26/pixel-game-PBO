package id.girah.alur;

/**
 * Dilempar saat file {@code assets/pertarungan/<id>.json} tidak bisa dibaca atau isinya salah.
 * Pesannya menyebut id pertarungan dan letak kesalahannya, mis. {@code musuh[2].jenis}.
 */
public class PertarunganTidakValidException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String idPertarungan;
    private final String letak;

    public PertarunganTidakValidException(String idPertarungan, String letak, String masalah) {
        this(idPertarungan, letak, masalah, null);
    }

    public PertarunganTidakValidException(String idPertarungan, String letak, String masalah, Throwable sebab) {
        super("Pertarungan '" + idPertarungan + "'" + (letak.isEmpty() ? "" : " di " + letak) + ": " + masalah, sebab);
        this.idPertarungan = idPertarungan;
        this.letak = letak;
    }

    public String getIdPertarungan() {
        return idPertarungan;
    }

    /** Letak kesalahan, mis. {@code "musuh[2].jenis"}; kosong untuk kesalahan seluruh file. */
    public String getLetak() {
        return letak;
    }
}
