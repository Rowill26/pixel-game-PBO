package id.girah.proto;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import java.util.List;

/**
 * HUD sementara (digambar setelah efek kamera, jadi tetap tajam): kotak narasi, kartu status
 * pengelana, nama + nyawa musuh, angka kerusakan, dan daftar tombol/saklar efek.
 *
 * <p>Font masih font bawaan LibGDX; nanti diganti font pixel sesuai brief.
 */
public class Hud implements Disposable {

    private static final Color PANEL = new Color(0.07f, 0.06f, 0.10f, 0.86f);
    private static final Color GOLD = new Color(0.72f, 0.58f, 0.29f, 1f);
    private static final Color GOLD_LIGHT = new Color(0.86f, 0.75f, 0.48f, 1f);
    private static final Color TEXT = new Color(0.93f, 0.90f, 0.84f, 1f);
    private static final Color MUTED = new Color(0.79f, 0.76f, 0.68f, 1f);
    private static final Color HP = new Color(0.76f, 0.27f, 0.24f, 1f);
    private static final Color HP_DARK = new Color(0.39f, 0.13f, 0.15f, 1f);
    private static final Color EN = new Color(0.37f, 0.58f, 0.73f, 1f);
    private static final Color EN_DARK = new Color(0.20f, 0.25f, 0.35f, 1f);

    public boolean visible = true;
    private final ArenaPrototype game;
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();
    private final Matrix4 matrix = new Matrix4();

    public Hud(ArenaPrototype game) {
        this.game = game;
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    public void draw(SpriteBatch batch, ArenaData arena, ArenaData.Mode mode, PostProcessor.Settings fx, CameraRig rig) {
        if (!visible) {
            return;
        }
        matrix.setToOrtho2D(0, 0, ArenaPrototype.SCREEN_W, ArenaPrototype.SCREEN_H);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(matrix);
        List<Actor25D> party = game.party();
        int active = game.activeIndex();

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(PANEL);
        shapes.rect(0, 0, ArenaPrototype.SCREEN_W, 112);
        shapes.rect(10, 676, 420, 34);
        shapes.rect(860, 534, 410, 176);
        for (int i = 0; i < party.size() && i < 4; i++) {
            float x = 512 + i * 191;
            Actor25D a = party.get(i);
            bar(x + 12, 40, 160, 10, a.hp, HP, HP_DARK);
            bar(x + 12, 22, 160, 7, a.energy, EN, EN_DARK);
        }
        for (Actor25D e : arena.actors) {
            if (e.isEnemy()) {
                float sx = game.toScreenX(e.footX());
                float sy = Math.max(game.toScreenY(e.y) - 18f, 140f);   // jangan tertutup panel bawah
                bar(sx - 40, sy, 80, 6, e.hp, HP, HP_DARK);
            }
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(GOLD);
        shapes.line(0, 112, ArenaPrototype.SCREEN_W, 112);
        shapes.rect(8, 8, 492, 96);
        for (int i = 0; i < party.size() && i < 4; i++) {
            shapes.setColor(i == active ? GOLD_LIGHT : GOLD);
            shapes.rect(512 + i * 191, 8, 183, 96);
            if (i == active) {
                shapes.rect(513 + i * 191, 9, 181, 94);
            }
        }
        shapes.end();

        batch.setShader(null);
        batch.setProjectionMatrix(matrix);
        batch.begin();
        font.setColor(GOLD_LIGHT);
        font.draw(batch, arena.title + "  -  " + ("day".equals(mode.name) ? "siang" : "malam"), 20, 700);
        font.setColor(TEXT);
        font.draw(batch, game.narration, 22, 92, 466, Align.left, true);
        for (int i = 0; i < party.size() && i < 4; i++) {
            float x = 512 + i * 191;
            Actor25D a = party.get(i);
            font.setColor(i == active ? GOLD_LIGHT : TEXT);
            font.draw(batch, a.name, x + 12, 92);
            font.setColor(MUTED);
            font.draw(batch, "nyawa " + Math.round(a.hp * 100) + "%", x + 12, 70);
        }
        for (Actor25D e : arena.actors) {
            if (e.isEnemy()) {
                float sx = game.toScreenX(e.footX());
                float sy = Math.max(game.toScreenY(e.y) - 18f, 140f) - 4f;
                outlined(batch, e.name, sx, sy, TEXT);
            }
        }
        for (int i = 0; i < game.popups.size(); i++) {
            float[] p = game.popups.get(i);
            Color c = new Color(1f, 0.85f, 0.55f, Math.min(1f, p[2] * 2f));
            font.getData().setScale(1.6f);
            outlined(batch, game.popupText.get(i), game.toScreenX(p[0]), game.toScreenY(p[1]), c);
            font.getData().setScale(1f);
        }
        font.setColor(MUTED);
        String[] help = {
                "SPASI serang   J mantra   K jurus Pendekar   X Pendekar diserang",
                "G Pendekar gugur/bangkit   E pamer efek jurus   T siang/malam   1/2 arena",
                "F1 normal map: " + onOff(game.useNormals) + "     F2 blur kedalaman: " + onOff(fx.dof),
                "F3 bloom: " + onOff(fx.bloom) + "     F4 kamera+parallax: " + onOff(rig.enabled),
                "F5 grading+vignette: " + onOff(fx.grade) + "     F6 bayangan: " + onOff(game.useShadows),
                "P screenshot   H sembunyikan HUD   ESC keluar",
                "FPS " + Gdx.graphics.getFramesPerSecond(),
        };
        for (int i = 0; i < help.length; i++) {
            font.draw(batch, help[i], 872, 698 - i * 22);
        }
        batch.end();
        font.setColor(Color.WHITE);
    }

    private void outlined(SpriteBatch batch, String text, float cx, float y, Color color) {
        layout.setText(font, text);
        float x = cx - layout.width / 2f;
        font.setColor(0.05f, 0.04f, 0.07f, color.a);
        font.draw(batch, text, x - 1, y);
        font.draw(batch, text, x + 1, y);
        font.draw(batch, text, x, y - 1);
        font.draw(batch, text, x, y + 1);
        font.setColor(color);
        font.draw(batch, text, x, y);
    }

    private void bar(float x, float y, float w, float h, float frac, Color light, Color dark) {
        shapes.setColor(0.03f, 0.03f, 0.05f, 1f);
        shapes.rect(x - 1, y - 1, w + 2, h + 2);
        shapes.setColor(dark);
        shapes.rect(x, y, w * frac, h);
        shapes.setColor(light);
        shapes.rect(x, y + h / 2f, w * frac, h / 2f);
    }

    private static String onOff(boolean b) {
        return b ? "ON" : "off";
    }

    @Override
    public void dispose() {
        shapes.dispose();
        font.dispose();
    }
}
