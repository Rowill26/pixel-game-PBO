package id.girah.ai;

import id.girah.karakter.KarakterMitos;
import java.util.List;

/**
 * Cara musuh memilih aksi. Dipisah dari thread supaya logikanya bisa diuji tanpa menunggu,
 * dan supaya musuh lain (mis. pemimpin pengikut Babak 4) bisa diberi strategi berbeda.
 */
public interface StrategiMusuh {

    /**
     * @param aktor         musuh yang sedang mendapat giliran
     * @param sasaranSerang lawan hidup yang boleh diserang (tidak kosong)
     * @param sasaranJurus  karakter yang boleh menjadi sasaran jurus aktor
     */
    KeputusanAksi tentukan(KarakterMitos aktor, List<KarakterMitos> sasaranSerang, List<KarakterMitos> sasaranJurus);
}
