package id.girah.karakter;

/** Pihak dalam pertarungan. Menentukan siapa kawan dan siapa lawan. */
public enum Faksi {
    PENGELANA("Kelompok Pengelana"),
    GIRAH("Kekuatan Girah");

    private final String namaTampilan;

    Faksi(String namaTampilan) {
        this.namaTampilan = namaTampilan;
    }

    public String getNamaTampilan() {
        return namaTampilan;
    }
}
