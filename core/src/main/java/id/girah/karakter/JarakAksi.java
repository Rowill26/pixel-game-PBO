package id.girah.karakter;

/**
 * Cara sebuah aksi dilakukan di arena. Hanya data untuk front end; logika battle tidak memakainya.
 */
public enum JarakAksi {
    /** Pelaku maju ke depan sasaran lalu kembali, mis. tebasan keris atau cakar leak. */
    DEKAT,
    /** Pelaku tetap di tempatnya, mis. mantra, kutukan, atau ramuan. */
    JAUH
}
