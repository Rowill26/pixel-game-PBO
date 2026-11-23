package id.girah.battle;

import id.girah.karakter.KarakterMitos;

/**
 * Observer untuk {@link BattleManager}. Layar LibGDX memasang listener ini supaya
 * BattleManager tidak perlu tahu apa pun tentang Label, Table, atau Stage.
 *
 * <p>Semua metode punya implementasi kosong, jadi pemasang cukup meng-override yang perlu.
 */
public interface BattleListener {

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
