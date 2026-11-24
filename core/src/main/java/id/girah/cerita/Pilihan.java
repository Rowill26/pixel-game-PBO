package id.girah.cerita;

import java.util.ArrayList;
import java.util.List;

/**
 * Pilihan untuk pemain. Format JSON sama dengan prototipe front end, ditambah
 * {@code "efek"} dan {@code "syarat"} yang opsional (sejajar dengan {@code "choice"}):
 * <pre>
 * {"choice":  ["Mendahulukan Girah", "Mendahulukan Tapak Wangi"],
 *  "replies": [[...baris jika A...], [...baris jika B...]],
 *  "efek":    [{"kepercayaanRatna": 1, "tandai": ["penawar_girah_dulu"]},
 *              {"kepercayaanRatna": -1, "tandai": ["penawar_tapak_wangi_dulu"]}],
 *  "syarat":  [null, {"kepercayaanRatna": 3}]}
 * </pre>
 */
public final class Pilihan extends LangkahNaskah {

    private final List<OpsiPilihan> opsi;

    public Pilihan(List<OpsiPilihan> opsi) {
        super(Syarat.SELALU);
        if (opsi.isEmpty()) {
            throw new IllegalArgumentException("Pilihan harus punya minimal satu opsi");
        }
        this.opsi = List.copyOf(opsi);
    }

    /** Semua opsi, termasuk yang syaratnya belum terpenuhi. */
    public List<OpsiPilihan> getOpsi() {
        return opsi;
    }

    /** Opsi yang ditampilkan untuk {@code status} saat ini, urut seperti di naskah. */
    public List<OpsiPilihan> getOpsiTersedia(StatusCerita status) {
        List<OpsiPilihan> tersedia = new ArrayList<>();
        for (OpsiPilihan o : opsi) {
            if (o.tersediaUntuk(status)) {
                tersedia.add(o);
            }
        }
        return tersedia;
    }

    /** Pilihan tanpa opsi tersedia dilewati (tidak pernah ditampilkan). */
    @Override
    public boolean berlakuUntuk(StatusCerita status) {
        return !getOpsiTersedia(status).isEmpty();
    }

    @Override
    public String toString() {
        return "Pilihan" + opsi;
    }
}
