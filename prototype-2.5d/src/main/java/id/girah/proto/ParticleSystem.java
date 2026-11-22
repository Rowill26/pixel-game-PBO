package id.girah.proto;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

/**
 * Partikel sederhana yang digambar dengan blending additive: kunang-kunang, debu cahaya,
 * abu dupa, bara obor, dan percikan serangan.
 */
public class ParticleSystem {

    /** Satu partikel. */
    private static final class P {
        float x;
        float y;
        float vx;
        float vy;
        float life;
        float max;
        float size;
        float r;
        float g;
        float b;
        float wobble;
        boolean blink;
    }

    private final Array<P> items = new Array<>(false, 512);
    private float spawnTimer;

    public void clear() {
        items.clear();
    }

    private P add(float x, float y, float vx, float vy, float life, float size, float r, float g, float b) {
        if (items.size > 900) {
            return null;
        }
        P p = new P();
        p.x = x;
        p.y = y;
        p.vx = vx;
        p.vy = vy;
        p.life = life;
        p.max = life;
        p.size = size;
        p.r = r;
        p.g = g;
        p.b = b;
        p.wobble = MathUtils.random(0f, MathUtils.PI2);
        items.add(p);
        return p;
    }

    /** Partikel suasana sesuai jenis arena/waktu: fireflies, dust, atau ash. */
    public void ambient(String kind, float dt) {
        spawnTimer += dt;
        float interval = "fireflies".equals(kind) ? 0.12f : 0.06f;
        while (spawnTimer > interval) {
            spawnTimer -= interval;
            if ("fireflies".equals(kind)) {
                P p = add(MathUtils.random(-20f, 660f), MathUtils.random(60f, 230f), MathUtils.random(-6f, 6f),
                        MathUtils.random(-3f, 5f), MathUtils.random(4f, 8f), 1.5f, 0.85f, 0.95f, 0.5f);
                if (p != null) {
                    p.blink = true;
                }
            } else if ("dust".equals(kind)) {
                add(MathUtils.random(100f, 640f), MathUtils.random(80f, 360f), MathUtils.random(2f, 8f),
                        MathUtils.random(-6f, -2f), MathUtils.random(4f, 9f), 1f, 1f, 0.95f, 0.8f);
            } else if ("ash".equals(kind)) {
                add(MathUtils.random(-20f, 660f), MathUtils.random(40f, 200f), MathUtils.random(-4f, 4f),
                        MathUtils.random(6f, 14f), MathUtils.random(3f, 7f), 1f, 0.95f, 0.6f, 0.35f);
            }
        }
    }

    public void ember(float x, float y) {
        add(x + MathUtils.random(-2f, 2f), y, MathUtils.random(-4f, 4f), MathUtils.random(14f, 30f),
                MathUtils.random(0.6f, 1.3f), 1f, 1f, 0.7f, 0.3f);
    }

    /** Asap dupa: kepulan ungu samar yang naik pelan (asap wabah pengikut Calon Arang). */
    public void incense(float x, float y) {
        add(x + MathUtils.random(-1.5f, 1.5f), y, MathUtils.random(-3f, 3f), MathUtils.random(8f, 16f),
                MathUtils.random(1.2f, 2.4f), MathUtils.random(1f, 2f), 0.55f, 0.42f, 0.75f);
    }

    public void burst(float x, float y, int count, float r, float g, float b, float speed) {
        for (int i = 0; i < count; i++) {
            float a = MathUtils.random(0f, MathUtils.PI2);
            float s = MathUtils.random(speed * 0.3f, speed);
            add(x, y, MathUtils.cos(a) * s, MathUtils.sin(a) * s + 20f, MathUtils.random(0.25f, 0.6f),
                    MathUtils.random(1f, 2f), r, g, b);
        }
    }

    public void update(float dt) {
        for (int i = items.size - 1; i >= 0; i--) {
            P p = items.get(i);
            p.life -= dt;
            if (p.life <= 0) {
                items.removeIndex(i);
                continue;
            }
            p.wobble += dt * 2f;
            p.x += (p.vx + MathUtils.sin(p.wobble) * 3f) * dt;
            p.y += p.vy * dt;
        }
    }

    public void draw(SpriteBatch batch, Texture pixel) {
        for (P p : items) {
            float t = p.life / p.max;
            float a = Math.min(1f, t * 3f) * Math.min(1f, (1f - t) * 4f + 0.2f);
            if (p.blink) {
                a *= 0.5f + 0.5f * MathUtils.sin(p.wobble * 3f);
            }
            batch.setColor(p.r, p.g, p.b, a);
            batch.draw(pixel, p.x, p.y, p.size, p.size);
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }
}
