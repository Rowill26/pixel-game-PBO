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
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;

/**
 * Layar judul: diorama Desa Girah saat senja (berlapis, kamera bergoyang pelan, abu dupa) di belakang,
 * judul besar dengan hiasan emas, dan menu Mulai / Lanjut / Pengaturan / Keluar dengan panel miring
 * yang sama dengan pilihan di layar cerita. Digambar di skala UI 640x360.
 */
public class TitleLayer implements Disposable {

    private static final int W = Hud.W;
    private static final int H = Hud.H;
    private static final float SLANT = 10f;

    private static final Color INK = Color.valueOf("121019");
    private static final Color PANEL = Color.valueOf("1e1b27");
    private static final Color GOLD = Color.valueOf("dcc07a");
    private static final Color GOLD_D = Color.valueOf("b8954a");
    private static final Color RED = Color.valueOf("8c2f2e");
    private static final Color TEXT = Color.valueOf("ece6d7");
    private static final Color MUTED = Color.valueOf("878d87");

    private static final String[] MAIN = {"Mulai", "Lanjut", "Pengaturan", "Keluar"};

    public boolean active;
    private final ArenaPrototype game;
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font;
    private final BitmapFont title;
    private final BitmapFont subtitle;
    private final GlyphLayout layout = new GlyphLayout();
    private final Matrix4 matrix = new Matrix4();
    private final Texture white;
    private int index;
    private boolean settings;
    private float t;
    /** Fade to black when a choice is made, then the action runs. */
    private float leaving = -1f;
    private Runnable after;

    public TitleLayer(ArenaPrototype game) {
        this.game = game;
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/monogram-extended.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.mono = true;
        p.minFilter = Texture.TextureFilter.Nearest;
        p.magFilter = Texture.TextureFilter.Nearest;
        p.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "…—–";
        p.size = 16;
        font = gen.generateFont(p);
        p.size = 64;                         // 4x the UI pixel size
        p.borderWidth = 4;
        p.borderColor = INK;
        p.borderStraight = true;
        p.shadowOffsetX = 0;
        p.shadowOffsetY = 4;
        p.shadowColor = new Color(0.07f, 0.06f, 0.1f, 0.85f);
        title = gen.generateFont(p);
        p.size = 32;
        p.borderWidth = 2;
        p.shadowOffsetY = 2;
        subtitle = gen.generateFont(p);
        gen.dispose();
        font.setUseIntegerPositions(true);
        title.setUseIntegerPositions(true);
        subtitle.setUseIntegerPositions(true);
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        white = new Texture(pm);
        pm.dispose();
    }

    public void open() {
        active = true;
        settings = false;
        index = 0;
        leaving = -1f;
        t = 0f;
    }

    private String[] items() {
        if (settings) {
            return new String[] {"Efek kamera: " + (game.cameraEffects() ? "ON" : "off"), "Kembali"};
        }
        return MAIN;
    }

    private boolean enabled(int i) {
        return settings || i != 1;           // Lanjut: there is no saved game yet
    }

    public void update(float dt) {
        if (!active) {
            return;
        }
        t += dt;
        if (leaving >= 0f) {
            leaving += dt;
            if (leaving > 0.6f) {
                active = false;
                after.run();
            }
            return;
        }
        String[] items = items();
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            do {
                index = (index + items.length - 1) % items.length;
            } while (!enabled(index));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            do {
                index = (index + 1) % items.length;
            } while (!enabled(index));
        }
        boolean ok = Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.Z);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            if (settings) {
                settings = false;
                index = 2;
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                Gdx.app.exit();
            }
        }
        if (ok) {
            choose();
        }
    }

    private void choose() {
        if (settings) {
            if (index == 0) {
                game.toggleCameraEffects();
            } else {
                settings = false;
                index = 2;
            }
            return;
        }
        switch (index) {
            case 0:
                leave(game::startNewGame);
                break;
            case 2:
                settings = true;
                index = 0;
                break;
            case 3:
                Gdx.app.exit();
                break;
            default:
                break;
        }
    }

    private void leave(Runnable r) {
        after = r;
        leaving = 0f;
    }

    // ------------------------------------------------------------------ drawing

    private void slantRect(float x, float y, float w, float h, Color c) {
        shapes.setColor(c);
        shapes.triangle(x + SLANT, y + h, x + w + SLANT, y + h, x, y);
        shapes.triangle(x, y, x + w + SLANT, y + h, x + w, y);
    }

    public void draw(SpriteBatch batch) {
        if (!active) {
            return;
        }
        matrix.setToOrtho2D(0, 0, W, H);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setShader(null);
        batch.setProjectionMatrix(matrix);
        shapes.setProjectionMatrix(matrix);
        float in = Math.min(1f, t / 1.2f);

        // darken top and bottom so the title and menu read over the diorama
        batch.begin();
        for (int y = 0; y < H; y += 2) {
            float d = y < 120 ? (1f - y / 120f) * 0.55f : y > 250 ? (y - 250) / 110f * 0.45f : 0f;
            if (d > 0f) {
                batch.setColor(INK.r, INK.g, INK.b, d);
                batch.draw(white, 0, y, W, 2);
            }
        }
        batch.setColor(Color.WHITE);
        batch.end();

        // title ornament: two gold lines with a diamond in the middle, under the main title
        float cx = W / 2f;
        float ty = H - 84;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        float lw = 150 * in;
        shapes.setColor(GOLD_D);
        shapes.rect(cx - 12 - lw, ty - 16, lw, 1);
        shapes.rect(cx + 12, ty - 16, lw, 1);
        shapes.setColor(GOLD);
        shapes.triangle(cx - 6, ty - 15.5f, cx, ty - 21.5f, cx + 6, ty - 15.5f);
        shapes.triangle(cx - 6, ty - 15.5f, cx, ty - 9.5f, cx + 6, ty - 15.5f);
        shapes.setColor(RED);
        shapes.rect(cx - 1, ty - 16.5f, 2, 2);

        // menu panels
        String[] items = items();
        float mx = cx - 70;
        float my = 116;
        for (int i = 0; i < items.length; i++) {
            float y = my - i * 22;
            boolean sel = i == index;
            float shift = sel ? 6 + MathUtils.sin(t * 4f) : 0;
            slantRect(mx - 2 + shift, y - 2, 144, 20, INK);
            slantRect(mx + shift, y, 140, 16, sel ? GOLD : PANEL);
        }
        shapes.end();

        batch.begin();
        // main title "CALON ARANG" and the subtitle, fading in
        String main = "CALON ARANG";
        title.setColor(GOLD.r, GOLD.g, GOLD.b, in);
        layout.setText(title, main);
        title.draw(batch, main, Math.round(cx - layout.width / 2f), Math.round(ty + 44));
        String sub = "Bayang-Bayang Girah";
        subtitle.setColor(TEXT.r, TEXT.g, TEXT.b, in);
        layout.setText(subtitle, sub);
        subtitle.draw(batch, sub, Math.round(cx - layout.width / 2f), Math.round(ty - 28));

        for (int i = 0; i < items.length; i++) {
            float y = my - i * 22;
            boolean sel = i == index;
            float shift = sel ? 6 + MathUtils.sin(t * 4f) : 0;
            font.setColor(!enabled(i) ? MUTED : sel ? INK : TEXT);
            layout.setText(font, items[i]);
            font.draw(batch, items[i], Math.round(mx + SLANT / 2f + 70 - layout.width / 2f + shift), y + 12);
        }
        font.setColor(MUTED);
        String hint = "Panah pilih   Enter pakai" + (settings ? "   Backspace kembali" : "   ESC keluar");
        layout.setText(font, hint);
        font.draw(batch, hint, Math.round(cx - layout.width / 2f), 14);
        font.draw(batch, "prototipe", 6, 14);

        // fade out to black when leaving
        float fade = leaving < 0 ? 0f : Math.min(1f, leaving / 0.6f);
        float fadeIn = Math.max(0f, 1f - t / 0.8f);
        float black = Math.max(fade, fadeIn);
        if (black > 0f) {
            batch.setColor(0f, 0f, 0f, black);
            batch.draw(white, 0, 0, W, H);
            batch.setColor(Color.WHITE);
        }
        batch.end();
    }

    @Override
    public void dispose() {
        shapes.dispose();
        font.dispose();
        title.dispose();
        subtitle.dispose();
        white.dispose();
    }
}
