package id.girah.proto;

/** Pilihan baris perintah untuk prototipe. */
public final class PrototypeOptions {

    public String arena = "hutan";
    public String mode;
    public boolean demo;
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

    public static PrototypeOptions parse(String[] args) {
        PrototypeOptions o = new PrototypeOptions();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            String next = i + 1 < args.length ? args[i + 1] : null;
            switch (a) {
                case "--arena": o.arena = next; i++; break;
                case "--mode": o.mode = next; i++; break;
                case "--demo": o.demo = true; break;
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
                default: break;
            }
        }
        return o;
    }
}
