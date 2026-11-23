package id.girah.ai;

import id.girah.karakter.KarakterMitos;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Tugas yang dijalankan di thread AI: menunggu sejenak (agar pemain sempat membaca
 * dialog), memilih aksi, lalu menyerahkan keputusannya.
 *
 * <p>Kelas ini <b>tidak</b> memegang BattleManager. Semua yang ia butuhkan sudah
 * disalin sebelum thread dimulai, dan hasilnya dikirim lewat {@code kirimKeputusan},
 * yang meneruskannya ke render thread. Dengan begitu tidak ada data yang dibaca dan
 * ditulis bersamaan oleh dua thread.
 */
final class PikiranMusuh implements Runnable {

    private final KarakterMitos aktor;
    private final List<KarakterMitos> sasaranSerang;
    private final List<KarakterMitos> sasaranJurus;
    private final StrategiMusuh strategi;
    private final long jedaMs;
    private final Consumer<KeputusanAksi> kirimKeputusan;

    PikiranMusuh(KarakterMitos aktor, List<KarakterMitos> sasaranSerang, List<KarakterMitos> sasaranJurus,
                 StrategiMusuh strategi, long jedaMs, Consumer<KeputusanAksi> kirimKeputusan) {
        this.aktor = Objects.requireNonNull(aktor);
        this.sasaranSerang = List.copyOf(sasaranSerang);
        this.sasaranJurus = List.copyOf(sasaranJurus);
        this.strategi = Objects.requireNonNull(strategi);
        this.jedaMs = jedaMs;
        this.kirimKeputusan = Objects.requireNonNull(kirimKeputusan);
    }

    @Override
    public void run() {
        try {
            Thread.sleep(jedaMs);
        } catch (InterruptedException e) {
            // Dibatalkan (layar ditutup atau pertarungan berakhir): berhenti tanpa mengirim apa pun.
            Thread.currentThread().interrupt();
            return;
        }

        KeputusanAksi keputusan;
        try {
            keputusan = strategi.tentukan(aktor, sasaranSerang, sasaranJurus);
        } catch (RuntimeException e) {
            // Strategi yang rusak tidak boleh membuat game macet di giliran musuh.
            keputusan = KeputusanAksi.serang(sasaranSerang.get(0));
        }
        if (!Thread.currentThread().isInterrupted()) {
            kirimKeputusan.accept(keputusan);
        }
    }
}
