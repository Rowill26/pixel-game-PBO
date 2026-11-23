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
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Layar cerita (brief C7): diorama lokasi di belakang (sedikit digelapkan), potret setengah badan
 * besar di kiri, kotak dialog lebar dan miring yang menutupi tepi kanan potret, papan nama miring emas,
 * pilihan berupa panel miring bertumpuk, judul babak dan misi di atas.
 *
 * <p>Naskah dibaca dari {@code cerita/<nama>.json}. Potret dicari di {@code potret/<siapa>_<ekspresi>.png}
 * (128x192); bila belum ada, sementara dipakai sprite tokoh yang diperbesar 3x (bagian atas badan).
 * Bentuk panel dan warna adalah identitas game ini, bukan tiruan Persona.
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

    /** Satu baris naskah, atau satu titik pilihan. */
    private static final class Line {
        String who;
        String name;
        String face;
        String text;
        String[] choices;
        List<List<Line>> replies;
    }

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
    /** Arena loaded when the script ends ("then" in the script), e.g. the battle that follows. */
    private String then;
    private String mission = "";
    private final List<Line> queue = new ArrayList<>();
    private int index;
    private float typed;
    private int choice;
    private float portraitIn;

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

    public void start(String script) {
        JsonValue root = new JsonReader().parse(Gdx.files.internal("cerita/" + script + ".json"));
        // the scene's location: a story background in assets/arena/<key>/ (party standing in it)
        String place = root.getString("arena", null);
        if (place != null) {
            game.loadArena(place, root.getString("mode", null));
        }
        title = root.getString("title", "");
        then = root.getString("then", null);
        mission = root.getString("mission", "");
        queue.clear();
        queue.addAll(parse(root.get("lines")));
        index = 0;
        typed = 0f;
        portraitIn = 0f;
        active = !queue.isEmpty();
    }

    private static List<Line> parse(JsonValue list) {
        List<Line> out = new ArrayList<>();
        for (JsonValue v = list.child; v != null; v = v.next) {
            Line l = new Line();
            if (v.has("choice")) {
                l.choices = v.get("choice").asStringArray();
                l.replies = new ArrayList<>();
                for (JsonValue r = v.get("replies").child; r != null; r = r.next) {
                    l.replies.add(parse(r));
                }
            } else {
                l.who = v.getString("who", "narator");
                l.name = v.getString("name", "");
                l.face = v.getString("face", "netral");
                l.text = v.getString("text", "");
            }
            out.add(l);
        }
        return out;
    }

    private Line current() {
        return index < queue.size() ? queue.get(index) : null;
    }

    public void update(float dt) {
        if (!active) {
            return;
        }
        Line l = current();
        if (l == null) {
            active = false;
            return;
        }
        typed += dt * 40f;                 // letters per second
        portraitIn = Math.min(1f, portraitIn + dt * 6f);
        boolean ok = Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.Z)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
        if (l.choices != null) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                choice = (choice + l.choices.length - 1) % l.choices.length;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                choice = (choice + 1) % l.choices.length;
            }
            if (ok) {
                // the reply lines of the chosen option come right after the choice
                queue.addAll(index + 1, l.replies.get(choice));
                next();
            }
            return;
        }
        if (ok) {
            if (typed < l.text.length()) {
                typed = l.text.length();        // first press finishes the line
            } else {
                next();
            }
        }
    }

    /** Test hook: run the script to its end (as if every line was read), including "then". */
    public void finish() {
        while (active && current() != null && current().choices == null) {
            next();
        }
    }

    /** Test hook: skip to line n (for screenshots). */
    public void jump(int n) {
        while (index < n && current() != null && current().choices == null) {
            next();
        }
        typed = 9999f;
    }

    private void next() {
        Line before = current();
        index++;
        typed = 0f;
        choice = 0;
        Line after = current();
        if (after == null) {
            active = false;
            if (then != null) {
                game.loadArena(then, null);
            }
        } else if (before == null || before.who == null || after.who == null || !before.who.equals(after.who)) {
            portraitIn = 0f;                    // a new speaker slides in
        }
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
        if (!active || current() == null) {
            return;
        }
        Line l = current();
        // find the speaker shown: a choice keeps the last speaker on screen
        Line speaker = l;
        for (int k = index; k >= 0 && speaker.choices != null; k--) {
            speaker = queue.get(k);
        }
        boolean narrator = speaker.who == null || speaker.who.equals("narator");

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
            TextureRegion r = portrait(speaker.who, speaker.face);
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
        String name = narrator ? "" : speaker.name.isEmpty() ? speaker.who : speaker.name;
        float bx = narrator ? 40 : 118;
        float bw = W - bx - 30;
        float titleW = textW(font, title) + SLANT + 14;
        float missionW = textW(font, mission) + SLANT + 14;
        float missionX = W - missionW - SLANT - 8;
        float nameW = textW(big, name) + SLANT + 16;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if (!title.isEmpty()) {
            slantRect(6, H - 28, titleW + 4, 20, INK);
            slantRect(8, H - 26, titleW, 16, RED);
        }
        if (!mission.isEmpty()) {
            slantRect(missionX - 2, H - 28, missionW + 4, 20, INK);
            slantRect(missionX, H - 26, missionW, 16, PANEL);
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
        if (l.choices != null) {
            for (int i = 0; i < l.choices.length; i++) {
                float cy = 100 + (l.choices.length - 1 - i) * 22;
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
        if (!narrator) {
            big.setColor(INK);
            big.draw(batch, name, bx + 16 + SLANT / 2f + 8, centreY(big, 66, 20));
        }
        String text = speaker.text == null ? "" : speaker.text;
        int n = l.choices != null ? text.length() : Math.min(text.length(), (int) typed);
        font.setColor(narrator ? NARRATOR : TEXT);
        font.draw(batch, text.substring(0, n), bx + 20, 58, bw - 40, Align.left, true);
        if (l.choices == null && n >= text.length() && ((int) (game.clock() * 2.5f)) % 2 == 0) {
            batch.draw(arrow, bx + bw - 14, 12);
        }
        if (l.choices != null) {
            for (int i = 0; i < l.choices.length; i++) {
                float cy = 100 + (l.choices.length - 1 - i) * 22;
                boolean sel = i == choice;
                font.setColor(sel ? INK : TEXT);
                font.draw(batch, l.choices[i], W - 230 - (sel ? 8 : 0) + SLANT / 2f + 10, centreY(font, cy, 16));
            }
        }
        String hint = l.choices != null ? "Panah pilih, Enter" : "Enter lanjut";
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
