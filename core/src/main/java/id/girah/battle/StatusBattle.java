package id.girah.battle;

/** Keadaan pertarungan. UI membaca ini untuk menentukan tombol mana yang aktif. */
public enum StatusBattle {
    BELUM_MULAI,
    /** Giliran anggota kelompok pengelana: tombol aksi aktif. */
    GILIRAN_PEMAIN,
    /** Giliran makhluk Girah: tombol aksi nonaktif, AI yang bertindak. */
    GILIRAN_MUSUH,
    /** Semua lawan gugur. */
    MENANG,
    /** Semua pengelana gugur. */
    KALAH,
    /** Batas ronde habis sebelum lawan dikalahkan (mis. ritual Babak 5 tuntas). */
    WAKTU_HABIS;

    public boolean isSelesai() {
        return this == MENANG || this == KALAH || this == WAKTU_HABIS;
    }
}
