package id.girah.proto;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;

/**
 * Efek kamera setelah scene digambar:
 * <ol>
 *   <li>Scene digambar ke {@link #scene} (1280x720).</li>
 *   <li>Blur kedalaman: scene diperkecil ke 320x180 lalu diburamkan (dipakai di atas & bawah layar).</li>
 *   <li>Bloom: bagian terang diambil, diperkecil ke 640x360, lalu diburamkan.</li>
 *   <li>Composite: gabungkan + color grading + vignette ke layar.</li>
 * </ol>
 */
public class PostProcessor implements Disposable {

    /** Saklar efek, bisa diubah dengan tombol F1-F6. */
    public static class Settings {
        public boolean dof = true;
        public boolean bloom = true;
        public boolean grade = true;
        public float bloomAmount = 0.9f;
        public float threshold = 0.6f;
        public float sat = 0.85f;
        public float[] tint = {1f, 1f, 1f};
        public float[] lift = {0f, 0f, 0f};
        public float vignette = 0.5f;
    }

    public final FrameBuffer scene;
    private final FrameBuffer quarterA;
    private final FrameBuffer quarterB;
    private final FrameBuffer halfA;
    private final FrameBuffer halfB;
    private final ShaderProgram bright;
    private final ShaderProgram blur;
    private final ShaderProgram composite;
    private final Matrix4 matrix = new Matrix4();
    private final int width;
    private final int height;

    public PostProcessor(int width, int height) {
        this.width = width;
        this.height = height;
        scene = fbo(width, height);
        quarterA = fbo(width / 4, height / 4);
        quarterB = fbo(width / 4, height / 4);
        halfA = fbo(width / 2, height / 2);
        halfB = fbo(width / 2, height / 2);
        bright = Shaders.load("sprite.vert", "bright.frag");
        blur = Shaders.load("sprite.vert", "blur.frag");
        composite = Shaders.load("sprite.vert", "composite.frag");
    }

    private static FrameBuffer fbo(int w, int h) {
        FrameBuffer f = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
        f.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return f;
    }

    private void pass(SpriteBatch batch, Texture source, FrameBuffer target, ShaderProgram shader, Runnable uniforms) {
        target.begin();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        matrix.setToOrtho2D(0, 0, target.getWidth(), target.getHeight());
        batch.setProjectionMatrix(matrix);
        batch.setShader(shader);
        batch.disableBlending();
        batch.begin();
        if (uniforms != null) {
            uniforms.run();
        }
        batch.draw(source, 0, 0, target.getWidth(), target.getHeight(), 0, 0, 1, 1);
        batch.end();
        batch.enableBlending();
        target.end();
    }

    private void blurInto(SpriteBatch batch, FrameBuffer a, FrameBuffer b, float spread, int iterations) {
        for (int i = 0; i < iterations; i++) {
            float s = spread * (i + 1);
            pass(batch, a.getColorBufferTexture(), b, blur, () -> blur.setUniformf("u_dir", s / a.getWidth(), 0f));
            pass(batch, b.getColorBufferTexture(), a, blur, () -> blur.setUniformf("u_dir", 0f, s / b.getHeight()));
        }
    }

    /** Menjalankan semua efek dan menggambar hasilnya ke layar. */
    public void render(SpriteBatch batch, Settings s) {
        Texture sceneTex = scene.getColorBufferTexture();
        pass(batch, sceneTex, quarterA, null, null);
        blurInto(batch, quarterA, quarterB, 1.2f, 2);
        pass(batch, sceneTex, halfA, bright, () -> bright.setUniformf("u_threshold", s.threshold));
        blurInto(batch, halfA, halfB, 1.5f, 3);

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        matrix.setToOrtho2D(0, 0, width, height);
        batch.setProjectionMatrix(matrix);
        batch.setShader(composite);
        batch.disableBlending();
        batch.begin();
        quarterA.getColorBufferTexture().bind(1);
        halfA.getColorBufferTexture().bind(2);
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        composite.setUniformi("u_blur", 1);
        composite.setUniformi("u_bloom", 2);
        composite.setUniformf("u_screen", Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        composite.setUniformf("u_dof", s.dof ? 1f : 0f);
        composite.setUniformf("u_focus", 0.66f, 0.92f, 0.20f, 0.04f);
        composite.setUniformf("u_bloomAmt", s.bloom ? s.bloomAmount : 0f);
        composite.setUniformf("u_sat", s.sat);
        composite.setUniformf("u_tint", s.tint[0], s.tint[1], s.tint[2]);
        composite.setUniformf("u_lift", s.lift[0], s.lift[1], s.lift[2]);
        composite.setUniformf("u_vignette", s.vignette);
        composite.setUniformf("u_grade", s.grade ? 1f : 0f);
        batch.draw(sceneTex, 0, 0, width, height, 0, 0, 1, 1);
        batch.end();
        batch.enableBlending();
        batch.setShader(null);
    }

    @Override
    public void dispose() {
        scene.dispose();
        quarterA.dispose();
        quarterB.dispose();
        halfA.dispose();
        halfB.dispose();
        bright.dispose();
        blur.dispose();
        composite.dispose();
    }
}
