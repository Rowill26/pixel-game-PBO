package id.girah.proto;

/** Pilihan baris perintah untuk prototipe. */
public final class PrototypeOptions {

    public String arena = "hutan";
    /** Start on the title screen (off when the arguments ask for a specific arena, story or test). */
    public boolean title = true;
    /** Test: press "Mulai" on frame N; with storyEndAt the opening script is skipped to its end. */
    public int startAt = -1;
    public int storyEndAt = -1;
    public String mode;
    public boolean demo;
    /** Mode pamer lama: aksi pertunjukan tanpa back end (tombol E, K, X, G). */
    public boolean pamer;
    /** AI musuh berpikir lebih singkat (untuk uji). */
    public boolean fast;
    /** Keluar begitu hasil pertarungan tampil (untuk uji otomatis). */
    public boolean exitOnEnd;
    /** UJI SAJA: nyawa bos dikurangi sebelum pertarungan mulai, supaya pergantian wujud bisa dicek. */
    public boolean weakBoss;
    /** UJI SAJA: simpan screenshot tepat saat wujud ritual bos muncul. */
    public String shotTransform;
    /** UJI SAJA: tombol yang "ditekan" berurutan saat menu/hasil terbuka, mis. "DOWN,ENTER,ENTER". */
    public String keys;
    /** UJI SAJA: opsi pilihan cerita yang dipilih pada frame --story-end-at (0 = opsi pertama). */
    public int storyChoice = -1;
    public String shotPath;
    public int shotFrame = 120;
    public int attackAt = -1;
    public int jurusAt = -1;
    public int pendekarJurusAt = -1;
    public int enemyAttackAt = -1;
    public int gugurAt = -1;
    public int bossDownAt = -1;
    public int fxAt = -1;
    public int fxSkip = 0;
    public int chooseAt = -1;
    public String story;
    public int storyAt = 5;
    public int storyLine = 0;
    public int chooseItem = 0;

    public static PrototypeOptions parse(String[] args) {
        PrototypeOptions o = new PrototypeOptions();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            String next = i + 1 < args.length ? args[i + 1] : null;
            switch (a) {
                case "--arena": o.arena = next; o.title = false; i++; break;
                case "--title": o.title = true; break;
                case "--no-title": o.title = false; break;
                case "--start-at": o.startAt = Integer.parseInt(next); i++; break;
                case "--story-end-at": o.storyEndAt = Integer.parseInt(next); i++; break;
                case "--mode": o.mode = next; i++; break;
                case "--demo": o.demo = true; o.title = false; break;
                case "--pamer": o.pamer = true; break;
                case "--cepat": o.fast = true; break;
                case "--keluar-saat-selesai": o.exitOnEnd = true; break;
                case "--uji-bos-lemah": o.weakBoss = true; break;
                case "--shot-berubah": o.shotTransform = next; i++; break;
                case "--tekan": o.keys = next; i++; break;
                case "--pilih": o.storyChoice = Integer.parseInt(next); i++; break;
                case "--shot": o.shotPath = next; i++; break;
                case "--frames": o.shotFrame = Integer.parseInt(next); i++; break;
                case "--attack-at": o.attackAt = Integer.parseInt(next); i++; break;
                case "--jurus-at": o.jurusAt = Integer.parseInt(next); i++; break;
                case "--pendekar-jurus-at": o.pendekarJurusAt = Integer.parseInt(next); i++; break;
                case "--diserang-at": o.enemyAttackAt = Integer.parseInt(next); i++; break;
                case "--gugur-at": o.gugurAt = Integer.parseInt(next); i++; break;
                case "--boss-down-at": o.bossDownAt = Integer.parseInt(next); i++; break;
                case "--fx-at": o.fxAt = Integer.parseInt(next); i++; break;
                case "--fx-skip": o.fxSkip = Integer.parseInt(next); i++; break;
                case "--choose-at": o.chooseAt = Integer.parseInt(next); i++; break;
                case "--story": o.story = next; o.title = false; i++; break;
                case "--story-line": o.storyLine = Integer.parseInt(next); i++; break;
                case "--choose-item": o.chooseItem = Integer.parseInt(next); i++; break;
                default: break;
            }
        }
        return o;
    }
}
