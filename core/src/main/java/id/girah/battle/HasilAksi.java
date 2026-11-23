package id.girah.battle;

import id.girah.karakter.JarakAksi;
import id.girah.karakter.KarakterMitos;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Laporan lengkap satu aksi: siapa melakukan apa, ke siapa, dan apa akibatnya.
 * Dikirim ke front end lewat {@link BattleListener#onAksi(HasilAksi)} supaya animasi
 * musuh (yang dijalankan AI) bisa diputar tanpa menebak dari teks narasi. Immutable.
 */
public final class HasilAksi {

    private final KarakterMitos aktor;
    private final JenisAksi jenis;
    private final String namaAksi;
    private final JarakAksi jarak;
    private final List<KarakterMitos> sasaran;
    private final List<PerubahanStatus> perubahan;
    private final String narasi;

    HasilAksi(KarakterMitos aktor, JenisAksi jenis, String namaAksi, JarakAksi jarak,
              List<KarakterMitos> sasaran, List<PerubahanStatus> perubahan, String narasi) {
        this.aktor = Objects.requireNonNull(aktor, "Aktor tidak boleh null");
        this.jenis = Objects.requireNonNull(jenis, "Jenis aksi tidak boleh null");
        this.namaAksi = Objects.requireNonNull(namaAksi, "Nama aksi tidak boleh null");
        this.jarak = Objects.requireNonNull(jarak, "Jarak aksi tidak boleh null");
        this.sasaran = List.copyOf(sasaran);
        this.perubahan = List.copyOf(perubahan);
        this.narasi = Objects.requireNonNull(narasi, "Narasi tidak boleh null");
    }

    public KarakterMitos getAktor() {
        return aktor;
    }

    public JenisAksi getJenis() {
        return jenis;
    }

    /** "Serangan", nama jurus (mis. "Sebar Wabah"), atau nama pusaka (mis. "Tirta Suci"). */
    public String getNamaAksi() {
        return namaAksi;
    }

    public JarakAksi getJarak() {
        return jarak;
    }

    /** Sasaran yang dipilih. Untuk jurus area: semua lawan yang hidup saat jurus dipakai. */
    public List<KarakterMitos> getSasaran() {
        return sasaran;
    }

    /** Semua karakter yang nyawa atau energi batinnya berubah, termasuk energi yang dipakai aktor. */
    public List<PerubahanStatus> getPerubahan() {
        return perubahan;
    }

    /** Perubahan untuk satu karakter, atau kosong jika karakter itu tidak terdampak. */
    public Optional<PerubahanStatus> getPerubahan(KarakterMitos karakter) {
        for (PerubahanStatus p : perubahan) {
            if (p.getKarakter() == karakter) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    /** Karakter yang gugur karena aksi ini. */
    public List<KarakterMitos> getYangGugur() {
        List<KarakterMitos> gugur = new ArrayList<>();
        for (PerubahanStatus p : perubahan) {
            if (p.isGugur()) {
                gugur.add(p.getKarakter());
            }
        }
        return gugur;
    }

    /** Narasi yang sama dengan yang dikirim ke {@link BattleListener#onNarasi(String)}. */
    public String getNarasi() {
        return narasi;
    }

    @Override
    public String toString() {
        return aktor.getNama() + " " + jenis + " (" + namaAksi + ", " + jarak + ") -> " + perubahan;
    }
}
