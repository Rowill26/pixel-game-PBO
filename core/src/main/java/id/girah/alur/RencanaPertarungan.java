package id.girah.alur;

import id.girah.battle.BattleManager;
import id.girah.karakter.MakhlukGirah;
import id.girah.pusaka.TasPusaka;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Satu pertarungan dalam alur cerita, dibaca dari {@code assets/pertarungan/<id>.json} oleh
 * {@link PembacaPertarungan}: di arena mana, melawan siapa, batas ronde, dan naskah yang
 * diputar setelah menang. Immutable; setiap {@link #buatBattle} membuat karakter baru.
 */
public final class RencanaPertarungan {

    /** Satu musuh dalam rencana: jenis (kunci sprite) dan nama tampilan. */
    public static final class Musuh {
        private final String jenis;
        private final String nama;

        public Musuh(String jenis, String nama) {
            this.jenis = Objects.requireNonNull(jenis, "Jenis musuh tidak boleh null");
            this.nama = nama;
        }

        public String getJenis() {
            return jenis;
        }

        /** Nama tampilan, atau {@code null} untuk nama bawaan jenis itu. */
        public String getNama() {
            return nama;
        }

        @Override
        public String toString() {
            return nama == null ? jenis : jenis + ":" + nama;
        }
    }

    private final String id;
    private final String arena;
    private final String mode;
    private final List<Musuh> musuh;
    private final int batasRonde;
    private final String lanjutanMenang;

    /**
     * @param mode           waktu arena (mis. "night"), boleh {@code null}
     * @param batasRonde     {@link BattleManager#TANPA_BATAS_RONDE} untuk pertarungan biasa
     * @param lanjutanMenang naskah yang diputar setelah menang, boleh {@code null}
     */
    public RencanaPertarungan(String id, String arena, String mode, List<Musuh> musuh,
                              int batasRonde, String lanjutanMenang) {
        this.id = Objects.requireNonNull(id, "Id tidak boleh null");
        this.arena = Objects.requireNonNull(arena, "Arena tidak boleh null");
        this.mode = mode;
        this.musuh = List.copyOf(musuh);
        if (this.musuh.isEmpty()) {
            throw new IllegalArgumentException("Pertarungan harus punya minimal satu musuh");
        }
        if (batasRonde < 0) {
            throw new IllegalArgumentException("Batas ronde tidak boleh negatif");
        }
        this.batasRonde = batasRonde;
        this.lanjutanMenang = lanjutanMenang;
    }

    /** Nama file tanpa {@code .json}, mis. "babak1_hutan". */
    public String getId() {
        return id;
    }

    /** Folder arena di {@code assets/arena/}. */
    public String getArena() {
        return arena;
    }

    /** Waktu arena, atau {@code null} untuk bawaan arena. */
    public String getMode() {
        return mode;
    }

    public List<Musuh> getMusuh() {
        return musuh;
    }

    public int getBatasRonde() {
        return batasRonde;
    }

    /** Naskah setelah menang, atau {@code null}. */
    public String getLanjutanMenang() {
        return lanjutanMenang;
    }

    /** Musuh baru (nyawa penuh) sesuai rencana, urut seperti di file. */
    public List<MakhlukGirah> buatMusuh() {
        List<MakhlukGirah> hasil = new ArrayList<>();
        for (Musuh m : musuh) {
            hasil.add(PabrikKarakter.buatMusuh(m.getJenis(), m.getNama()));
        }
        return hasil;
    }

    /**
     * Pertarungan baru: kelompok pengelana dan musuh dibuat ulang dengan nyawa penuh,
     * memakai {@code tas} yang sama dengan pertarungan sebelumnya. Dipanggil juga untuk
     * mengulang pertarungan setelah kalah. Belum di-{@code mulai()}.
     */
    public BattleManager buatBattle(TasPusaka tas) {
        return new BattleManager(PabrikKarakter.buatKelompok(), buatMusuh(), batasRonde, tas);
    }

    @Override
    public String toString() {
        return "RencanaPertarungan[" + id + " di " + arena + ", musuh " + musuh + "]";
    }
}
