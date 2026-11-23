package id.girah.ai;

import id.girah.karakter.KarakterMitos;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Strategi bawaan untuk semua makhluk Girah. Aturannya membaca jenis jurus aktor,
 * jadi tidak perlu {@code instanceof CalonArang} atau {@code instanceof Leak}:
 * <ul>
 *   <li><b>Jurus area</b> (Sebar Wabah): dipakai jika mengenai minimal dua lawan.</li>
 *   <li><b>Jurus satu lawan</b> (Jerat Kabut): dipakai dengan peluang
 *       {@value #PELUANG_JURUS_TUNGGAL}, ke lawan dengan nyawa terendah.</li>
 *   <li><b>Jurus satu kawan</b> (penyembuhan): dipakai jika ada kawan yang nyawanya
 *       di bawah {@value #AMBANG_KAWAN_KRITIS_PERSEN}%.</li>
 *   <li>Selain itu: serang lawan dengan nyawa terendah.</li>
 * </ul>
 */
public class StrategiDasar implements StrategiMusuh {

    public static final double PELUANG_JURUS_TUNGGAL = 0.35;
    public static final int AMBANG_KAWAN_KRITIS_PERSEN = 50;

    private final Random acak;

    public StrategiDasar() {
        this(new Random());
    }

    /** Random disuntikkan agar keputusan bisa diuji secara pasti. */
    public StrategiDasar(Random acak) {
        this.acak = Objects.requireNonNull(acak, "Random tidak boleh null");
    }

    @Override
    public KeputusanAksi tentukan(KarakterMitos aktor, List<KarakterMitos> sasaranSerang,
                                  List<KarakterMitos> sasaranJurus) {
        Objects.requireNonNull(aktor, "Aktor tidak boleh null");
        if (sasaranSerang == null || sasaranSerang.isEmpty()) {
            throw new IllegalArgumentException("Tidak ada lawan yang bisa diserang");
        }

        if (aktor.bisaMemakaiJurus() && sasaranJurus != null && !sasaranJurus.isEmpty()) {
            switch (aktor.getSasaranJurus()) {
                case SEMUA_LAWAN:
                    if (sasaranJurus.size() >= 2) {
                        return KeputusanAksi.jurus(null);
                    }
                    break;
                case SATU_LAWAN:
                    if (acak.nextDouble() < PELUANG_JURUS_TUNGGAL) {
                        return KeputusanAksi.jurus(terlemah(sasaranJurus));
                    }
                    break;
                case SATU_KAWAN:
                    KarakterMitos kawan = terlemah(sasaranJurus);
                    if (kawan.getNyawa() * 100 < kawan.getNyawaMaks() * AMBANG_KAWAN_KRITIS_PERSEN) {
                        return KeputusanAksi.jurus(kawan);
                    }
                    break;
                default:
                    break;
            }
        }
        return KeputusanAksi.serang(terlemah(sasaranSerang));
    }

    /** Karakter dengan nyawa terendah; jika seri, yang paling awal di daftar. */
    static KarakterMitos terlemah(List<KarakterMitos> daftar) {
        KarakterMitos hasil = daftar.get(0);
        for (KarakterMitos k : daftar) {
            if (k.getNyawa() < hasil.getNyawa()) {
                hasil = k;
            }
        }
        return hasil;
    }
}
