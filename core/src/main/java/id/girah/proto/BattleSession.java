package id.girah.proto;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import id.girah.ai.KeputusanAksi;
import id.girah.ai.PengendaliMusuh;
import id.girah.ai.StrategiDasar;
import id.girah.battle.AntreanDialog;
import id.girah.battle.BattleListener;
import id.girah.battle.BattleManager;
import id.girah.battle.HasilAksi;
import id.girah.battle.JenisAksi;
import id.girah.battle.PerubahanStatus;
import id.girah.battle.StatusBattle;
import id.girah.io.BabadExporter;
import id.girah.karakter.Balian;
import id.girah.karakter.CalonArang;
import id.girah.karakter.EnergiBatinHabisException;
import id.girah.karakter.JarakAksi;
import id.girah.karakter.JenisSasaran;
import id.girah.karakter.KarakterMitos;
import id.girah.karakter.Leak;
import id.girah.karakter.MakhlukGirah;
import id.girah.karakter.PemimpinPengikut;
import id.girah.karakter.Pengelana;
import id.girah.karakter.Pengikut;
import id.girah.karakter.Senopati;
import id.girah.karakter.TelikSandi;
import id.girah.karakter.Wiku;
import id.girah.pusaka.KembangKantil;
import id.girah.pusaka.Pusaka;
import id.girah.pusaka.TasPusaka;
import id.girah.pusaka.TirtaSuci;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pertarungan sungguhan di satu arena: menyambungkan tokoh-tokoh di arena ({@link Actor25D}) ke
 * {@link BattleManager} back end, menjalankan menu aksi pemain, memutar setiap {@link HasilAksi}
 * sebagai animasi, menahan AI musuh sampai layar diam, menampilkan narasi dari
 * {@link AntreanDialog}, dan mengekspor Babad saat pertarungan berakhir.
 *
 * <p>Mengikuti {@code docs/panduan-integrasi-backend.md} bagian 3-8. Semua pemanggilan back end
 * terjadi di render thread; AI mengirim keputusannya lewat {@code Gdx.app.postRunnable}.
 *
 * <p>Tokoh sekutu ({@code "sekutu"}: Ratna, Mpu Bharadah) belum punya kelas back end, jadi tetap
 * menjadi hiasan di arena dan tidak ikut bertarung.
 */
final class BattleSession implements BattleListener {

    /** Lama minimum satu baris narasi tampil sebelum baris berikutnya (detik). */
    private static final float LINE_SECONDS = 1.05f;
    /** Jarak berhenti di depan sasaran untuk aksi jarak dekat (px dunia). */
    private static final float MELEE_GAP = 30f;

    private final ArenaPrototype game;
    private final ArenaData arena;
    final BattleManager battle;
    private final PengendaliMusuh ai;
    private final boolean auto;
    private final Map<KarakterMitos, Actor25D> actorOf = new IdentityHashMap<>();
    private final Map<Actor25D, KarakterMitos> charOf = new IdentityHashMap<>();
    private final Deque<HasilAksi> queue = new ArrayDeque<>();

    private Playing playing;
    /** Bos yang sedang berubah wujud (Calon Arang), dan sisa waktu sampai wujud barunya muncul. */
    private Actor25D transforming;
    private float transformLeft;

    // narasi
    private float lineTimer = LINE_SECONDS;
    private int linesThisAction;
    String narration = "";

    // menu pemain
    private int menuLevel;                 // 0 = Serang/Jurus/Pusaka, 1 = daftar pusaka
    private int menuIndex;
    private List<Pusaka> pusakaList = List.of();
    private Pusaka chosenPusaka;
    private int chosenAction = -1;         // 0 serang, 1 jurus, 2 pusaka (saat memilih sasaran)
    private List<Actor25D> targets;
    private int targetIndex;
    private float autoDelay;

    // urutan giliran dan ronde yang tampil di HUD
    private List<Actor25D> shownOrder = List.of();
    private String shownRound = "Ronde 1";

    // akhir pertarungan
    private StatusBattle result;
    String babadMessage;

    /** Satu aksi yang sedang dipertunjukkan. */
    private static final class Playing {
        HasilAksi h;
        Actor25D actor;
        Actor25D target;
        List<Actor25D> fallers = new ArrayList<>();
        String anim;
        boolean melee;
        boolean hit;
        boolean animStarted;
        boolean areaFxStarted;
        float animAt;
        float t;
        float hitAt;
        float endAt;
        float zoom;
    }

    private BattleSession(ArenaPrototype game, ArenaData arena, BattleManager battle, boolean auto, boolean fast) {
        this.game = game;
        this.arena = arena;
        this.battle = battle;
        this.auto = auto;
        this.ai = fast
                ? new PengendaliMusuh(battle, new StrategiDasar(), Gdx.app::postRunnable, 150, 300)
                : new PengendaliMusuh(battle, new StrategiDasar(), Gdx.app::postRunnable);
    }

    // ================================================================
    // Membuat pertarungan (panduan bagian 3)
    // ================================================================

    /**
     * Membuat pertarungan dari tokoh di arena, atau {@code null} jika arena itu bukan arena battle
     * (lokasi cerita tanpa musuh). Pengelana dan musuh dibuat baru (nyawa penuh) setiap kali.
     */
    static BattleSession create(ArenaPrototype game, ArenaData arena, TasPusaka tas, boolean auto, boolean fast) {
        return create(game, arena, tas, auto, fast, false);
    }

    /**
     * @param weakBoss UJI SAJA ({@code --uji-bos-lemah}): nyawa bos dikurangi lewat serangan sebelum
     *                 pertarungan mulai, supaya pergantian wujud bisa dicek tanpa 40 ronde. Melanggar
     *                 aturan panduan 10.6 dengan sengaja, jadi tidak pernah aktif di permainan biasa.
     */
    static BattleSession create(ArenaPrototype game, ArenaData arena, TasPusaka tas, boolean auto, boolean fast,
                                boolean weakBoss) {
        List<Pengelana> party = new ArrayList<>();
        List<MakhlukGirah> foes = new ArrayList<>();
        Map<KarakterMitos, Actor25D> made = new IdentityHashMap<>();
        for (Actor25D a : arena.actors) {
            KarakterMitos k = newCharacter(a);
            if (k == null) {
                continue;
            }
            if (!k.getIdSprite().equals(a.sprite)) {
                throw new IllegalStateException("Sprite '" + a.sprite + "' tidak cocok dengan getIdSprite() '"
                        + k.getIdSprite() + "' untuk " + a.name);
            }
            if (k instanceof Pengelana && a.isParty()) {
                party.add((Pengelana) k);
            } else if (k instanceof MakhlukGirah && a.isEnemy()) {
                foes.add((MakhlukGirah) k);
            } else {
                throw new IllegalStateException(a.name + " ada di sisi yang salah di arena.json");
            }
            made.put(k, a);
        }
        if (party.isEmpty() || foes.isEmpty()) {
            return null;
        }
        if (weakBoss) {
            for (MakhlukGirah f : foes) {
                while (f.isBos() && f.getNyawa() * 100 > f.getNyawaMaks() * 33) {
                    f.terimaSerangan(40);
                }
            }
        }
        BattleManager bm = new BattleManager(party, foes, BattleManager.TANPA_BATAS_RONDE, tas);
        return start(game, arena, bm, made, auto, fast);
    }

    /**
     * Membuat pertarungan dari rencana di {@code assets/pertarungan/<id>.json} (panduan bagian 12).
     * Musuh diambil dari rencana; {@code arena.json} hanya memberi posisi. Setiap karakter dipasangkan
     * ke tokoh arena pertama yang belum terpakai dengan sprite yang sama. Tokoh pengelana/musuh di
     * arena yang tidak dipakai rencana dikeluarkan dari layar; sekutu (Ratna, Bharadah) tetap tampil.
     */
    static BattleSession create(ArenaPrototype game, ArenaData arena, id.girah.alur.RencanaPertarungan plan,
                                TasPusaka tas, boolean auto, boolean fast, boolean weakBoss) {
        BattleManager bm = plan.buatBattle(tas);
        Map<KarakterMitos, Actor25D> made = new IdentityHashMap<>();
        List<Actor25D> free = new ArrayList<>();
        for (Actor25D a : arena.actors) {
            if (a.isParty() || a.isEnemy()) {
                free.add(a);
            }
        }
        List<KarakterMitos> all = new ArrayList<>(bm.getKelompok());
        all.addAll(bm.getLawan());
        for (KarakterMitos k : all) {
            Actor25D spot = null;
            for (Actor25D a : free) {
                if (a.sprite.equals(k.getIdSprite())) {
                    spot = a;
                    break;
                }
            }
            if (spot == null) {
                throw new IllegalStateException("Arena '" + arena.key + "' tidak punya posisi untuk sprite '"
                        + k.getIdSprite() + "' (" + k.getNama() + ") dari pertarungan '" + plan.getId() + "'");
            }
            free.remove(spot);
            spot.name = k.getNama();                 // nama tampilan dari rencana, mis. "Leak 1"
            made.put(k, spot);
        }
        arena.actors.removeAll(free);                // posisi yang tidak dipakai rencana tidak ditampilkan
        if (weakBoss) {
            for (KarakterMitos k : bm.getLawan()) {
                MakhlukGirah f = (MakhlukGirah) k;
                while (f.isBos() && f.getNyawa() * 100 > f.getNyawaMaks() * 33) {
                    f.terimaSerangan(40);
                }
            }
        }
        return start(game, arena, bm, made, auto, fast);
    }

    private static BattleSession start(ArenaPrototype game, ArenaData arena, BattleManager bm,
                                       Map<KarakterMitos, Actor25D> made, boolean auto, boolean fast) {
        BattleSession s = new BattleSession(game, arena, bm, auto, fast);
        for (Map.Entry<KarakterMitos, Actor25D> e : made.entrySet()) {
            s.link(e.getKey(), e.getValue());
            Actor25D a = e.getValue();
            KarakterMitos k = e.getKey();
            a.nyawa = k.getNyawa();
            a.nyawaMaks = k.getNyawaMaks();
            a.energi = k.getEnergiBatin();
            a.energiMaks = k.getEnergiBatinMaks();
            a.syncBars();
        }
        bm.tambahListener(s);
        s.ai.setTungguIzinFrontEnd(true);   // AI menunggu animasi dan narasi selesai (panduan 6)
        s.ai.aktifkan();                    // sebelum mulai()
        bm.mulai();
        s.refreshTurnDisplay();
        s.narration = "Pertarungan dimulai: " + arena.title + ".";
        Gdx.app.log("battle", "mulai: " + arena.title + " (" + bm.getKelompok().size() + " pengelana, "
                + bm.getLawan().size() + " lawan)");
        return s;
    }

    /** Kelas back end untuk satu tokoh arena, menurut nama sprite-nya; {@code null} untuk sekutu/hiasan. */
    private static KarakterMitos newCharacter(Actor25D a) {
        switch (a.sprite) {
            case "pendekar":
                return new Senopati(a.name);
            case "juru_mantra":
                return new Wiku(a.name);
            case "peramu":
                return new Balian(a.name);
            case "pengintai":
                return new TelikSandi(a.name);
            case "leak":
                return new Leak(a.name);
            case "pengikut":
                return new Pengikut(a.name);
            case "pemimpin_pengikut":
                return new PemimpinPengikut();
            case "calon_arang":
                return new CalonArang();
            default:
                return null;        // ratna, bharadah: belum ada kelas back end (catatan D2)
        }
    }

    /** Tas Pusaka awal permainan baru (panduan bagian 3). */
    static TasPusaka newBag() {
        TasPusaka tas = new TasPusaka();
        tas.tambah(new TirtaSuci(), 3);
        tas.tambah(new KembangKantil(), 2);
        return tas;
    }

    private void link(KarakterMitos k, Actor25D a) {
        actorOf.put(k, a);
        charOf.put(a, k);
    }

    /** Wajib dipanggil saat layar battle ditutup (panduan 10.5). */
    void stop() {
        ai.hentikan();
    }

    // ================================================================
    // Event dari back end (panduan bagian 5)
    // ================================================================

    @Override
    public void onAksi(HasilAksi hasil) {
        queue.add(hasil);
    }

    @Override
    public void onBattleSelesai(StatusBattle hasil) {
        result = hasil;
        try {
            Path file = new BabadExporter(Gdx.files.local("babad").file().toPath()).ekspor(battle);
            babadMessage = "Babad tersimpan: " + file.getFileName();
        } catch (IOException e) {
            babadMessage = "Babad gagal disimpan: " + e.getMessage();
        }
        Gdx.app.log("battle", "selesai: " + hasil + " (ronde " + battle.getRonde() + "). " + babadMessage);
    }

    // ================================================================
    // Keadaan untuk HUD
    // ================================================================

    /** {@code true} selama ada animasi, antrean animasi, atau narasi yang belum habis dibaca. */
    /** {@code true} saat pemain bisa menekan tombol (menu, pilih sasaran, atau hasil akhir). */
    boolean acceptsKeys() {
        return menuOpen() || resultTitle() != null;
    }

    boolean busy() {
        return playing != null || !queue.isEmpty() || transforming != null
                || battle.getAntreanDialog().adaBaris() || lineTimer < 0.35f;
    }

    boolean menuOpen() {
        return battle.getStatus() == StatusBattle.GILIRAN_PEMAIN && !busy() && !auto;
    }

    /**
     * Pengelana yang sedang bergiliran (atau yang aksinya sedang diputar), atau {@code null} saat
     * giliran musuh / selesai. Back end sudah memajukan giliran saat onAksi tiba, jadi selama
     * animasi yang disorot tetap pelaku aksi itu.
     */
    Actor25D activeActor() {
        if (playing != null) {
            return playing.actor.isParty() ? playing.actor : null;
        }
        if (busy() || battle.getStatus() != StatusBattle.GILIRAN_PEMAIN) {
            return null;
        }
        return actorOf.get(battle.getAktor());
    }

    /** Musuh yang sedang berpikir (untuk tanda "..."), atau {@code null}. */
    Actor25D thinkingActor() {
        if (battle.getStatus() != StatusBattle.GILIRAN_MUSUH || playing != null) {
            return null;
        }
        return actorOf.get(battle.getAktor());
    }

    String roundLabel() {
        return shownRound;
    }

    /** Urutan giliran yang tersisa di ronde ini (ikon di atas layar), diperbarui saat layar diam. */
    List<Actor25D> turnOrder() {
        return shownOrder;
    }

    /** Menyalin urutan giliran dan ronde dari back end; dipanggil hanya saat tidak ada animasi. */
    private void refreshTurnDisplay() {
        List<Actor25D> out = new ArrayList<>();
        for (KarakterMitos k : battle.getAntrianGiliran()) {
            Actor25D a = actorOf.get(k);
            if (a != null) {
                out.add(a);
            }
        }
        shownOrder = out;
        int batas = battle.getBatasRonde();
        shownRound = "Ronde " + Math.max(1, battle.getRonde())
                + (batas != BattleManager.TANPA_BATAS_RONDE ? " dari " + batas : "");
    }

    String[] menuItems() {
        if (menuLevel == 1) {
            String[] items = new String[pusakaList.size()];
            TasPusaka tas = battle.getTasPusaka();
            for (int i = 0; i < items.length; i++) {
                items[i] = pusakaList.get(i).getNama() + " x" + tas.jumlah(pusakaList.get(i));
            }
            return items;
        }
        return new String[] {"Serang", "Jurus", "Pusaka"};
    }

    int menuIndex() {
        return menuIndex;
    }

    boolean choosingTarget() {
        return targets != null;
    }

    Actor25D menuTarget() {
        return menuOpen() && targets != null && !targets.isEmpty() ? targets.get(targetIndex % targets.size()) : null;
    }

    /** Tombol aktif sesuai aturan panduan bagian 4 dan 10.2. */
    boolean menuEnabled(int i) {
        if (battle.getStatus() != StatusBattle.GILIRAN_PEMAIN) {
            return false;
        }
        KarakterMitos aktor = battle.getAktor();
        if (menuLevel == 1) {
            return i < pusakaList.size() && !battle.getSasaranPusaka(pusakaList.get(i)).isEmpty();
        }
        switch (i) {
            case 0:
                return !battle.getSasaranSerang().isEmpty();
            case 1:
                return aktor.bisaMemakaiJurus() && !battle.getSasaranJurus().isEmpty();
            case 2:
                if (battle.getTasPusaka().isKosong()) {
                    return false;
                }
                for (Pusaka p : battle.getTasPusaka().getDaftarPusaka()) {
                    if (!battle.getSasaranPusaka(p).isEmpty()) {
                        return true;
                    }
                }
                return false;
            default:
                return false;
        }
    }

    /** Judul hasil akhir untuk HUD, atau {@code null} selama belum selesai atau masih ada animasi/narasi. */
    String resultTitle() {
        if (result == null || busy()) {
            return null;
        }
        switch (result) {
            case MENANG:
                return "MENANG";
            case KALAH:
                return "KALAH";
            default:
                return "WAKTU HABIS";
        }
    }

    String resultHint() {
        return result == StatusBattle.KALAH ? "Enter: ulangi pertarungan" : "Enter: kembali ke layar judul";
    }

    // ================================================================
    // Menu pemain (panduan bagian 4)
    // ================================================================

    /** Dibaca setiap frame. Mengembalikan aksi akhir pertarungan yang diminta pemain, atau {@code null}. */
    StatusBattle input() {
        boolean ok = game.justPressed(Input.Keys.ENTER) || game.justPressed(Input.Keys.Z);
        if (resultTitle() != null) {
            return ok ? result : null;
        }
        if (ok && battle.getAntreanDialog().adaBaris() && playing == null) {
            lineTimer = LINE_SECONDS;                    // Enter mempercepat narasi
        }
        if (!menuOpen()) {
            targets = null;
            return null;
        }
        boolean up = game.justPressed(Input.Keys.UP);
        boolean down = game.justPressed(Input.Keys.DOWN);
        boolean left = game.justPressed(Input.Keys.LEFT);
        boolean right = game.justPressed(Input.Keys.RIGHT);
        boolean back = game.justPressed(Input.Keys.BACKSPACE) || game.justPressed(Input.Keys.X);
        if (targets != null) {
            if (up || left) {
                targetIndex = (targetIndex + targets.size() - 1) % targets.size();
            }
            if (down || right) {
                targetIndex = (targetIndex + 1) % targets.size();
            }
            if (back) {
                targets = null;
                narration = menuLevel == 1 ? "Pilih pusaka." : "Pilih aksi.";
            } else if (ok) {
                Actor25D t = targets.get(targetIndex % targets.size());
                targets = null;
                perform(chosenAction, charOf.get(t));
            }
            return null;
        }
        int n = menuItems().length;
        if (n > 0 && up) {
            menuIndex = (menuIndex + n - 1) % n;
        }
        if (n > 0 && down) {
            menuIndex = (menuIndex + 1) % n;
        }
        if (back && menuLevel == 1) {
            menuLevel = 0;
            menuIndex = 2;
            narration = "Pilih aksi.";
        } else if (ok) {
            choose(menuIndex);
        }
        return null;
    }

    /** Test hook: pilih aksi {@code item} dengan sasaran pertama, tanpa menu. */
    void quickAction(int item) {
        if (!menuOpen() && !(auto && battle.getStatus() == StatusBattle.GILIRAN_PEMAIN && !busy())) {
            return;
        }
        if (!menuEnabled(item)) {
            return;
        }
        KarakterMitos aktor = battle.getAktor();
        List<KarakterMitos> list = item == 0 ? battle.getSasaranSerang() : battle.getSasaranJurus();
        boolean all = item == 1 && aktor.getSasaranJurus() == JenisSasaran.SEMUA_LAWAN;
        perform(item, all ? null : list.get(0));
    }

    private void choose(int i) {
        if (!menuEnabled(i)) {
            narration = disabledReason(i);
            Gdx.app.log("battle", "nonaktif: " + narration);
            return;
        }
        KarakterMitos aktor = battle.getAktor();
        if (menuLevel == 1) {
            chosenPusaka = pusakaList.get(i);
            chosenAction = 2;
            openTargets(battle.getSasaranPusaka(chosenPusaka), true,
                    chosenPusaka.getNama() + ": " + chosenPusaka.getDeskripsi());
            return;
        }
        switch (i) {
            case 0:
                chosenAction = 0;
                openTargets(battle.getSasaranSerang(), false, "Pilih sasaran.");
                break;
            case 1:
                chosenAction = 1;
                if (aktor.getSasaranJurus() == JenisSasaran.SEMUA_LAWAN) {
                    perform(1, null);
                } else {
                    openTargets(battle.getSasaranJurus(), aktor.getSasaranJurus() == JenisSasaran.SATU_KAWAN,
                            aktor.getNamaJurus() + " (" + aktor.getBiayaJurus() + " energi batin): pilih sasaran.");
                }
                break;
            default:
                pusakaList = battle.getTasPusaka().getDaftarPusaka();
                menuLevel = 1;
                menuIndex = 0;
                narration = "Pilih pusaka. Backspace: kembali.";
                break;
        }
    }

    private String disabledReason(int i) {
        KarakterMitos aktor = battle.getAktor();
        if (menuLevel == 1) {
            return "Tidak ada kawan yang terbantu oleh " + pusakaList.get(i).getNama() + ".";
        }
        if (i == 1) {
            if (!aktor.bisaMemakaiJurus()) {
                return aktor.getNama() + " belum bisa memakai " + aktor.getNamaJurus()
                        + " (butuh " + aktor.getBiayaJurus() + " energi batin).";
            }
            return "Tidak ada sasaran untuk " + aktor.getNamaJurus() + ".";
        }
        if (i == 2) {
            return battle.getTasPusaka().isKosong() ? "Tas Pusaka kosong." : "Belum ada kawan yang butuh pusaka.";
        }
        return "Tidak ada sasaran.";
    }

    private void openTargets(List<KarakterMitos> list, boolean allies, String message) {
        List<Actor25D> out = new ArrayList<>();
        for (KarakterMitos k : list) {
            Actor25D a = actorOf.get(k);
            if (a != null) {
                out.add(a);
            }
        }
        if (out.isEmpty()) {
            return;
        }
        out.sort((p, q) -> Float.compare(q.y, p.y));     // atas layar dulu, supaya panah atas/bawah terasa wajar
        targets = out;
        Actor25D self = actorOf.get(battle.getAktor());
        targetIndex = allies && out.contains(self) ? out.indexOf(self) : 0;
        narration = message;
    }

    /** Mengirim pilihan ke back end. Event onAksi menyusul dan diputar di {@link #update}. */
    private void perform(int item, KarakterMitos target) {
        Gdx.app.log("battle", "pemain: " + battle.getAktor().getNama() + " "
                + (item == 0 ? "serang" : item == 1 ? "jurus" : "pusaka " + chosenPusaka.getNama())
                + " -> " + (target == null ? "semua" : target.getNama()));
        try {
            if (item == 0) {
                battle.serang(target);
            } else if (item == 1) {
                battle.gunakanJurus(target);
            } else {
                battle.gunakanPusaka(chosenPusaka, target);
            }
            menuLevel = 0;
            menuIndex = 0;
        } catch (EnergiBatinHabisException e) {
            narration = e.getMessage();              // giliran TIDAK habis: menu tetap terbuka
            Gdx.app.log("battle", "energi habis: " + narration);
        }
    }

    /** Pemain otomatis (--demo): memilih aksi dengan strategi yang sama seperti AI musuh. */
    private void autoPlay(float dt) {
        if (!auto || battle.getStatus() != StatusBattle.GILIRAN_PEMAIN || busy()) {
            autoDelay = 0f;
            return;
        }
        autoDelay += dt;
        if (autoDelay < 0.5f) {
            return;
        }
        autoDelay = 0f;
        KarakterMitos aktor = battle.getAktor();
        KeputusanAksi k = new StrategiDasar().tentukan(aktor, battle.getSasaranSerang(), battle.getSasaranJurus());
        try {
            if (k.getJenis() == KeputusanAksi.Jenis.JURUS) {
                battle.gunakanJurus(k.getTarget());
            } else {
                battle.serang(k.getTarget());
            }
        } catch (EnergiBatinHabisException e) {
            battle.serang(battle.getSasaranSerang().get(0));
        }
    }

    // ================================================================
    // Setiap frame
    // ================================================================

    void update(float dt) {
        autoPlay(dt);
        if (playing == null && transforming == null && !queue.isEmpty()) {
            start(queue.poll());
        }
        if (playing != null) {
            advance(playing, dt);
        }
        updateTransform(dt);
        updateNarration(dt);
        if (playing == null && queue.isEmpty() && transforming == null) {
            refreshTurnDisplay();
        }
        // panduan bagian 6: izinkan AI bertindak hanya saat layar diam
        if (battle.getStatus() == StatusBattle.GILIRAN_MUSUH && !busy()) {
            ai.izinkanBertindak();
        }
    }

    private void updateNarration(float dt) {
        lineTimer += dt;
        AntreanDialog dialog = battle.getAntreanDialog();
        if (!dialog.adaBaris() || lineTimer < LINE_SECONDS) {
            return;
        }
        // satu baris saat aksi mulai, sisanya (kerusakan, ucapan) setelah benturan
        if (playing != null && !playing.hit && linesThisAction >= 1) {
            return;
        }
        narration = dialog.ambilBerikutnya();
        lineTimer = 0f;
        linesThisAction++;
    }

    // ================================================================
    // HasilAksi -> animasi (panduan bagian 5)
    // ================================================================

    private void start(HasilAksi h) {
        Playing p = new Playing();
        p.h = h;
        p.actor = actorOf.get(h.getAktor());
        List<KarakterMitos> sasaran = h.getSasaran();
        p.target = sasaran.isEmpty() ? null : actorOf.get(sasaran.get(0));
        p.anim = h.getJenis() == JenisAksi.SERANG ? "serang" : h.getJenis() == JenisAksi.JURUS ? "jurus" : null;
        p.melee = h.getJarak() == JarakAksi.DEKAT && p.target != null && p.target.isEnemy() != p.actor.isEnemy();
        boolean area = h.getJenis() == JenisAksi.JURUS && h.getAktor().getSasaranJurus() == JenisSasaran.SEMUA_LAWAN;
        p.zoom = area ? 1.06f : h.getJenis() == JenisAksi.JURUS ? 1.15f : 1.12f;
        float animAt = p.melee ? 0.12f : 0.05f;
        p.animAt = animAt;
        p.hitAt = animAt + (h.getJenis() == JenisAksi.SERANG ? 0.3f : 0.55f);
        p.endAt = p.hitAt + 0.6f;
        if (p.anim != null && p.actor.hasAnimation(p.anim)) {
            float len = p.actor.animationLength(p.anim);
            float impact = p.actor.impactTime(p.anim);
            if (impact >= 0f) {
                p.hitAt = animAt + impact;
            } else if (h.getJenis() == JenisAksi.SERANG) {
                p.hitAt = animAt + Math.min(0.3f, len * 0.6f);
            }
            p.endAt = Math.max(p.hitAt + 0.5f, animAt + len + 0.15f);
        }
        if (p.melee) {
            p.endAt = Math.max(p.endAt, 1.0f);
        }
        // energi batin yang dipakai pelaku langsung terlihat berkurang
        h.getPerubahan(h.getAktor()).ifPresent(c -> {
            if (c.getPerubahanEnergiBatin() < 0) {
                p.actor.energi += c.getPerubahanEnergiBatin();
                p.actor.syncBars();
            }
        });
        if (h.getJenis() == JenisAksi.PUSAKA && p.target != null) {
            game.effects.play(h.getNamaAksi().equals("Tirta Suci") ? "ramuan_penawar" : "doa_ratna",
                    p.target.footX(), p.target.y, 0.15f);
        }
        playing = p;
        linesThisAction = 0;
        lineTimer = LINE_SECONDS;                 // baris pertama aksi ini langsung tampil
    }

    private void advance(Playing p, float dt) {
        p.t += dt;
        if (p.melee) {
            float dir = p.actor.x > p.target.x ? -1f : 1f;      // pengelana maju ke kiri, musuh ke kanan
            float approach = Math.max(0f, Math.abs(p.actor.x - p.target.x) - MELEE_GAP);
            float depth = p.target.y - p.actor.y - 1f;
            float s = p.endAt;
            float k;
            if (p.t < 0.25f * s) {
                k = MathUtils.sin(p.t / (0.25f * s) * MathUtils.HALF_PI);
            } else if (p.t < 0.55f * s) {
                k = 1f;
            } else if (p.t < 0.85f * s) {
                k = 1f - MathUtils.sin((p.t - 0.55f * s) / (0.3f * s) * MathUtils.HALF_PI);
            } else {
                k = 0f;
            }
            p.actor.offsetX = dir * approach * k;
            p.actor.offsetY = depth * k;
        }
        focusCamera(p);
        if (!p.animStarted && p.t >= p.animAt) {
            p.animStarted = true;
            if (p.anim != null) {
                p.actor.play(p.anim);
            }
        }
        if (!p.areaFxStarted && p.t >= 0.1f) {
            p.areaFxStarted = true;
            startAreaEffect(p);
        }
        if (!p.hit && p.t >= p.hitAt) {
            p.hit = true;
            impact(p);
        }
        if (p.t >= p.endAt) {
            p.actor.offsetX = 0f;
            p.actor.offsetY = 0f;
            for (Actor25D f : p.fallers) {
                f.play("gugur");
            }
            checkTransform(p);
            playing = null;
        }
    }

    private void focusCamera(Playing p) {
        if (p.target != null && p.target != p.actor) {
            game.rig.focus((p.actor.footX() + p.target.footX()) / 2f, (p.actor.y + p.target.y) / 2f + 20f, p.zoom);
        } else {
            game.rig.focus(p.actor.footX(), p.actor.y + 20f, p.zoom);
        }
    }

    /** Efek jurus yang menutupi satu sisi arena (dimulai lebih awal supaya penuh saat benturan). */
    private void startAreaEffect(Playing p) {
        if (p.h.getJenis() != JenisAksi.JURUS) {
            return;
        }
        String sprite = p.h.getAktor().getIdSprite();
        float[] c = centroid(p.h.getSasaran());
        switch (sprite) {
            case "juru_mantra":
                game.effects.play("lingkar_suci", c[0], c[1], 0f);
                break;
            case "pengikut":
            case "calon_arang":
                game.effects.play("asap_wabah", c[0], c[1] - 6f, 0.1f);
                break;
            case "leak":
            case "pemimpin_pengikut":
                if (p.target != null) {
                    game.effects.play("jerat_kabut", p.target.footX(), p.target.y, 0.1f);
                }
                break;
            case "peramu":
                if (p.target != null) {
                    game.effects.play("ramuan_penawar", p.target.footX(), p.target.y, 0.1f);
                }
                break;
            default:
                break;
        }
    }

    private float[] centroid(List<KarakterMitos> list) {
        float x = 0f;
        float y = 0f;
        int n = 0;
        for (KarakterMitos k : list) {
            Actor25D a = actorOf.get(k);
            if (a != null) {
                x += a.footX();
                y += a.y;
                n++;
            }
        }
        return n == 0 ? new float[] {320f, 120f} : new float[] {x / n, y / n};
    }

    /** Efek benturan di tubuh sasaran untuk serangan dan jurus satu sasaran. */
    private String hitEffect(Playing p) {
        String sprite = p.actor.sprite;
        if (p.h.getJenis() == JenisAksi.JURUS) {
            switch (p.h.getAktor().getIdSprite()) {
                case "pendekar":
                    return "tebasan";
                case "pengintai":
                    return "langkah_bayangan";
                default:
                    return null;                           // jurus lain sudah punya efek area/kaki
            }
        }
        if (p.h.getJenis() != JenisAksi.SERANG) {
            return null;
        }
        if (sprite.equals("leak") || sprite.equals("calon_arang_ritual")) {
            return "cakar";
        }
        if (sprite.equals("calon_arang")) {
            return "kutukan";                              // serangan jarak jauh: asap dupa di kaki sasaran
        }
        return "sabetan";
    }

    private void impact(Playing p) {
        String fx = hitEffect(p);
        boolean big = p.h.getJenis() == JenisAksi.JURUS;
        float dir = p.target != null && p.actor.x > p.target.x ? -1f : 1f;
        for (PerubahanStatus c : p.h.getPerubahan()) {
            Actor25D a = actorOf.get(c.getKarakter());
            if (a == null) {
                continue;
            }
            boolean isActor = c.getKarakter() == p.h.getAktor();
            if (c.getPerubahanEnergiBatin() > 0 || (!isActor && c.getPerubahanEnergiBatin() < 0)) {
                a.energi += c.getPerubahanEnergiBatin();
            }
            if (c.getPerubahanNyawa() != 0) {
                a.nyawa += c.getPerubahanNyawa();
            }
            a.syncBars();
            float hx = a.footX();
            float hy = a.y + a.height() * 0.45f;
            if (c.getKerusakan() > 0) {
                a.flash = 1f;
                a.knock = 7f * dir;
                a.play("terluka");
                if (fx != null) {
                    if (fx.equals("kutukan")) {
                        game.effects.play(fx, a.footX(), a.y, 0f);
                    } else {
                        game.effects.play(fx, a.footX(), a.y + 24f, 0f);
                    }
                }
                game.particles.burst(hx, hy, big ? 32 : 24, 1f, 0.85f, 0.55f, 90f);
                game.tempLights.add(new PointLight(hx, hy, 24f, 1f, 0.85f, 0.6f, 70f, 2.2f, 0f).temporary(0.35f, 60f));
                game.popup(hx, hy + 30f, "-" + c.getKerusakan());
            }
            if (c.getPemulihanNyawa() > 0) {
                game.particles.burst(hx, hy, 16, 0.6f, 1f, 0.6f, 50f);
                game.popup(hx, hy + 30f, "+" + c.getPemulihanNyawa());
            }
            if (c.getPerubahanEnergiBatin() > 0) {
                game.popup(hx, hy + 42f, "+" + c.getPerubahanEnergiBatin() + " energi");
            }
            if (c.isGugur()) {
                p.fallers.add(a);
            }
        }
        if (!p.h.getYangGugur().isEmpty() || big) {
            game.hitStop(big ? 0.12f : 0.07f);
        } else if (p.h.getJenis() == JenisAksi.SERANG) {
            game.hitStop(0.07f);
        }
        if (p.h.getJenis() != JenisAksi.PUSAKA) {
            game.rig.shake(big ? 6f : 5f);
        }
    }

    // ================================================================
    // Calon Arang berubah wujud (hanya tampilan; catatan D4)
    // ================================================================

    /**
     * Back end belum punya wujud ritual (catatan D4). Sementara, tampilan Calon Arang berganti ke
     * wujud ritual saat nyawanya pertama kali jatuh ke ambang rendah; karakter back end-nya sama.
     */
    private void checkTransform(Playing p) {
        for (PerubahanStatus c : p.h.getPerubahan()) {
            KarakterMitos k = c.getKarakter();
            Actor25D a = actorOf.get(k);
            if (a == null || a.nextForm == null || !k.isHidup() || !(k instanceof MakhlukGirah)
                    || !((MakhlukGirah) k).isNyawaRendah()) {
                continue;
            }
            transforming = a;
            transformLeft = a.play("gugur") + 0.6f;   // klip "gugur" wujud manusia = awal berubah wujud
            Gdx.app.log("battle", k.getNama() + " mulai berubah wujud (frame " + Gdx.graphics.getFrameId() + ")");
            return;
        }
    }

    private void updateTransform(float dt) {
        if (transforming == null) {
            return;
        }
        transformLeft -= dt;
        if (transformLeft > 0f) {
            return;
        }
        Actor25D old = transforming;
        Actor25D next = old.nextForm;
        transforming = null;
        int i = arena.actors.indexOf(old);
        if (i < 0) {
            return;
        }
        KarakterMitos k = charOf.remove(old);
        next.nyawa = old.nyawa;
        next.nyawaMaks = old.nyawaMaks;
        next.energi = old.energi;
        next.energiMaks = old.energiMaks;
        next.syncBars();
        old.nextForm = null;
        arena.actors.set(i, next);
        link(k, next);
        next.flash = 1f;
        float cx = next.footX();
        float cy = next.y + next.height() * 0.45f;
        game.rig.shake(9f);
        game.particles.burst(cx, cy, 60, 0.95f, 0.35f, 0.3f, 140f);
        game.tempLights.add(new PointLight(cx, cy, 30f, 1f, 0.3f, 0.25f, 120f, 3f, 0f).temporary(1.2f, 140f));
        narration = k.getNama() + " berubah wujud! Surai putihnya menjuntai sampai tanah.";
        lineTimer = 0f;
        game.onBossTransformed();
    }
}
