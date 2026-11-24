package id.girah.proto;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;

/**
 * Prototipe arena battle 2.5D: lapisan parallax, pencahayaan dengan normal map, bayangan
 * yang jatuh menjauhi cahaya, partikel, kamera bergerak, dan efek kamera (blur kedalaman,
 * bloom, color grading, vignette).
 *
 * <p>Tujuannya hanya menilai "rasa" visual. Logika battle sungguhan ada di backend
 * ({@code id.girah.battle.BattleManager}); di sini serangan hanya pertunjukan.
 */
public class ArenaPrototype extends ApplicationAdapter {

    public static final int SCREEN_W = 1280;
    public static final int SCREEN_H = 720;
    private static final float SCALE = SCREEN_W / ArenaData.VIEW_W;
    private static final int MAX_LIGHTS = 16;
    private static final String[] ARENAS = {"hutan", "pemakaman"};

    private final PrototypeOptions options;
    private SpriteBatch batch;
    private ShaderProgram lit;
    private PostProcessor post;
    private final PostProcessor.Settings fx = new PostProcessor.Settings();
    private Hud hud;
    final CameraRig rig = new CameraRig();
    private final OrthographicCamera cam = new OrthographicCamera();
    final ParticleSystem particles = new ParticleSystem();
    /** Efek serangan dan jurus (assets/fx). */
    FxPlayer effects;
    /** Layar cerita dan dialog (tombol C memutar naskah contoh). */
    private StoryLayer story;
    /** Layar judul dan menu utama. */
    private TitleLayer title;
    private final Map<String, Texture[][]> spriteCache = new HashMap<>();
    private final Map<Texture, TextureRegion> regions = new HashMap<>();
    final List<PointLight> tempLights = new ArrayList<>();
    private final List<PointLight> frameLights = new ArrayList<>();
    private final float[] lightPos = new float[MAX_LIGHTS * 3];
    private final float[] lightColor = new float[MAX_LIGHTS * 3];
    private final float[] lightRadius = new float[MAX_LIGHTS];
    private final Affine2 affine = new Affine2();

    private Texture flatNormal;
    private Texture pixel;
    private Texture glow;
    private Texture shaft;
    private ArenaData arena;
    private ArenaData.Mode mode;
    private int arenaIndex;

    boolean useNormals = true;
    boolean useShadows = true;
    private float time;
    private int frame;
    private int turn;
    private float demoTimer;
    private Action action;
    /**
     * Pertarungan sungguhan (back end) di arena yang sedang dimuat, atau {@code null} untuk lokasi
     * cerita dan mode pamer ({@code --pamer}).
     */
    BattleSession session;
    /** Tas Pusaka kelompok: dibuat sekali per permainan dan dipakai ulang di setiap pertarungan. */
    private id.girah.pusaka.TasPusaka bag;
    /** Status cerita (nilai kepercayaan, penanda pilihan): dibuat sekali per permainan (panduan 11). */
    private id.girah.cerita.StatusCerita storyStatus = new id.girah.cerita.StatusCerita();

    id.girah.cerita.StatusCerita storyStatus() {
        return storyStatus;
    }
    final List<float[]> popups = new ArrayList<>();
    final List<String> popupText = new ArrayList<>();
    String narration = "Pilih aksi dengan panah dan Enter. F12 untuk daftar tombol.";

    /** Satu aksi yang sedang dipertunjukkan. */
    private static final class Action {
        Actor25D actor;
        Actor25D target;
        boolean jurus;
        boolean hit;
        float t;
        /** Animasi tokoh yang diputar (serang/jurus), detik mulai, saat kena, dan saat selesai. */
        String anim = "serang";
        boolean animStarted;
        boolean fxStarted;
        float animAt = 0.12f;
        float hitAt = 0.3f;
        float endAt = 1.0f;
        int dmgMin = 14;
        int dmgMax = 32;
        float zoom = 1.12f;
    }

    public ArenaPrototype(PrototypeOptions options) {
        this.options = options;
    }

    @Override
    public void create() {
        if (options.keys != null) {
            for (String k : options.keys.split(",")) {
                int code = Input.Keys.valueOf(k.trim());
                if (code < 0) {
                    throw new IllegalArgumentException("Tombol tidak dikenal di --tekan: " + k);
                }
                scriptKeys.add(code);
            }
        }
        batch = new SpriteBatch(2000);
        lit = Shaders.load("sprite.vert", "lit.frag");
        post = new PostProcessor(SCREEN_W, SCREEN_H);
        hud = new Hud(this);
        flatNormal = solid(128, 128, 255);
        pixel = solid(255, 255, 255);
        glow = radialTexture(64);
        shaft = shaftTexture();
        effects = new FxPlayer();
        story = new StoryLayer(this);
        title = new TitleLayer(this);
        cam.setToOrtho(false, ArenaData.VIEW_W, ArenaData.VIEW_H);
        for (int i = 0; i < ARENAS.length; i++) {
            if (ARENAS[i].equals(options.arena)) {
                arenaIndex = i;
            }
        }
        if (options.title) {
            loadArena("girah", null);            // title backdrop: Desa Girah at dusk
            title.open();
        } else {
            loadArena(arenaIndex, options.mode);
        }
    }

    /** Menu "Mulai": the opening scene of Babak 1; its script continues into the first battle. */
    void startNewGame() {
        bag = BattleSession.newBag();
        storyStatus = new id.girah.cerita.StatusCerita();
        story.start("contoh_babak1");
    }

    boolean cameraEffects() {
        return fx.bloom && fx.dof && fx.grade;
    }

    void toggleCameraEffects() {
        boolean on = !cameraEffects();
        fx.bloom = on;
        fx.dof = on;
        fx.grade = on;
    }

    private static Texture solid(int r, int g, int b) {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(r / 255f, g / 255f, b / 255f, 1f);
        p.fill();
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    private static Texture radialTexture(int size) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x + 0.5f) / size * 2f - 1f;
                float dy = (y + 0.5f) / size * 2f - 1f;
                float d = Math.max(0f, 1f - (float) Math.sqrt(dx * dx + dy * dy));
                p.setColor(1f, 1f, 1f, d * d);
                p.drawPixel(x, y);
            }
        }
        Texture t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p.dispose();
        return t;
    }

    private static Texture shaftTexture() {
        Pixmap p = new Pixmap(32, 128, Pixmap.Format.RGBA8888);
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 32; x++) {
                float across = 1f - Math.abs((x + 0.5f) / 16f - 1f);
                float along = 1f - y / 128f;
                p.setColor(1f, 1f, 1f, across * across * (0.25f + 0.75f * along));
                p.drawPixel(x, 127 - y);
            }
        }
        Texture t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p.dispose();
        return t;
    }

    private void loadArena(int index, String modeName) {
        arenaIndex = index;
        loadArena(ARENAS[index], modeName);
    }

    /** Memuat arena atau lokasi cerita mana pun dari {@code assets/arena/<key>/} (dipakai naskah cerita). */
    void loadArena(String key, String modeName) {
        if (session != null) {
            session.stop();                      // panduan 10.5: AI berhenti saat layar battle ditutup
            session = null;
        }
        if (arena != null) {
            arena.dispose();
        }
        arena = new ArenaData(key, spriteCache);
        mode = arena.mode(modeName);
        particles.clear();
        effects.clear();
        later.clear();
        tempLights.clear();
        action = null;
        popups.clear();
        popupText.clear();
        applyGrade();
        if (!options.pamer) {
            if (bag == null) {
                bag = BattleSession.newBag();
            }
            session = BattleSession.create(this, arena, bag, options.demo, options.fast, options.weakBoss);
        }
    }

    private void applyGrade() {
        fx.sat = mode.sat;
        fx.tint = mode.tint;
        fx.lift = mode.lift;
        fx.vignette = mode.vignette;
        fx.threshold = "day".equals(mode.name) ? 0.8f : 0.55f;           // bright day skies should not glow
        fx.bloomAmount = "day".equals(mode.name) ? 0.5f : 1.1f;
    }

    // ------------------------------------------------------------------ input & update

    private void handleInput() {
        if (title.active) {
            return;                              // the title screen reads its own keys
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.C) && !story.active) {
            story.start("contoh_babak2");
            return;
        }
        if (story.active) {
            // the story screen owns the keys; only the view toggles still work
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                story.active = false;
            }
            return;
        }
        if (session != null) {
            feedScriptKeys();
            id.girah.battle.StatusBattle end = session.input();
            if (end != null) {
                afterBattle(end);
                return;
            }
        } else {
            menuInput();
            showcaseKeys();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F12)) {
            hud.showHelp = !hud.showHelp;
        }
        commonKeys();
    }

    /** Tombol pamer (tanpa back end): hanya aktif di mode {@code --pamer}. */
    private void showcaseKeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            startAction(false);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
            startAction(true);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            startPendekarJurus();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            showcase();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.X)) {
            startEnemyAttack();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            Actor25D p = pendekar();
            if (p != null) {
                if (p.isDown()) {
                    p.stopAnimation();
                    narration = p.name + " bangkit kembali.";
                } else {
                    p.play("gugur");
                    narration = p.name + " roboh berlutut, kerisnya tertancap di tanah.";
                }
            }
        }
    }

    /** Tombol tampilan yang berlaku di semua mode. */
    private void commonKeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            useNormals = !useNormals;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            fx.dof = !fx.dof;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            fx.bloom = !fx.bloom;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) {
            rig.enabled = !rig.enabled;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            fx.grade = !fx.grade;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F6)) {
            useShadows = !useShadows;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
            hud.visible = !hud.visible;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.T) && arena.modes.size() > 1) {
            List<String> names = new ArrayList<>(arena.modes.keySet());
            mode = arena.modes.get(names.get((names.indexOf(mode.name) + 1) % names.size()));
            particles.clear();
            applyGrade();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            loadArena(0, null);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            loadArena(1, null);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            saveScreenshot("screenshots/arena-" + System.currentTimeMillis() + ".png");
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }
    }

    /** Efek yang muncul di sasaran saat serangan biasa atau jurus Pendekar mengenai. */
    private String hitEffect(Action a) {
        if ("jurus".equals(a.anim) && "pendekar".equals(a.actor.id)) {
            return "tebasan";
        }
        if ("jurus".equals(a.anim) && "pengintai".equals(a.actor.id)) {
            return "langkah_bayangan";
        }
        if (a.actor.isEnemy() && (a.actor.id.startsWith("leak")
                || (a.actor.id.equals("calon_arang") && a.actor.nextForm == null))) {
            return "cakar";
        }
        return "sabetan";
    }

    /** Jarak berhenti di depan sasaran untuk serangan jarak dekat (px dunia, kira-kira setengah badan + sela). */
    private static final float MELEE_GAP = 30f;

    /**
     * Serangan jarak dekat (maju ke sasaran) atau jarak jauh (tetap di tempat)? Backend belum memberi
     * informasi ini (lihat docs/catatan-revisi-backend.md B7), jadi sementara ditentukan di sini.
     */
    private boolean isMelee(Action a) {
        String id = a.actor.id;
        if (id.equals("calon_arang")) {
            return a.actor.nextForm == null;       // human form curses from afar, ritual form claws
        }
        return !(id.equals("ratna") || id.equals("bharadah"));
    }

    /** Tindakan tertunda (detik dunia). */
    private static final class Later {
        final float at;
        final Runnable run;

        Later(float at, Runnable run) {
            this.at = at;
            this.run = run;
        }
    }

    private final List<Later> later = new ArrayList<>();

    private void after(float seconds, Runnable r) {
        later.add(new Later(time + seconds, r));
    }

    private Actor25D find(String idPrefix) {
        for (Actor25D a : arena.actors) {
            if (a.id.startsWith(idPrefix) && !a.isDown()) {
                return a;
            }
        }
        return null;
    }

    /** Urutan pamer efek (tombol E): setiap tekan memutar jurus tokoh berikutnya yang ada di arena. */
    private static final String[][] SHOWCASE = {
            {"peramu", "jurus", "ramuan_penawar", "ally"},
            {"pengintai", "jurus", "langkah_bayangan", "enemy"},
            {"ratna", "jurus", "doa_ratna", "party"},
            {"bharadah", "jurus", "lingkar_perlindungan", "partyGround"},
            {"pengikut", "jurus", "asap_wabah", "partyGround"},
            {"pemimpin_pengikut", "jurus", "jerat_kabut", "victimFeet"},
            {"calon_arang", "jurus", "kutukan", "victimFeet"},
            {"leak", "serang", "cakar", "victim"},
    };
    private int showcaseIndex;

    private void hurt(Actor25D t) {
        t.flash = 1f;
        t.play("terluka");
    }

    private void showcase() {
        if (action != null) {
            return;
        }
        for (int tries = 0; tries < SHOWCASE.length; tries++) {
            String[] s = SHOWCASE[showcaseIndex];
            showcaseIndex = (showcaseIndex + 1) % SHOWCASE.length;
            Actor25D actor = find(s[0]);
            List<Actor25D> party = party();
            if (actor == null || party.isEmpty()) {
                continue;
            }
            String effect = s[2];
            String place = s[3];
            if (actor.id.equals("calon_arang") && actor.nextForm != null) {
                effect = "asap_wabah";                      // human form: Sebar Wabah
                place = "partyGround";
            }
            Actor25D victim = pendekar() != null && !pendekar().isDown() ? pendekar() : party.get(0);
            float px = 0f;
            float py = 0f;
            for (Actor25D p : party) {
                px += p.footX();
                py += p.y;
            }
            px /= party.size();
            py /= party.size();
            if (place.equals("enemy")) {
                List<Actor25D> foes = enemies();
                if (foes.isEmpty()) {
                    continue;
                }
                Action a = new Action();
                a.actor = actor;
                a.target = foes.get(MathUtils.random(foes.size() - 1));
                a.anim = "jurus";
                a.animAt = 0.02f;
                a.hitAt = 0.4f;
                a.endAt = 1.1f;
                a.dmgMin = 30;
                a.dmgMax = 38;
                a.zoom = 1.14f;
                action = a;
                narration = actor.name + " melesat secepat bayangan. LANGKAH BAYANGAN!";
                return;
            } else if (place.equals("ally")) {
                effects.play(effect, victim.footX(), victim.y, 0.2f);
            } else if (place.equals("party")) {
                for (Actor25D p : party) {
                    effects.play(effect, p.footX(), p.y, 0.25f + MathUtils.random(0.15f));
                }
            } else if (place.equals("partyGround")) {
                effects.play(effect, px, py - 6f, 0.2f);
                if (actor.isEnemy()) {
                    after(0.8f, () -> {
                        for (Actor25D p : party()) {
                            hurt(p);
                        }
                    });
                }
            } else if (place.equals("victimFeet")) {
                effects.play(effect, victim.footX(), victim.y, 0.2f);
                after(0.7f, () -> hurt(victim));
            } else {
                effects.play(effect, victim.footX(), victim.y + 24f, 0.2f);
                after(0.3f, () -> hurt(victim));
            }
            actor.play(s[1]);
            narration = actor.name + ": " + effect.replace('_', ' ') + ".";
            return;
        }
    }

    List<Actor25D> party() {
        List<Actor25D> out = new ArrayList<>();
        for (Actor25D a : arena.actors) {
            if (a.isParty()) {
                out.add(a);
            }
        }
        return out;
    }

    private List<Actor25D> enemies() {
        List<Actor25D> out = new ArrayList<>();
        for (Actor25D a : arena.actors) {
            if (a.isEnemy() && !a.isDown()) {
                out.add(a);
            }
        }
        return out;
    }


    // ------------------------------------------------------------------ battle menu (brief C6)

    private static final String[] MENU = {"Serang", "Jurus", "Pusaka"};
    private static final float JURUS_COST = 0.3f;
    private int menuIndex;
    /** Sasaran yang bisa dipilih saat memilih sasaran, atau null saat memilih aksi. */
    private List<Actor25D> targets;
    private int targetIndex;

    /** Pengelana yang sedang bergiliran (yang gugur dilewati), atau null jika semua gugur. */
    Actor25D activeActor() {
        if (session != null) {
            return session.activeActor();
        }
        List<Actor25D> party = party();
        for (int k = 0; k < party.size(); k++) {
            Actor25D a = party.get((activeIndex() + k) % party.size());
            if (!a.isDown()) {
                return a;
            }
        }
        return null;
    }

    int round() {
        return turn / Math.max(1, party().size()) + 1;
    }

    /** "Ronde 2" atau "Ronde 2 dari 8" (batas ronde). */
    String roundLabel() {
        return session != null ? session.roundLabel() : "Ronde " + round();
    }

    /** Urutan giliran sungguhan (kedua pihak), atau {@code null} di mode pamer. */
    List<Actor25D> turnOrder() {
        return session != null ? session.turnOrder() : null;
    }

    /** Musuh yang sedang berpikir (tanda "..."), atau {@code null}. */
    Actor25D thinkingActor() {
        return session != null ? session.thinkingActor() : null;
    }

    /** Judul hasil akhir pertarungan untuk HUD, atau {@code null}. */
    String resultTitle() {
        return session != null ? session.resultTitle() : null;
    }

    String resultHint() {
        return session != null ? session.resultHint() : "";
    }

    String babadMessage() {
        return session != null ? session.babadMessage : null;
    }

    /** Setelah pemain menekan Enter pada hasil akhir (panduan bagian 8). */
    private void afterBattle(id.girah.battle.StatusBattle end) {
        Gdx.app.log("battle", "setelah " + end + ": " + (end == id.girah.battle.StatusBattle.KALAH ? "ulangi" : "layar judul"));
        if (end == id.girah.battle.StatusBattle.KALAH) {
            loadArena(arena.key, mode.name);      // ulangi: pengelana dan musuh baru, BattleManager baru
            return;
        }
        // MENANG / WAKTU_HABIS: naskah lanjutan dan Ending 3 belum ada (catatan D6), kembali ke layar judul
        loadArena("girah", null);
        title.open();
    }

    float clock() {
        return time;
    }

    boolean menuOpen() {
        if (session != null) {
            return session.menuOpen();
        }
        return action == null && later.isEmpty() && transforming == null && activeActor() != null;
    }

    String[] menuItems() {
        if (session != null) {
            return session.menuItems();
        }
        return MENU;
    }

    int menuIndex() {
        if (session != null) {
            return session.menuIndex();
        }
        return menuIndex;
    }

    boolean choosingTarget() {
        if (session != null) {
            return session.choosingTarget();
        }
        return targets != null;
    }

    Actor25D menuTarget() {
        if (session != null) {
            return session.menuTarget();
        }
        return menuOpen() && targets != null && !targets.isEmpty() ? targets.get(targetIndex % targets.size()) : null;
    }

    boolean menuEnabled(int i) {
        if (session != null) {
            return session.menuEnabled(i);
        }
        Actor25D a = activeActor();
        if (a == null) {
            return false;
        }
        if (i == 1) {
            return a.energy >= JURUS_COST - 1e-3f;
        }
        return i != 2;      // Pusaka: belum ada di prototipe (backend punya paket pusaka)
    }

    private void menuInput() {
        if (!menuOpen()) {
            targets = null;
            return;
        }
        boolean up = Gdx.input.isKeyJustPressed(Input.Keys.UP);
        boolean down = Gdx.input.isKeyJustPressed(Input.Keys.DOWN);
        boolean left = Gdx.input.isKeyJustPressed(Input.Keys.LEFT);
        boolean right = Gdx.input.isKeyJustPressed(Input.Keys.RIGHT);
        boolean ok = Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.Z);
        boolean back = Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE);
        if (targets == null) {
            if (up) {
                menuIndex = (menuIndex + MENU.length - 1) % MENU.length;
            }
            if (down) {
                menuIndex = (menuIndex + 1) % MENU.length;
            }
            if (ok) {
                chooseAction();
            }
        } else {
            if (up || left) {
                targetIndex = (targetIndex + targets.size() - 1) % targets.size();
            }
            if (down || right) {
                targetIndex = (targetIndex + 1) % targets.size();
            }
            if (back) {
                targets = null;
            }
            if (ok) {
                Actor25D t = targets.get(targetIndex % targets.size());
                targets = null;
                perform(activeActor(), menuIndex, t);
            }
        }
    }

    /** What the chosen action targets: "enemy", "ally" or "all" (no choice needed). */
    private String targetKind(Actor25D a, int item) {
        if (item == 0) {
            return "enemy";
        }
        switch (a.id) {
            case "juru_mantra":
                return "all";
            case "peramu":
                return "ally";
            default:
                return "enemy";
        }
    }

    private void chooseAction() {
        Actor25D a = activeActor();
        if (a == null) {
            return;
        }
        if (!menuEnabled(menuIndex)) {
            narration = menuIndex == 2 ? "Belum ada pusaka yang bisa dipakai." : a.name + " kehabisan energi untuk jurus.";
            return;
        }
        String kind = targetKind(a, menuIndex);
        if (kind.equals("all")) {
            perform(a, menuIndex, null);
            return;
        }
        List<Actor25D> list = kind.equals("ally") ? new ArrayList<>(party()) : enemies();
        list.removeIf(Actor25D::isDown);
        if (list.isEmpty()) {
            return;
        }
        // top of the screen first, so up/down feels natural
        list.sort((p, q) -> Float.compare(q.y, p.y));
        targets = list;
        targetIndex = kind.equals("ally") ? list.indexOf(a) < 0 ? 0 : list.indexOf(a) : 0;
        narration = kind.equals("ally") ? "Pilih kawan yang diobati." : "Pilih sasaran.";
    }

    private void perform(Actor25D a, int item, Actor25D target) {
        if (a == null) {
            return;
        }
        Action act = new Action();
        act.actor = a;
        act.target = target;
        if (item == 0) {
            narration = a.name + " menyerang " + target.name + "!";
            action = act;
            return;
        }
        a.energy = Math.max(0f, a.energy - JURUS_COST);
        switch (a.id) {
            case "juru_mantra":
                act.jurus = true;
                narration = a.name + " merapal Mantra Lingkar Suci. Cahaya putih melingkar di sekeliling medan!";
                action = act;
                return;
            case "peramu": {
                a.play("jurus");
                effects.play("ramuan_penawar", target.footX(), target.y, 0.2f);
                narration = a.name + " menuangkan Ramuan Penawar untuk " + target.name + ".";
                after(0.9f, () -> {
                    int heal = 30;
                    target.hp = Math.min(1f, target.hp + heal / 120f);
                    popup(target.footX(), target.y + 54f, "+" + heal);
                    narration = target.name + " pulih " + heal + " nyawa.";
                });
                after(1.4f, () -> turn++);
                return;
            }
            case "pengintai":
                narration = a.name + " melesat secepat bayangan. LANGKAH BAYANGAN!";
                act.anim = "jurus";
                act.animAt = 0.02f;
                act.hitAt = 0.4f;
                act.endAt = 1.1f;
                act.dmgMin = 30;
                act.dmgMax = 38;
                act.zoom = 1.14f;
                action = act;
                return;
            default:
                narration = a.name + " memusatkan tenaga dalam pada keris pusakanya. TEBASAN KERIS PUSAKA!";
                act.anim = "jurus";
                act.animAt = 0.05f;
                act.hitAt = 0.5f;
                act.endAt = 1.3f;
                act.dmgMin = 40;
                act.dmgMax = 48;
                act.zoom = 1.16f;
                action = act;
        }
    }

    int activeIndex() {
        return turn % Math.max(1, party().size());
    }

    private void startAction(boolean jurus) {
        if (action != null) {
            return;
        }
        List<Actor25D> party = party();
        List<Actor25D> foes = enemies();
        if (party.isEmpty() || foes.isEmpty()) {
            return;
        }
        Action a = new Action();
        a.jurus = jurus;
        if (jurus) {
            for (Actor25D p : party) {
                if (p.staff != null) {
                    a.actor = p;
                }
            }
            if (a.actor == null) {
                a.actor = party.get(activeIndex());
            }
            if (a.actor.isDown()) {
                return;
            }
            narration = a.actor.name + " merapal Mantra Lingkar Suci. Cahaya putih melingkar di sekeliling medan!";
        } else {
            a.actor = activeActor();
            if (a.actor == null) {
                return;
            }
            a.target = foes.get(MathUtils.random(foes.size() - 1));
            narration = a.actor.name + " menyerang " + a.target.name + "!";
        }
        action = a;
    }

    private Actor25D pendekar() {
        for (Actor25D a : arena.actors) {
            if ("pendekar".equals(a.id)) {
                return a;
            }
        }
        return null;
    }

    /** K: Pendekar memakai jurus Tebasan Keris Pusaka (animasi jurus, satu sasaran, kerusakan besar). */
    private void startPendekarJurus() {
        Actor25D p = pendekar();
        List<Actor25D> foes = enemies();
        if (action != null || p == null || p.isDown() || foes.isEmpty()) {
            return;
        }
        Action a = new Action();
        a.actor = p;
        a.target = foes.get(MathUtils.random(foes.size() - 1));
        a.anim = "jurus";
        a.animAt = 0.05f;
        a.hitAt = 0.5f;
        a.endAt = 1.3f;
        a.dmgMin = 40;
        a.dmgMax = 48;
        a.zoom = 1.16f;
        narration = p.name + " memusatkan tenaga dalam pada keris pusakanya. TEBASAN KERIS PUSAKA!";
        action = a;
    }

    /** X: musuh acak menyerang Pendekar, yang memutar animasi terluka. */
    private void startEnemyAttack() {
        Actor25D p = pendekar();
        List<Actor25D> foes = enemies();
        if (action != null || p == null || p.isDown() || foes.isEmpty()) {
            return;
        }
        Action a = new Action();
        a.actor = foes.get(MathUtils.random(foes.size() - 1));
        a.target = p;
        a.anim = "serang";
        a.dmgMin = 9;
        a.dmgMax = 16;
        narration = a.actor.name + " menyerang " + p.name + "!";
        action = a;
    }

    private void updateAction(float dt) {
        if (action == null) {
            rig.release();
            return;
        }
        Action a = action;
        a.t += dt;
        if (!a.jurus) {
            float dir = a.actor.x > a.target.x ? -1f : 1f;    // pengelana maju ke kiri, musuh ke kanan
            boolean melee = isMelee(a);
            // melee: stop just in front of the target (half a body away), ranged: stay in place
            float approach = melee ? Math.max(0f, Math.abs(a.actor.x - a.target.x) - MELEE_GAP) : 0f;
            float depth = melee ? a.target.y - a.actor.y - 1f : 0f;   // 1 px in front of the target
            float s = a.endAt / 1.0f;                        // regangkan lintasan maju-mundur untuk aksi yang lebih lama
            float k;
            if (a.t < 0.25f * s) {
                k = MathUtils.sin(a.t / (0.25f * s) * MathUtils.HALF_PI);
            } else if (a.t < 0.55f * s) {
                k = 1f;
            } else if (a.t < 0.85f * s) {
                k = 1f - MathUtils.sin((a.t - 0.55f * s) / (0.3f * s) * MathUtils.HALF_PI);
            } else {
                k = 0f;
            }
            a.actor.offsetX = dir * approach * k;
            a.actor.offsetY = depth * k;
            if (!a.animStarted && a.t >= a.animAt) {
                a.animStarted = true;
                float len = a.actor.play(a.anim);
                float impact = a.actor.impactTime(a.anim);
                if (impact >= 0f) {
                    // the damage lands exactly on the impact frame, and the action lasts until the clip ends
                    a.hitAt = a.t + impact;
                    a.endAt = Math.max(a.endAt, a.t + len + 0.15f);
                }
            }
            rig.focus((a.actor.footX() + a.target.footX()) / 2f, (a.actor.y + a.target.y) / 2f + 20f, a.zoom);
            if (a.t >= a.hitAt && !a.hit) {
                a.hit = true;
                hitStop = a.dmgMax >= 40 ? 0.12f : 0.07f;      // hit-stop: big hits freeze a bit longer
                int dmg = MathUtils.random(a.dmgMin, a.dmgMax);
                a.target.flash = 1f;
                a.target.knock = 7f * dir;
                a.target.play("terluka");
                effects.play(hitEffect(a), a.target.footX(), a.target.y + 24f, 0f);
                fallIfBeaten = a.target;
                a.target.hp = Math.max(0.1f, a.target.hp - dmg / 120f);
                float hx = a.target.footX();
                float hy = a.target.y + a.target.height() * 0.45f;
                rig.shake(5f);
                particles.burst(hx, hy, 26, 1f, 0.85f, 0.55f, 90f);
                tempLights.add(new PointLight(hx, hy, 24f, 1f, 0.85f, 0.6f, 70f, 2.2f, 0f).temporary(0.35f, 60f));
                popup(hx, hy + 30f, "-" + dmg);
                narration = a.target.name + " menerima " + dmg + " kerusakan.";
            }
            if (a.t > a.endAt) {
                a.actor.offsetX = 0f;
                a.actor.offsetY = 0f;
                action = null;
                if (a.actor.isParty()) {
                    turn++;
                }
            }
        } else {
            List<Actor25D> foes = enemies();
            float cx = 0f;
            float cy = 0f;
            for (Actor25D e : foes) {
                cx += e.footX();
                cy += e.y;
            }
            cx /= foes.size();
            cy /= foes.size();
            rig.focus((cx + a.actor.footX()) / 2f, cy + 30f, 1.06f);
            if (!a.animStarted) {
                a.animStarted = true;
                a.actor.play("jurus");          // tongkat bersinar terang pada frame 1-4, mantra kena di 0,5 s
            }
            if (a.t < 0.5f) {
                a.actor.flash = 0.25f * MathUtils.sin(a.t * 30f) + 0.25f;
            }
            if (a.t >= 0.1f && !a.fxStarted) {
                // the ring starts early so it is fully drawn when the mantra hits
                a.fxStarted = true;
                effects.play("lingkar_suci", cx, cy, 0f);
            }
            if (a.t >= 0.5f && !a.hit) {
                a.hit = true;
                tempLights.add(new PointLight(cx, cy + 20f, 30f, 0.95f, 0.95f, 1f, 30f, 2.6f, 0f).temporary(0.9f, 300f));
                rig.shake(4f);
                for (Actor25D e : foes) {
                    e.flash = 1f;
                    e.knock = 6f;
                    e.hp = Math.max(0.1f, e.hp - 22f / 120f);
                    particles.burst(e.footX(), e.y + e.height() * 0.4f, 18, 0.95f, 0.95f, 1f, 70f);
                    popup(e.footX(), e.y + e.height() * 0.45f + 30f, "-22");
                }
                narration = "Mantra Lingkar Suci mengenai semua lawan (22 kerusakan).";
                a.actor.energy = Math.max(0f, a.actor.energy - 0.3f);
            }
            if (a.t > 1.5f) {
                action = null;
                if (a.actor.isParty()) {
                    turn++;
                }
            }
        }
    }

    /** Musuh yang nyawanya habis setelah serangan terakhir; animasi gugur diputar sesudah terluka. */
    private Actor25D fallIfBeaten;

    /** Bos yang sedang berubah wujud, dan saat wujud barunya muncul. */
    private Actor25D transforming;
    private float transformAt;

    private void checkTransform() {
        if (transforming == null || time < transformAt) {
            return;
        }
        Actor25D old = transforming;
        Actor25D next = old.nextForm;
        transforming = null;
        int i = arena.actors.indexOf(old);
        if (i < 0 || next == null) {
            return;
        }
        arena.actors.set(i, next);
        next.flash = 1f;
        float cx = next.footX();
        float cy = next.y + next.height() * 0.45f;
        rig.shake(9f);
        particles.burst(cx, cy, 60, 0.95f, 0.35f, 0.3f, 140f);
        tempLights.add(new PointLight(cx, cy, 30f, 1f, 0.3f, 0.25f, 120f, 3f, 0f).temporary(1.2f, 140f));
        narration = next.name + " bangkit! Surai putihnya menjuntai sampai tanah. (Fase 2)";
    }

    private void checkBeaten() {
        Actor25D t = fallIfBeaten;
        if (t != null && action == null) {
            fallIfBeaten = null;
            if (t.isEnemy() && t.hp <= 0.1f) {
                t.hp = 0f;
                float d = t.play("gugur");
                if (t.nextForm != null) {
                    // fase 1 -> fase 2: the "gugur" clip of the human form is the start of the transformation
                    transforming = t;
                    transformAt = time + d + 0.6f;
                    narration = t.name + " jatuh berlutut... rambutnya memanjang, kukunya mencakar tanah. Ia berubah wujud!";
                } else {
                    narration = t.name + " memudar menjadi asap dupa dan lenyap.";
                }
            }
        }
    }

    void popup(float x, float y, String text) {
        popups.add(new float[] {x, y, 1.0f});
        popupText.add(text);
    }

    /** Sisa waktu hit-stop (detik nyata): animasi dan aksi hampir berhenti sesaat saat benturan. */
    private float hitStop;
    private boolean exitPending;
    private int transformShotFrame = -1;
    /** Tombol uji (--tekan) yang tersisa, dan tombol uji untuk frame ini. */
    private final java.util.ArrayDeque<Integer> scriptKeys = new java.util.ArrayDeque<>();
    private int scriptKey = -1;
    private int scriptWait;

    /** Seperti {@code Gdx.input.isKeyJustPressed}, ditambah tombol uji dari {@code --tekan}. */
    boolean justPressed(int key) {
        return Gdx.input.isKeyJustPressed(key) || key == scriptKey;
    }

    /** Mengambil satu tombol uji ketika sesi siap menerima tombol (jeda 12 frame antar tombol). */
    private void feedScriptKeys() {
        scriptKey = -1;
        if (scriptKeys.isEmpty() || session == null) {
            return;
        }
        if (!session.acceptsKeys()) {
            scriptWait = 0;
            return;
        }
        if (++scriptWait < 12) {
            return;
        }
        scriptWait = 0;
        scriptKey = scriptKeys.poll();
        Gdx.app.log("uji", "tekan " + Input.Keys.toString(scriptKey));
    }

    /** Dipanggil {@link BattleSession} saat wujud baru bos muncul (untuk screenshot uji). */
    void onBossTransformed() {
        if (options.shotTransform != null) {
            transformShotFrame = frame + 20;
        }
    }

    /** Pemicu uji mode pamer (tanpa back end). */
    private void pamerTriggers() {
        if (frame == options.chooseAt) {
            // test: pick a menu item for the active member (shows target selection)
            menuIndex = options.chooseItem;
            chooseAction();
        }
        if (frame == options.fxAt) {
            showcaseIndex = options.fxSkip;
            showcase();
        }
        if (frame == options.pendekarJurusAt) {
            startPendekarJurus();
        }
        if (frame == options.enemyAttackAt) {
            startEnemyAttack();
        }
        if (frame == options.bossDownAt) {
            // test: the boss loses its last HP (Calon Arang changes form)
            for (Actor25D a : arena.actors) {
                if (a.nextForm != null) {
                    a.hp = 0.05f;
                    fallIfBeaten = a;
                }
            }
        }
        if (frame == options.gugurAt && pendekar() != null) {
            pendekar().play("gugur");
        }
    }

    /** Membekukan aksi sesaat saat benturan (dipakai juga oleh {@link BattleSession}). */
    void hitStop(float seconds) {
        hitStop = Math.max(hitStop, seconds);
    }

    private void update(float realDt) {
        time += realDt;
        // hit-stop slows the action and the actors; the camera shake and particles keep real time
        float dt = hitStop > 0f ? realDt * 0.05f : realDt;
        hitStop = Math.max(0f, hitStop - realDt);
        handleInput();
        if (session != null) {
            if (frame == options.attackAt) {
                session.quickAction(0);
            }
            if (frame == options.jurusAt) {
                session.quickAction(1);
            }
            session.update(dt);
            narration = session.narration;
            if (options.exitOnEnd && session.resultTitle() != null && !exitPending) {
                Gdx.app.log("battle", "hasil: " + session.resultTitle() + " | " + session.babadMessage);
                exitPending = true;                    // keluar setelah frame ini digambar (screenshot hasil)
            }
        } else if (options.demo) {
            demoTimer += dt;
            if (demoTimer > 2.6f && action == null) {
                demoTimer = 0f;
                startAction(turn % 4 == 3);
            }
        }
        if (session == null && frame == options.attackAt) {
            startAction(false);
        }
        if (session == null && frame == options.jurusAt) {
            startAction(true);
        }
        if (frame == options.storyAt && options.story != null) {
            story.start(options.story);
            story.jump(options.storyLine);
        }
        if (frame == options.startAt && title.active) {
            title.active = false;
            startNewGame();
        }
        if (frame == options.storyEndAt) {
            story.finish();
            if (options.storyChoice >= 0 && story.choosing()) {
                story.choose(options.storyChoice);     // uji: pilih opsi; balasannya tampil berikutnya
            }
        }
        if (session == null) {
            pamerTriggers();
        }
        updateAction(dt);
        checkBeaten();
        checkTransform();
        rig.update(realDt, time);
        for (Actor25D a : arena.actors) {
            a.update(dt);
        }
        for (int i = tempLights.size() - 1; i >= 0; i--) {
            tempLights.get(i).update(realDt);
            if (tempLights.get(i).isExpired()) {
                tempLights.remove(i);
            }
        }
        for (int i = popups.size() - 1; i >= 0; i--) {
            float[] p = popups.get(i);
            p[2] -= realDt;
            p[1] += realDt * 18f;
            if (p[2] <= 0) {
                popups.remove(i);
                popupText.remove(i);
            }
        }
        particles.ambient(mode.particles, realDt);
        if (mode.torches && MathUtils.randomBoolean(0.5f)) {
            float[] t = arena.torches.get(MathUtils.random(arena.torches.size() - 1));
            particles.ember(t[0], t[1] + 4f);
        }
        for (Actor25D a : arena.actors) {
            int[] sp = a.smokePoint();
            if (sp != null) {
                float rate = a.isPlaying("jurus") ? 60f : 7f;           // kepulan per detik
                if (MathUtils.random() < rate * realDt) {
                    particles.incense(a.drawX() + sp[0], a.drawY(time) + a.height() - sp[1]);
                }
            }
        }
        particles.update(realDt);
        effects.update(realDt);
        story.update(realDt);
        title.update(realDt);
        for (int i = later.size() - 1; i >= 0; i--) {
            if (time >= later.get(i).at) {
                Later l = later.remove(i);
                l.run.run();
            }
        }
    }

    // ------------------------------------------------------------------ rendering helpers

    float camCenterX() {
        return ArenaData.VIEW_W / 2f + rig.camX;
    }

    float camCenterY() {
        return ArenaData.VIEW_H / 2f + rig.camY;
    }

    /** Dunia (640x360) ke piksel framebuffer/HUD (1280x720), lewat kamera bidang main. */
    float toScreenX(float wx) {
        return ((wx - camCenterX()) * rig.zoom + ArenaData.VIEW_W / 2f) * SCALE;
    }

    float toScreenY(float wy) {
        return ((wy - camCenterY()) * rig.zoom + ArenaData.VIEW_H / 2f) * SCALE;
    }

    private void useLayerCamera(float parallax, float shakeX, float shakeY) {
        float f = rig.enabled ? parallax : 0f;
        cam.position.set(ArenaData.VIEW_W / 2f + (rig.camX + shakeX) * f,
                ArenaData.VIEW_H / 2f + (rig.camY + shakeY) * f, 0f);
        cam.zoom = 1f / (1f + (rig.zoom - 1f) * f);   // rig.zoom = perbesaran; zoom LibGDX > 1 justru memperkecil
        cam.update();
        batch.setProjectionMatrix(cam.combined);
    }

    private void collectLights() {
        frameLights.clear();
        boolean night = mode.torches;
        if (night) {
            for (float[] t : arena.torches) {
                frameLights.add(new PointLight(t[0], t[1], 34f, 0.92f, 0.68f, 0.36f, 135f, 1.5f, 0.18f));
            }
        }
        frameLights.addAll(arena.staticLights);
        for (Actor25D a : arena.actors) {
            float baseX = a.drawX();
            float baseY = a.drawY(time);
            if (a.eyes != null && night && !a.isActing()) {
                float ex = baseX + (a.eyes[0] + a.eyes[2]) / 2f;
                float ey = baseY + a.height() - a.eyes[1];
                float[] ec = a.eyeColor;
                frameLights.add(new PointLight(ex, ey, 12f, ec[0] * 0.9f, ec[1] * 0.9f, ec[2] * 0.85f, 34f, 1.1f, 0.1f));
            }
            int[] tip = a.staffPoint();
            if (tip != null) {
                float sx = baseX + tip[0];
                float sy = baseY + a.height() - tip[1];
                frameLights.add(new PointLight(sx, sy, 16f, 0.95f, 0.95f, 0.9f, 46f, night ? 0.9f : 0.25f, 0.05f));
            }
        }
        frameLights.addAll(tempLights);
    }

    private void uploadLights() {
        int n = Math.min(MAX_LIGHTS, frameLights.size());
        for (int i = 0; i < n; i++) {
            PointLight l = frameLights.get(i);
            float k = l.currentIntensity(time);
            lightPos[i * 3] = toScreenX(l.x);
            lightPos[i * 3 + 1] = toScreenY(l.y);
            lightPos[i * 3 + 2] = l.z * SCALE;
            lightColor[i * 3] = l.r * k;
            lightColor[i * 3 + 1] = l.g * k;
            lightColor[i * 3 + 2] = l.b * k;
            lightRadius[i] = l.radius * SCALE * rig.zoom;
        }
        lit.setUniformi("u_lightCount", n);
        if (n > 0) {
            lit.setUniform3fv("u_lightPos", lightPos, 0, n * 3);
            lit.setUniform3fv("u_lightColor", lightColor, 0, n * 3);
            lit.setUniform1fv("u_lightRadius", lightRadius, 0, n);
        }
        lit.setUniformf("u_ambient", mode.ambient[0], mode.ambient[1], mode.ambient[2]);
        lit.setUniformf("u_sunColor", mode.sun[0], mode.sun[1], mode.sun[2]);
        lit.setUniformf("u_sunDir", mode.sunDir[0], mode.sunDir[1], mode.sunDir[2]);
        lit.setUniformi("u_normals", 1);
        lit.setUniformf("u_flash", 0f);
    }

    private void bindNormals(Texture normals, float lit01) {
        batch.flush();
        (normals != null ? normals : flatNormal).bind(1);
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        lit.setUniformf("u_useNormals", normals != null && useNormals ? 1f : 0f);
        lit.setUniformf("u_lit", lit01);
    }

    private TextureRegion region(Texture t) {
        return regions.computeIfAbsent(t, TextureRegion::new);
    }

    private PointLight nearestLight(float x, float y) {
        PointLight best = null;
        float bestD = Float.MAX_VALUE;
        for (PointLight l : frameLights) {
            if (l.radius < 60f || l.ttl >= 0) {
                continue;
            }
            float d = (l.x - x) * (l.x - x) + (l.y - y) * (l.y - y);
            if (d < bestD) {
                bestD = d;
                best = l;
            }
        }
        return best;
    }

    private void drawShadows() {
        batch.setShader(null);
        for (Actor25D a : arena.actors) {
            float w = a.width();
            float fx = a.footX();
            float fy = a.footY();
            batch.setColor(0f, 0f, 0f, 0.55f);
            batch.draw(glow, fx - w * 0.28f, fy - 4f, w * 0.56f, 9f);
            if (!useShadows) {
                continue;
            }
            float vx;
            float vy;
            float strength;
            PointLight l = mode.torches ? nearestLight(fx, fy) : null;
            if (l != null) {
                vx = fx - l.x;
                vy = fy - l.y;
                float d = (float) Math.sqrt(vx * vx + vy * vy);
                strength = MathUtils.clamp(1.2f - d / 260f, 0.15f, 0.55f);
            } else {
                vx = -mode.sunDir[0] * 100f;
                vy = -mode.sunDir[1] * 100f;
                strength = 0.32f;
            }
            float sy = vy < 0 ? -0.34f : 0.22f;
            float lean = MathUtils.clamp(vx / Math.max(Math.abs(vy), 25f), -1.6f, 1.6f) * 0.55f;
            float shx = lean / sy;
            float lift = a.floating ? 0.55f : 1f;
            affine.setToTranslation(fx, fy).shear(shx, 0f).scale(1f, sy).translate(-w / 2f, -3f);
            batch.setColor(0.02f, 0.02f, 0.06f, strength * lift);
            batch.draw(region(a.texture), w, a.height(), affine);
        }
        batch.setColor(1f, 1f, 1f, 1f);
        batch.setShader(lit);
    }

    private void drawAdditive() {
        batch.setShader(null);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        if (mode.shafts) {
            for (int i = 0; i < 5; i++) {
                float x = 140f + i * 110f;
                float a = 0.07f + 0.03f * MathUtils.sin(time * 0.6f + i);
                affine.setToTranslation(x, 360f).shear(0.55f, 0f).scale(1f, -1f).translate(-28f, 0f);
                batch.setColor(1f, 0.93f, 0.72f, a);
                batch.draw(region(shaft), 56f, 260f, affine);
            }
        }
        if (mode.torches) {
            for (float[] t : arena.torches) {
                float f = 0.85f + 0.15f * MathUtils.sin(time * 17f + t[0]) + 0.1f * MathUtils.sin(time * 9f + t[1]);
                batch.setColor(1f, 0.55f, 0.2f, 0.35f * f);
                batch.draw(glow, t[0] - 26f, t[1] - 22f, 52f, 52f);
                batch.setColor(1f, 0.75f, 0.35f, 0.9f);
                batch.draw(pixel, t[0] - 2f, t[1] - 1f, 4f, 5f + 2f * f);
                batch.setColor(1f, 0.95f, 0.75f, 1f);
                batch.draw(pixel, t[0] - 1f, t[1], 2f, 3f + f);
                batch.setColor(1f, 0.7f, 0.3f, 0.6f);
                batch.draw(glow, t[0] - 7f, t[1] - 4f, 14f, 16f);
            }
            for (Actor25D a : arena.actors) {
                if (a.eyes != null && !a.isActing()) {
                    float bx = a.drawX();
                    float by = a.drawY(time) + a.height();
                    batch.setColor(a.eyeColor[0], a.eyeColor[1], a.eyeColor[2], 0.5f);
                    batch.draw(glow, bx + a.eyes[0] - 5f, by - a.eyes[1] - 5f, 10f, 10f);
                    batch.draw(glow, bx + a.eyes[2] - 5f, by - a.eyes[3] - 5f, 10f, 10f);
                }
            }
        }
        for (Actor25D a : arena.actors) {
            int[] tip = a.staffPoint();
            if (tip != null) {
                float sx = a.drawX() + tip[0];
                float sy = a.drawY(time) + a.height() - tip[1];
                batch.setColor(1f, 1f, 0.92f, mode.torches ? 0.6f : 0.3f);
                batch.draw(glow, sx - 7f, sy - 7f, 14f, 14f);
            }
        }
        for (PointLight l : tempLights) {
            float k = l.currentIntensity(time) / Math.max(0.01f, l.intensity);
            batch.setColor(l.r, l.g, l.b, 0.35f * k);
            float r = Math.min(l.radius, 160f) * 0.7f;
            batch.draw(glow, l.x - r, l.y - r * 0.5f, r * 2f, r);
        }
        particles.draw(batch, pixel);
        batch.setColor(1f, 1f, 1f, 1f);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setShader(lit);
    }

    private void renderScene() {
        collectLights();
        float sx = rig.shakeX();
        float sy = rig.shakeY();
        post.scene.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setShader(lit);
        batch.begin();
        uploadLights();

        useLayerCamera(0.08f, sx, sy);
        bindNormals(null, 0f);
        batch.draw(mode.sky, ArenaData.LAYER_X, ArenaData.LAYER_Y);
        for (ArenaData.Layer layer : arena.layers) {
            useLayerCamera(layer.parallax, sx, sy);
            bindNormals(layer.normals, layer.lit);
            batch.draw(layer.texture, ArenaData.LAYER_X, ArenaData.LAYER_Y);
        }

        useLayerCamera(1f, sx, sy);
        drawShadows();
        batch.setShader(null);
        effects.draw(batch, true);
        batch.setShader(lit);
        List<Actor25D> sorted = new ArrayList<>(arena.actors);
        sorted.sort((p, q) -> Float.compare(q.footY(), p.footY()));
        for (Actor25D a : sorted) {
            if (title.active || (story.active && a.isEnemy())) {
                continue;
            }
            bindNormals(a.normals, 1f);
            lit.setUniformf("u_flash", a.flash);
            batch.draw(a.texture, a.drawX(), a.drawY(time));
        }
        batch.flush();
        lit.setUniformf("u_flash", 0f);
        batch.setShader(null);
        effects.draw(batch, false);
        batch.setShader(lit);
        drawAdditive();

        useLayerCamera(arena.front.parallax, sx, sy);
        bindNormals(arena.front.normals, 1f);
        batch.draw(arena.front.texture, ArenaData.LAYER_X, ArenaData.LAYER_Y);
        batch.end();
        post.scene.end();
    }

    @Override
    public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 1f / 20f);
        if (options.shotPath != null) {
            dt = 1f / 60f;
        }
        update(dt);
        renderScene();
        post.render(batch, fx);
        if (title.active) {
            title.draw(batch);
        } else if (story.active) {
            story.draw(batch);
        } else {
            hud.draw(batch, arena, mode, fx, rig);
        }
        frame++;
        if (frame == transformShotFrame) {
            saveScreenshot(options.shotTransform);
        }
        if (exitPending) {
            if (options.shotPath != null) {
                saveScreenshot(options.shotPath);
            }
            Gdx.app.exit();
            return;
        }
        if (options.shotPath != null && frame == options.shotFrame && !options.exitOnEnd) {
            saveScreenshot(options.shotPath);
            Gdx.app.exit();
        }
    }

    private void saveScreenshot(String path) {
        int w = Gdx.graphics.getBackBufferWidth();
        int h = Gdx.graphics.getBackBufferHeight();
        Pixmap pm = Pixmap.createFromFrameBuffer(0, 0, w, h);
        // the window ignores alpha, but see-through UI (dimming) leaves alpha < 1 in the buffer:
        // make the saved picture opaque so it looks like the screen
        java.nio.ByteBuffer px = pm.getPixels();
        for (int i = 3; i < px.limit(); i += 4) {
            px.put(i, (byte) 0xFF);
        }
        PixmapIO.writePNG(Gdx.files.local(path), pm, Deflater.DEFAULT_COMPRESSION, true);
        pm.dispose();
        Gdx.app.log("prototipe", "screenshot disimpan: " + Gdx.files.local(path).file().getAbsolutePath());
    }

    String arenaTitle() {
        return arena.title;
    }

    String modeName() {
        return mode.name;
    }

    @Override
    public void dispose() {
        if (session != null) {
            session.stop();
        }
        arena.dispose();
        for (Texture[][] t : spriteCache.values()) {
            for (Texture[] set : t) {
                for (Texture tex : set) {
                    tex.dispose();
                }
            }
        }
        batch.dispose();
        effects.dispose();
        story.dispose();
        title.dispose();
        lit.dispose();
        post.dispose();
        hud.dispose();
        flatNormal.dispose();
        pixel.dispose();
        glow.dispose();
        shaft.dispose();
    }
}
