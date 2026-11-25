package id.girah.proto;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HUD battle (brief C6), digambar di skala pixel 640x360 (setiap pixel UI = 2x2 pixel layar) setelah
 * efek kamera, jadi tetap tajam: kotak narasi, kartu status pengelana, nama + nyawa musuh, urutan
 * giliran, ronde, menu aksi melayang, penunjuk sasaran, dan angka kerusakan.
 *
 * <p>Font: monogram (datagoblin, CC0) ukuran 16 = tinggi huruf 7 px.
 */
public class Hud implements Disposable {

    static final int W = 640;
    static final int H = 360;
    /** Tinggi panel bawah; bidang main aman di atasnya (brief: y 130-300 dari atas). */
    static final int BOTTOM = 62;

    // palette colours (palette2.py)
    private static final Color TEXT = rgb("ece6d7");
    private static final Color MUTED = rgb("c9c1ad");
    private static final Color DIM = rgb("656b6b");
    private static final Color GOLD = rgb("dcc07a");
    private static final Color GOLD_D = rgb("b8954a");
    private static final Color HP = rgb("c2463d");
    private static final Color HP_D = rgb("642127");
    private static final Color EN = rgb("5f95b9");
    private static final Color EN_D = rgb("34405a");
    private static final Color BAR_BG = rgb("121019");
    private static final Color HEAL = rgb("74ad7e");
    private static final Color HIT = rgb("e8b25e");
    private static final Color SHADE = rgb("121019");

    public boolean visible = true;
    public boolean showHelp = false;
    private final ArenaPrototype game;
    private final BitmapFont font;
    private final BitmapFont outlined;
    /** Smaller outlined font for names over the fighters, drawn at screen resolution (1280x720). */
    private final BitmapFont small;
    private final Matrix4 screenMatrix = new Matrix4();
    private final GlyphLayout layout = new GlyphLayout();
    private final Matrix4 matrix = new Matrix4();
    private final Texture panelTex;
    private final Texture panelActiveTex;
    private final NinePatch panel;
    private final NinePatch panelActive;
    private final Texture cursor;
    private final Texture arrow;
    private final Texture white;
    private final Map<String, Texture> icons = new HashMap<>();

    public Hud(ArenaPrototype game) {
        this.game = game;
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/monogram-extended.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = 16;
        p.mono = true;
        p.minFilter = Texture.TextureFilter.Nearest;
        p.magFilter = Texture.TextureFilter.Nearest;
        p.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "…—–“”‘’";
        font = gen.generateFont(p);
        p.borderWidth = 1;
        p.borderColor = SHADE;
        p.borderStraight = true;
        outlined = gen.generateFont(p);
        p.size = 22;
        p.borderWidth = 2;
        small = gen.generateFont(p);
        gen.dispose();
        small.setUseIntegerPositions(true);
        font.setUseIntegerPositions(true);
        outlined.setUseIntegerPositions(true);

        panelTex = load("ui/panel.png");
        panelActiveTex = load("ui/panel_active.png");
        panel = new NinePatch(panelTex, 4, 4, 4, 4);
        panelActive = new NinePatch(panelActiveTex, 4, 4, 4, 4);
        cursor = load("ui/cursor.png");
        arrow = load("ui/arrow.png");
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        white = new Texture(pm);
        pm.dispose();
    }

    private static Texture load(String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return t;
    }

    private static Color rgb(String hex) {
        return Color.valueOf(hex);
    }

    private Texture icon(Actor25D a) {
        String key = a.sprite;
        if (!icons.containsKey(key)) {
            String path = "ui/ikon_" + key + ".png";
            icons.put(key, Gdx.files.internal(path).exists() ? load(path) : null);
        }
        return icons.get(key);
    }

    /** Dunia (640x360, y ke atas) -> koordinat UI (sama dengan layar dibagi 2). */
    private float ux(float wx) {
        return Math.round(game.toScreenX(wx) / 2f);
    }

    private float uy(float wy) {
        return Math.round(game.toScreenY(wy) / 2f);
    }

    public void draw(SpriteBatch batch, ArenaData arena, ArenaData.Mode mode, PostProcessor.Settings fx, CameraRig rig) {
        if (!visible) {
            return;
        }
        matrix.setToOrtho2D(0, 0, W, H);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);   // bloom may leave it additive
        batch.setShader(null);
        batch.setProjectionMatrix(matrix);
        batch.setColor(Color.WHITE);
        batch.begin();
        List<Actor25D> party = game.party();
        Actor25D active = game.activeActor();

        drawEnemyLabels(batch, arena);
        drawTop(batch, arena, mode, party, active);
        drawBottom(batch, party, active);
        drawMenu(batch, active);
        drawPopups(batch);
        drawThinking(batch);
        drawEnemyNames(batch, arena);
        drawResult(batch);
        if (showHelp) {
            drawHelp(batch, fx, rig);
        } else {
            outlined.setColor(DIM);
            layout.setText(outlined, "F12 bantuan");
            outlined.draw(batch, "F12 bantuan", W - layout.width - 6, H - 6);
        }
        batch.end();
        batch.setColor(Color.WHITE);
    }

    // ------------------------------------------------------------------ parts

    private void drawTop(SpriteBatch batch, ArenaData arena, ArenaData.Mode mode, List<Actor25D> party, Actor25D active) {
        // round + arena name, top left
        String place = arena.title + " - " + ("day".equals(mode.name) ? "siang" : "malam");
        layout.setText(font, place);
        float w = Math.max(layout.width, 70) + 14;
        panel.draw(batch, 4, H - 36, w, 32);
        font.setColor(GOLD);
        font.draw(batch, game.roundLabel(), 11, H - 10);
        font.setColor(MUTED);
        font.draw(batch, place, 11, H - 21);

        // turn order: the real order from the back end (both sides), or the party loop in pamer mode
        List<Actor25D> order = game.turnOrder();
        int n = order != null ? order.size() : party.size();
        if (n == 0) {
            return;
        }
        int start = order != null ? 0 : game.activeIndex();
        int shown = order != null ? Math.min(8, n) : 6;
        // start right after the title panel, so a long arena name never overlaps
        layout.setText(font, "Giliran");
        float labelX = 4 + w + 10;
        float x0 = labelX + layout.width + 6;
        font.setColor(MUTED);
        font.draw(batch, "Giliran", labelX, H - 10);
        for (int k = 0; k < shown; k++) {
            Actor25D a = order != null ? order.get(k) : party.get((start + k) % n);
            Texture t = icon(a);
            float x = x0 + k * 19;
            float y = H - 22 - (k == 0 ? 2 : 0);
            if (t != null) {
                batch.setColor(a.isDown() ? DIM : Color.WHITE);
                batch.draw(t, x, y, 16, 16);
                batch.setColor(Color.WHITE);
            }
            if (k == 0) {
                batch.draw(arrow, x + 4, y - 7);
            }
        }
    }

    private void drawBottom(SpriteBatch batch, List<Actor25D> party, Actor25D active) {
        // narration box
        panel.draw(batch, 4, 4, 232, BOTTOM - 8);
        font.setColor(TEXT);
        font.draw(batch, game.narration, 12, BOTTOM - 10, 216, Align.left, true);
        if (((int) (game.clock() * 2.5f)) % 2 == 0) {
            batch.draw(arrow, 222, 9);
        }

        // status cards
        float cw = 98;
        for (int i = 0; i < party.size() && i < 4; i++) {
            Actor25D a = party.get(i);
            boolean on = a == active;
            float x = 240 + i * (cw + 1);
            (on ? panelActive : panel).draw(batch, x, 4, cw, BOTTOM - 8);
            Texture t = icon(a);
            if (t != null) {
                batch.setColor(a.isDown() ? DIM : Color.WHITE);
                batch.draw(t, x + 6, BOTTOM - 26, 16, 16);
                batch.setColor(Color.WHITE);
            }
            font.setColor(a.isDown() ? DIM : on ? GOLD : TEXT);
            font.draw(batch, shortName(a.name), x + 26, BOTTOM - 12);
            font.setColor(MUTED);
            String hpText = a.nyawaMaks > 0 ? a.nyawa + "/" + a.nyawaMaks : Math.round(a.hp * 120) + "/120";
            font.draw(batch, hpText, x + 26, BOTTOM - 22);
            bar(batch, x + 7, 17, cw - 14, 4, a.hp, HP, HP_D);
            bar(batch, x + 7, 10, cw - 14, 3, a.energy, EN, EN_D);
        }
    }

    private static String shortName(String name) {
        return name.length() > 11 ? name.replace("Juru Mantra", "J. Mantra") : name;
    }

    private void drawEnemyLabels(SpriteBatch batch, ArenaData arena) {
        for (Actor25D e : arena.actors) {
            if (!e.isEnemy() || e.isDown()) {
                continue;
            }
            float cx = ux(e.footX());
            float y = Math.max(uy(e.footY()) - 4, BOTTOM + 14);
            bar(batch, cx - 16, y, 32, 3, e.hp, HP, HP_D);
        }
    }

    /** Names under the enemies' health bars, in the small font (screen pixels, so it can be smaller). */
    private void drawEnemyNames(SpriteBatch batch, ArenaData arena) {
        batch.end();
        screenMatrix.setToOrtho2D(0, 0, W * 2, H * 2);
        batch.setProjectionMatrix(screenMatrix);
        batch.begin();
        small.setColor(TEXT);
        for (Actor25D e : arena.actors) {
            if (!e.isEnemy() || e.isDown()) {
                continue;
            }
            float cx = ux(e.footX());
            float y = Math.max(uy(e.footY()) - 4, BOTTOM + 14);
            layout.setText(small, e.name);
            small.draw(batch, e.name, Math.round(cx * 2 - layout.width / 2f), Math.round(y * 2 - 6));
        }
        batch.end();
        batch.setProjectionMatrix(matrix);
        batch.begin();
    }

    private void drawMenu(SpriteBatch batch, Actor25D active) {
        // target pointer
        Actor25D target = game.menuTarget();
        if (target != null) {
            float bob = ((int) (game.clock() * 4)) % 2;
            float tx = ux(target.footX());
            float ty = uy(target.footY() + 50f) + bob;
            batch.draw(arrow, tx - 3, ty);
            outlined.setColor(GOLD);
            layout.setText(outlined, target.name);
            outlined.draw(batch, target.name, tx - layout.width / 2f, ty + 16);
        }
        if (!game.menuOpen() || active == null) {
            return;
        }
        String[] items = game.menuItems();
        float mw = 64;
        float mh = 10 + items.length * 11;
        // float the menu next to the active member, toward the open middle of the arena
        float ax = ux(active.footX());
        float ay = uy(active.footY() + 30f);
        float x = Math.max(6, Math.min(W - mw - 6, ax - mw - 18));
        float y = Math.max(BOTTOM + 4, Math.min(H - mh - 40, ay - mh / 2f));
        panelActive.draw(batch, x, y, mw, mh);
        for (int i = 0; i < items.length; i++) {
            boolean enabled = game.menuEnabled(i);
            boolean sel = i == game.menuIndex() && !game.choosingTarget();
            float ty = y + mh - 6 - i * 11;
            font.setColor(!enabled ? DIM : sel ? GOLD : TEXT);
            font.draw(batch, items[i], x + 12, ty);
            if (i == game.menuIndex()) {
                batch.draw(cursor, x + 5, ty - 7);
            }
        }
        // name tag over the active member
        outlined.setColor(GOLD);
        layout.setText(outlined, active.name);
        outlined.draw(batch, active.name, x + mw / 2f - layout.width / 2f, y + mh + 10);
    }

    private void drawPopups(SpriteBatch batch) {
        for (int i = 0; i < game.popups.size(); i++) {
            float[] p = game.popups.get(i);
            String s = game.popupText.get(i);
            float life = p[2];
            float rise = (1f - life) * 14f;
            float bounce = life > 0.8f ? (life - 0.8f) * 25f : 0f;     // small hop when it appears
            Color c = s.startsWith("+") ? HEAL : HIT;
            outlined.setColor(c.r, c.g, c.b, Math.min(1f, life * 2.5f));
            layout.setText(outlined, s);
            outlined.draw(batch, s, ux(p[0]) - layout.width / 2f, uy(p[1]) + rise + bounce);
        }
        outlined.setColor(Color.WHITE);
    }

    /** "..." di atas musuh yang sedang berpikir (panduan bagian 6). */
    private void drawThinking(SpriteBatch batch) {
        Actor25D a = game.thinkingActor();
        if (a == null) {
            return;
        }
        int dots = 1 + ((int) (game.clock() * 3f)) % 3;
        String s = "...".substring(0, dots);
        outlined.setColor(TEXT);
        layout.setText(outlined, "...");
        outlined.draw(batch, s, ux(a.footX()) - layout.width / 2f, uy(a.footY() + a.height() - 4f) + 12);
    }

    /** Panel hasil akhir pertarungan (panduan bagian 8). */
    private void drawResult(SpriteBatch batch) {
        String title = game.resultTitle();
        if (title == null) {
            return;
        }
        String babadText = game.babadMessage();
        float w = 200;
        if (babadText != null) {
            layout.setText(font, babadText);
            w = Math.max(w, layout.width + 24);
        }
        float h = 46;
        float x = (W - w) / 2f;
        float y = (H - h) / 2f + 20;
        panelActive.draw(batch, x, y, w, h);
        outlined.setColor(GOLD);
        layout.setText(outlined, title);
        outlined.draw(batch, title, (W - layout.width) / 2f, y + h - 8);
        String babad = game.babadMessage();
        font.setColor(MUTED);
        if (babad != null) {
            layout.setText(font, babad);
            font.draw(batch, babad, (W - layout.width) / 2f, y + h - 21);
        }
        font.setColor(TEXT);
        String hint = game.resultHint();
        layout.setText(font, hint);
        font.draw(batch, hint, (W - layout.width) / 2f, y + 12);
    }

    private void drawHelp(SpriteBatch batch, PostProcessor.Settings fx, CameraRig rig) {
        boolean real = game.session != null;
        String[] help = {
                "Panah: pilih   Enter/Z: pakai   Backspace/X: batal",
                real ? "Enter juga mempercepat narasi" : "SPASI serang cepat   J mantra   K jurus Pendekar",
                real ? "C contoh layar cerita" : "X Pendekar diserang   G gugur/bangkit   E pamer efek",
                "T siang/malam   1/2 arena   P screenshot   H HUD",
                "F1 normal map " + onOff(game.useNormals) + "   F2 blur " + onOff(fx.dof) + "   F3 bloom " + onOff(fx.bloom),
                "F4 kamera " + onOff(rig.enabled) + "   F5 grading " + onOff(fx.grade) + "   F6 bayangan " + onOff(game.useShadows),
                "FPS " + Gdx.graphics.getFramesPerSecond() + "   F12 tutup   ESC keluar",
        };
        float w = 236;
        float h = 10 + help.length * 10;
        panel.draw(batch, W - w - 4, H - h - 4, w, h);
        font.setColor(MUTED);
        for (int i = 0; i < help.length; i++) {
            font.draw(batch, help[i], W - w + 4, H - 10 - i * 10);
        }
    }

    private void bar(SpriteBatch batch, float x, float y, float w, float h, float frac, Color light, Color dark) {
        frac = Math.max(0f, Math.min(1f, frac));
        batch.setColor(BAR_BG);
        batch.draw(white, x - 1, y - 1, w + 2, h + 2);
        batch.setColor(dark);
        batch.draw(white, x, y, Math.round(w * frac), h);
        batch.setColor(light);
        batch.draw(white, x, y + 1, Math.round(w * frac), h - 1);
        batch.setColor(GOLD_D.r, GOLD_D.g, GOLD_D.b, 0.35f);
        batch.draw(white, x, y + h - 1, Math.round(w * frac), 1);
        batch.setColor(Color.WHITE);
    }

    private static String onOff(boolean b) {
        return b ? "ON" : "off";
    }

    @Override
    public void dispose() {
        font.dispose();
        outlined.dispose();
        small.dispose();
        panelTex.dispose();
        panelActiveTex.dispose();
        cursor.dispose();
        arrow.dispose();
        white.dispose();
        for (Texture t : icons.values()) {
            if (t != null) {
                t.dispose();
            }
        }
    }
}
