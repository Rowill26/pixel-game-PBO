package id.girah.battle;

import id.girah.karakter.KarakterMitos;

/**
 * Observer untuk {@link BattleManager}. Layar LibGDX memasang listener ini supaya
 * BattleManager tidak perlu tahu apa pun tentang Label, Table, atau Stage.
 *
 * <p>Semua metode punya implementasi kosong, jadi pemasang cukup meng-override yang perlu.
 */
public interface BattleListener {

    /**
     * Sebuah aksi baru saja terjadi. Berisi pelaku, jenis aksi, jarak, sasaran, serta perubahan
     * nyawa/energi dan siapa yang gugur, sehingga front end bisa memutar animasi yang tepat.
     *
     * <p>Urutan pemanggilan untuk satu aksi: {@code onAksi} → {@code onNarasi} → lalu
     * {@code onBattleSelesai}, atau {@code onRondeBaru} (jika ronde berganti) dan {@code onGiliranBaru}.
     * Aksi yang ditolak (mis. energi batin habis) tidak memicu event apa pun.
     */
    default void onAksi(HasilAksi hasil) {
    }

    /** Ada narasi baru (boleh berisi beberapa baris). */
    default void onNarasi(String narasi) {
    }

    /** Giliran berpindah ke {@code aktor}. Dipanggil juga saat pertarungan dimulai. */
    default void onGiliranBaru(KarakterMitos aktor, StatusBattle status) {
    }

    /** Ronde baru dimulai dan urutan giliran disusun ulang. */
    default void onRondeBaru(int ronde) {
    }

    /** Pertarungan berakhir; {@code hasil} adalah MENANG, KALAH, atau WAKTU_HABIS. */
    default void onBattleSelesai(StatusBattle hasil) {
    }
}
