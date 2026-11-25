package id.girah.proto;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import id.girah.cerita.BarisDialog;
import id.girah.cerita.Naskah;
import id.girah.cerita.NaskahTidakValidException;
import id.girah.cerita.NilaiCerita;
import id.girah.cerita.OpsiPilihan;
import id.girah.cerita.PembacaNaskah;
import id.girah.cerita.PemutarNaskah;
import id.girah.cerita.StatusCerita;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Layar cerita (brief C7): diorama lokasi di belakang (sedikit digelapkan), potret setengah badan
 * besar di kiri, kotak dialog lebar dan miring yang menutupi tepi kanan potret, papan nama miring emas,
 * pilihan berupa panel miring bertumpuk, judul babak dan misi di atas.
 *
 * <p>Naskah dibaca dan dijalankan oleh back end (paket {@code id.girah.cerita}, panduan integrasi
 * bagian 11): {@link PembacaNaskah} memeriksa format, {@link PemutarNaskah} memberi baris/pilihan
 * berikutnya dan melewati yang syaratnya tidak terpenuhi, dan efek pilihan dicatat di
 * {@link StatusCerita} milik permainan. Naskah ada di {@code cerita/<nama>.json}.
 *
 * <p>Potret dicari di {@code potret/<siapa>_<ekspresi>.png} (128x192); bila belum ada, sementara
 * dipakai sprite tokoh yang diperbesar 3x (bagian atas badan). Bentuk panel dan warna adalah
 * identitas game ini, bukan tiruan Persona.
 */
public class StoryLayer implements Disposable {

    private static final int W = Hud.W;
    private static final int H = Hud.H;
    private static final float SLANT = 12f;

    private static final Color INK = Color.valueOf("121019");
    private static final Color PANEL = Color.valueOf("1e1b27");
    private static final Color PANEL_2 = Color.valueOf("2c2836");
    private static final Color GOLD = Color.valueOf("dcc07a");
    private static final Color GOLD_D = Color.valueOf("b8954a");
    private static final Color RED = Color.valueOf("8c2f2e");
    private static final Color TEXT = Color.valueOf("ece6d7");
    private static final Color NARRATOR = Color.valueOf("c9c1ad");     // brief: narator tanpa potret, Putih 1
    private static final Color MUTED = Color.valueOf("878d87");

    public boolean active;
    private final ArenaPrototype game;
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font;
    private final BitmapFont big;
    private final GlyphLayout layout = new GlyphLayout();
    private final Matrix4 matrix = new Matrix4();
    private final Texture white;
    private final Texture arrow;
    private final Map<String, TextureRegion> portraits = new HashMap<>();
    private final List<Texture> owned = new ArrayList<>();

    private String title = "";
    private String mission = "";
    /** Naskah yang sedang berjalan (back end), atau {@code null}. */
    private PemutarNaskah player;
    /** Baris terakhir yang tampil: tetap di layar selama pilihan ditampilkan. */
    private BarisDialog speaker;
    /** Opsi pilihan yang sedang tampil, atau {@code null} saat menampilkan baris dialog. */
    private List<OpsiPilihan> options;
    private float typed;
    private int choice;
    private float portraitIn;
    /** Pemberitahuan singkat perubahan nilai cerita ("Kepercayaan Ratna +1") dan sisa waktunya. */
    private String toast = "";
    private float toastLeft;

    public StoryLayer(ArenaPrototype game) {
        this.game = game;
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/monogram-extended.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = 16;
        p.mono = true;
        p.minFilter = Texture.TextureFilter.Nearest;
        p.magFilter = Texture.TextureFilter.Nearest;
        p.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "…—–“”‘’";
        font = gen.generateFont(p);
        p.size = 32;                       // 2x the pixel size, still crisp, for the name plate
        big = gen.generateFont(p);
        gen.dispose();
        font.setUseIntegerPositions(true);
        big.setUseIntegerPositions(true);
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        white = new Texture(pm);
        pm.dispose();
        arrow = new Texture(Gdx.files.internal("ui/arrow.png"));
        arrow.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    // ------------------------------------------------------------------ script

    /** {@code true} jika {@code cerita/<nama>.json} ada (untuk "then" berupa naskah lanjutan). */
    public static boolean exists(String script) {
        return Gdx.files.internal("cerita/" + script + ".json").exists();
    }

    public void start(String script) {
        Naskah naskah;
        try {
            naskah = PembacaNaskah.baca(script, Gdx.files.internal("cerita/" + script + ".json").readString("UTF-8"));
        } catch (NaskahTidakValidException e) {
            Gdx.app.error("Cerita", e.getMessage());   // pesan menyebut file dan letak kesalahannya
            active = false;
            return;
        }
        // the scene's location: a story background in assets/arena/<key>/ (party standing in it)
        if (naskah.getArena() != null) {
            game.loadArena(naskah.getArena(), naskah.getMode());
        }
        title = naskah.getJudul() == null ? "" : naskah.getJudul();
        mission = naskah.getMisi() == null ? "" : naskah.getMisi();
        player = new PemutarNaskah(naskah, game.storyStatus());
        speaker = null;
        toast = "";
        toastLeft = 0f;
        active = true;
        Gdx.app.log("cerita", "mulai: " + script);
        show();
    }

    /** Menyiapkan langkah sekarang dari pemutar: baris dialog atau pilihan; selesai -> "then". */
    private void show() {
        typed = 0f;
        choice = 0;
        if (player.isSelesai()) {
            end();
            return;
        }
        if (player.isPilihan()) {
            options = player.getOpsiTersedia();
            return;
        }
        options = null;
        BarisDialog b = player.getBaris();
        if (speaker == null || !speaker.getIdPembicara().equals(b.getIdPembicara())) {
            portraitIn = 0f;                    // a new speaker slides in
        }
        speaker = b;
    }

    private void end() {
        active = false;
        options = null;
        String then = player.getLanjutan();
        Gdx.app.log("cerita", "selesai: " + player.getNaskah().getNama() + " | " + game.storyStatus()
                + (then != null ? " | lanjut: " + then : ""));
        if (then == null) {
            game.backToTitle();                 // akhir alur yang sudah ditulis
            return;
        }
        if (exists(then)) {
            start(then);                        // naskah berikutnya
        } else if (Gdx.files.internal("pertarungan/" + then + ".json").exists()) {
            game.startBattle(then);             // pertarungan dari assets/pertarungan/ (panduan 12)
        } else {
            game.loadArena(then, null);         // perilaku lama: lokasi tanpa pertarungan
        }
    }

    public void update(float dt) {
        toastLeft = Math.max(0f, toastLeft - dt);
        if (!active || player == null) {
            return;
        }
        typed += dt * 40f;                 // letters per second
        portraitIn = Math.min(1f, portraitIn + dt * 6f);
        boolean ok = Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.Z)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
        if (options != null) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                choice = (choice + options.size() - 1) % options.size();
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                choice = (choice + 1) % options.size();
            }
            if (ok) {
                choose(choice);
            }
            return;
        }
        if (ok) {
            if (typed < speaker.getTeks().length()) {
                typed = speaker.getTeks().length();        // first press finishes the line
            } else {
                player.lanjut();
                show();
            }
        }
    }

    /** Memilih opsi ke-{@code i}: back end mencatat efeknya; perubahan nilai ditampilkan sebentar. */
    void choose(int i) {
        if (options == null || i < 0 || i >= options.size()) {
            return;
        }
        StatusCerita status = game.storyStatus();
        Map<NilaiCerita, Integer> before = new EnumMap<>(NilaiCerita.class);
        for (NilaiCerita n : NilaiCerita.values()) {
            before.put(n, status.getNilai(n));
        }
        Gdx.app.log("cerita", "pilih: " + options.get(i).getTeks());
        player.pilih(i);
        List<String> changes = new ArrayList<>();
        for (NilaiCerita n : NilaiCerita.values()) {
            int d = status.getNilai(n) - before.get(n);
            if (d != 0) {
                changes.add(label(n) + (d > 0 ? " +" : " ") + d);
            }
        }
        if (!changes.isEmpty()) {
            toast = String.join("   ", changes);
            toastLeft = 3f;
        }
        Gdx.app.log("cerita", "status: " + status);
        show();
    }

    private static String label(NilaiCerita n) {
        switch (n) {
            case KEPERCAYAAN_RATNA:
                return "Kepercayaan Ratna";
            case KEPERCAYAAN_GIRAH:
                return "Kepercayaan warga Girah";
            case KEPERCAYAAN_TAPAK_WANGI:
                return "Kepercayaan warga Tapak Wangi";
            default:
                return "Warga selamat";
        }
    }

    /** Test hook: run the script up to the next choice or its end (as if every line was read), including "then". */
    public void finish() {
        while (active && player != null && !player.isSelesai() && !player.isPilihan()) {
            player.lanjut();
            show();
        }
    }

    /** Test hook: skip n lines (for screenshots). */
    public void jump(int n) {
        for (int k = 0; k < n && active && options == null && !player.isSelesai(); k++) {
            player.lanjut();
            show();
        }
        typed = 9999f;
    }

    /** Test hook: {@code true} while a choice is on screen. */
    boolean choosing() {
        return active && options != null;
    }

    // ------------------------------------------------------------------ drawing

    private TextureRegion portrait(String who, String face) {
        String key = who + "_" + face;
        if (portraits.containsKey(key)) {
            return portraits.get(key);
        }
        TextureRegion r = null;
        String[] tries = {"potret/" + key + ".png", "potret/" + who + "_netral.png"};
        for (String t : tries) {
            if (Gdx.files.internal(t).exists()) {
                Texture tex = new Texture(Gdx.files.internal(t));
                tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                owned.add(tex);
                r = new TextureRegion(tex);
                break;
            }
        }
        if (r == null) {
            // placeholder until the real portrait exists: the upper body of the battle sprite, mirrored
            // to face right (party sprites face left), drawn 3x
            String f = Gdx.files.internal("sprites/" + who + "_idle_0.png").exists() ? "sprites/" + who + "_idle_0.png"
                    : "sprites/" + who + ".png";
            if (Gdx.files.internal(f).exists()) {
                Texture tex = new Texture(Gdx.files.internal(f));
                tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                owned.add(tex);
                int top = tex.getHeight() - 64 + 6;
                r = new TextureRegion(tex, tex.getWidth() / 2 - 22, Math.max(0, top), 44, 40);
                boolean party = !who.equals("calon_arang") && !who.startsWith("pengikut") && !who.equals("leak");
                if (party) {
                    r.flip(true, false);
                }
            }
        }
        portraits.put(key, r);
        return r;
    }

    private void slantRect(float x, float y, float w, float h, Color c) {
        shapes.setColor(c);
        shapes.triangle(x + SLANT, y + h, x + w + SLANT, y + h, x, y);
        shapes.triangle(x, y, x + w + SLANT, y + h, x + w, y);
    }

    private void slantFrame(float x, float y, float w, float h, Color c) {
        shapes.setColor(c);
        shapes.line(x + SLANT, y + h, x + w + SLANT, y + h);
        shapes.line(x, y, x + w, y);
        shapes.line(x, y, x + SLANT, y + h);
        shapes.line(x + w, y, x + w + SLANT, y + h);
    }

    public void draw(SpriteBatch batch) {
        if (!active || player == null) {
            return;
        }
        // a choice keeps the last speaker on screen; a choice as the very first step shows no speaker
        boolean narrator = speaker == null || speaker.isNarator();

        matrix.setToOrtho2D(0, 0, W, H);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);   // bloom may leave it additive
        batch.setShader(null);
        batch.setProjectionMatrix(matrix);
        shapes.setProjectionMatrix(matrix);

        // dim the diorama so the text reads (brief: digelapkan)
        batch.begin();
        batch.setColor(INK.r, INK.g, INK.b, 0.32f);
        batch.draw(white, 0, 0, W, H);
        batch.setColor(Color.WHITE);
        // portrait (left, bottom anchored, slides in)
        if (!narrator) {
            TextureRegion r = portrait(speaker.getIdPembicara(), speaker.getEkspresi());
            if (r != null) {
                float pw = r.getRegionWidth() == 128 ? 128 : r.getRegionWidth() * 3;
                float ph = r.getRegionHeight() == 192 ? 192 : r.getRegionHeight() * 3;
                float slide = (1f - portraitIn) * -40f;
                batch.setColor(1f, 1f, 1f, portraitIn);
                batch.draw(r, 6 + slide, 0, pw, ph);
                batch.setColor(Color.WHITE);
            }
        }
        batch.end();

        // panels. Every label panel is sized from its text: the text starts half a slant in from the
        // lower-left corner and is centred vertically on the panel's cap height.
        String name = narrator ? "" : speaker.getNama() == null || speaker.getNama().isEmpty()
                ? speaker.getIdPembicara() : speaker.getNama();
        float bx = narrator ? 40 : 118;
        float bw = W - bx - 30;
        float titleW = textW(font, title) + SLANT + 14;
        float missionW = textW(font, mission) + SLANT + 14;
        float missionX = W - missionW - SLANT - 8;
        float nameW = textW(big, name) + SLANT + 16;
        float toastW = toastLeft > 0f ? textW(font, toast) + SLANT + 14 : 0f;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if (!title.isEmpty()) {
            slantRect(6, H - 28, titleW + 4, 20, INK);
            slantRect(8, H - 26, titleW, 16, RED);
        }
        if (!mission.isEmpty()) {
            slantRect(missionX - 2, H - 28, missionW + 4, 20, INK);
            slantRect(missionX, H - 26, missionW, 16, PANEL);
        }
        if (toastLeft > 0f) {
            slantRect(W - toastW - SLANT - 10, H - 50, toastW + 4, 20, INK);
            slantRect(W - toastW - SLANT - 8, H - 48, toastW, 16, PANEL_2);
        }
        // dialog box: wide, slanted, covering the right edge of the portrait
        slantRect(bx - 3, 3, bw + 6, 74, INK);
        slantRect(bx, 6, bw, 68, PANEL);
        slantRect(bx + 4, 6, bw - 8, 3, PANEL_2);
        shapes.end();
        // the box frame goes UNDER the name plate, so its top line never crosses the name
        shapes.begin(ShapeRenderer.ShapeType.Line);
        slantFrame(bx, 6, bw, 68, GOLD_D);
        if (!mission.isEmpty()) {
            slantFrame(missionX, H - 26, missionW, 16, GOLD_D);
        }
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if (!narrator) {
            slantRect(bx + 14, 64, nameW + 4, 24, INK);
            slantRect(bx + 16, 66, nameW, 20, GOLD);
        }
        if (options != null) {
            for (int i = 0; i < options.size(); i++) {
                float cy = 100 + (options.size() - 1 - i) * 22;
                boolean sel = i == choice;
                slantRect(W - 232 - (sel ? 8 : 0), cy - 2, 214, 20, INK);
                slantRect(W - 230 - (sel ? 8 : 0), cy, 210, 16, sel ? GOLD : PANEL);
            }
        }
        shapes.end();

        batch.begin();
        if (!title.isEmpty()) {
            font.setColor(TEXT);
            font.draw(batch, title, 8 + SLANT / 2f + 7, centreY(font, H - 26, 16));
        }
        if (!mission.isEmpty()) {
            font.setColor(GOLD);
            font.draw(batch, mission, missionX + SLANT / 2f + 7, centreY(font, H - 26, 16));
        }
        if (toastLeft > 0f) {
            font.setColor(GOLD);
            font.draw(batch, toast, W - toastW - SLANT - 8 + SLANT / 2f + 7, centreY(font, H - 48, 16));
        }
        if (!narrator) {
            big.setColor(INK);
            big.draw(batch, name, bx + 16 + SLANT / 2f + 8, centreY(big, 66, 20));
        }
        String text = speaker == null ? "" : speaker.getTeks();
        int n = options != null ? text.length() : Math.min(text.length(), (int) typed);
        font.setColor(narrator ? NARRATOR : TEXT);
        font.draw(batch, text.substring(0, n), bx + 20, 58, bw - 40, Align.left, true);
        if (options == null && n >= text.length() && ((int) (game.clock() * 2.5f)) % 2 == 0) {
            batch.draw(arrow, bx + bw - 14, 12);
        }
        if (options != null) {
            for (int i = 0; i < options.size(); i++) {
                float cy = 100 + (options.size() - 1 - i) * 22;
                boolean sel = i == choice;
                font.setColor(sel ? INK : TEXT);
                font.draw(batch, options.get(i).getTeks(), W - 230 - (sel ? 8 : 0) + SLANT / 2f + 10,
                        centreY(font, cy, 16));
            }
        }
        String hint = options != null ? "Panah pilih, Enter" : "Enter lanjut";
        font.setColor(MUTED);
        layout.setText(font, hint);
        font.draw(batch, hint, W - layout.width - 34, 92);
        batch.end();
        batch.setColor(Color.WHITE);
    }

    private float textW(BitmapFont f, String text) {
        if (text == null || text.isEmpty()) {
            return 0f;
        }
        layout.setText(f, text);
        return layout.width;
    }

    /** y to pass to font.draw so the capitals sit in the middle of a panel from y to y + h. */
    private static float centreY(BitmapFont f, float y, float h) {
        return Math.round(y + (h + f.getCapHeight()) / 2f);
    }

    @Override
    public void dispose() {
        shapes.dispose();
        font.dispose();
        big.dispose();
        white.dispose();
        arrow.dispose();
        for (Texture t : owned) {
            t.dispose();
        }
    }
}
