package id.girah.karakter;

/**
 * Siapa yang boleh dipilih sebagai sasaran jurus, dilihat dari pihak si pengguna jurus.
 * UI memakai nilai ini untuk menentukan tombol sasaran mana yang aktif.
 */
public enum JenisSasaran {
    /** Satu karakter dari pihak lawan. */
    SATU_LAWAN,
    /** Semua karakter lawan yang masih hidup (serangan area). */
    SEMUA_LAWAN,
    /** Satu karakter dari pihak sendiri, termasuk diri sendiri. */
    SATU_KAWAN
}
