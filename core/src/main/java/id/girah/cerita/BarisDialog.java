package id.girah.cerita;

import java.util.Objects;

/**
 * Satu baris dialog atau narasi. Format JSON sama dengan prototipe front end:
 * {@code {"who": "ratna", "name": "Ratna Manggali", "face": "sedih", "text": "..."}}.
 */
public final class BarisDialog extends LangkahNaskah {

    /** Nilai {@code who} untuk narator (tanpa potret). */
    public static final String NARATOR = "narator";

    private final String idPembicara;
    private final String nama;
    private final String ekspresi;
    private final String teks;

    public BarisDialog(String idPembicara, String nama, String ekspresi, String teks, Syarat syarat) {
        super(syarat);
        this.idPembicara = Objects.requireNonNull(idPembicara, "Pembicara tidak boleh null");
        this.nama = Objects.requireNonNull(nama, "Nama tidak boleh null");
        this.ekspresi = Objects.requireNonNull(ekspresi, "Ekspresi tidak boleh null");
        this.teks = Objects.requireNonNull(teks, "Teks tidak boleh null");
    }

    /** {@code who}: kunci potret, mis. "ratna", atau {@link #NARATOR}. */
    public String getIdPembicara() {
        return idPembicara;
    }

    /** {@code name}: nama di papan nama; kosong untuk narator. */
    public String getNama() {
        return nama;
    }

    /** {@code face}: ekspresi potret, mis. "netral", "sedih". */
    public String getEkspresi() {
        return ekspresi;
    }

    /** {@code text}. */
    public String getTeks() {
        return teks;
    }

    public boolean isNarator() {
        return NARATOR.equals(idPembicara);
    }

    @Override
    public String toString() {
        return (isNarator() ? "Narator" : nama) + ": " + teks;
    }
}
