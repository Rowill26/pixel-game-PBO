package id.girah.battle;

/**
 * Catatan lengkap seluruh narasi pertarungan, dari awal sampai akhir.
 * Isinya nanti diekspor ke file .txt dengan BufferedWriter.
 */
public class BabadPertarungan {

    private final StringBuilder isi = new StringBuilder();
    private int jumlahPeristiwa;

    /** Judul bagian, mis. "Ronde 2". */
    public void catatJudul(String judul) {
        if (isi.length() > 0) {
            isi.append('\n');
        }
        isi.append("=== ").append(judul).append(" ===\n");
    }

    /** Satu peristiwa; boleh berisi beberapa baris. */
    public void catat(String narasi) {
        if (narasi == null || narasi.isBlank()) {
            return;
        }
        isi.append(narasi.strip()).append('\n');
        jumlahPeristiwa++;
    }

    public String getIsi() {
        return isi.toString();
    }

    public int getJumlahPeristiwa() {
        return jumlahPeristiwa;
    }
}
