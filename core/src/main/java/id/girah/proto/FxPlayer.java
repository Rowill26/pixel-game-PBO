package id.girah.proto;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Efek serangan dan jurus (sprite animasi sekali jalan dari {@code assets/fx/<nama>_<i>.png},
 * dibuat dengan kode di {@code codixel-work/fx/}).
 *
 * <p>Setiap efek punya titik jangkar dalam pixel efek (x dari kiri, y dari atas): titik itulah yang
 * diletakkan di posisi dunia yang diminta, misalnya pusat lingkaran di tanah atau tengah sabetan.
 * Efek "bawah" digambar sebelum tokoh (lingkaran di tanah), sisanya di depan tokoh.
 */
public class FxPlayer implements Disposable {

    /** Data satu jenis efek. */
    private static final class Kind {
        final Texture[] frames;
        final float seconds;
        final int ax;
        final int ay;
        final boolean under;

        Kind(Texture[] frames, float seconds, int ax, int ay, boolean under) {
            this.frames = frames;
            this.seconds = seconds;
            this.ax = ax;
            this.ay = ay;
            this.under = under;
        }
    }

    /** Satu efek yang sedang diputar (atau menunggu jeda). */
    private static final class Play {
        final Kind kind;
        final float x;
        final float y;
        float t;

        Play(Kind kind, float x, float y, float delay) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.t = -delay;
        }
    }

    private final Map<String, Kind> kinds = new HashMap<>();
    private final List<Play> active = new ArrayList<>();

    public FxPlayer() {
        // name, seconds per frame, anchor x/y in effect pixels, drawn under the actors?
        add("sabetan", 0.045f, 24, 24, false);
        add("tebasan", 0.05f, 54, 60, false);
        add("langkah_bayangan", 0.05f, 32, 32, false);
        add("cakar", 0.045f, 32, 32, false);
        add("lingkar_suci", 0.07f, 100, 70, true);
        add("ramuan_penawar", 0.07f, 24, 58, false);
        add("doa_ratna", 0.08f, 24, 60, false);
        add("lingkar_perlindungan", 0.07f, 88, 100, false);
        add("asap_wabah", 0.07f, 150, 60, false);
        add("jerat_kabut", 0.07f, 32, 76, false);
        add("kutukan", 0.07f, 48, 98, false);
    }

    private void add(String name, float seconds, int ax, int ay, boolean under) {
        List<Texture> list = new ArrayList<>();
        for (int i = 0; Gdx.files.internal("fx/" + name + "_" + i + ".png").exists(); i++) {
            Texture t = new Texture(Gdx.files.internal("fx/" + name + "_" + i + ".png"));
            t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            list.add(t);
        }
        if (!list.isEmpty()) {
            kinds.put(name, new Kind(list.toArray(new Texture[0]), seconds, ax, ay, under));
        }
    }

    /**
     * Memutar efek dengan titik jangkarnya di posisi dunia (x, y), setelah jeda (detik).
     *
     * @return lama efek dalam detik (tanpa jeda), 0 jika efek tidak ada
     */
    public float play(String name, float x, float y, float delay) {
        Kind k = kinds.get(name);
        if (k == null) {
            return 0f;
        }
        active.add(new Play(k, x, y, delay));
        return k.frames.length * k.seconds;
    }

    public void update(float dt) {
        for (Iterator<Play> it = active.iterator(); it.hasNext(); ) {
            Play p = it.next();
            p.t += dt;
            if (p.t >= p.kind.frames.length * p.kind.seconds) {
                it.remove();
            }
        }
    }

    /** Menggambar efek lapisan bawah ({@code under = true}) atau lapisan depan. */
    public void draw(SpriteBatch batch, boolean under) {
        for (Play p : active) {
            if (p.t < 0 || p.kind.under != under) {
                continue;
            }
            int i = Math.min(p.kind.frames.length - 1, (int) (p.t / p.kind.seconds));
            Texture tex = p.kind.frames[i];
            // anchor is measured from the top of the effect image; the world y axis points up
            batch.draw(tex, Math.round(p.x - p.kind.ax), Math.round(p.y - (tex.getHeight() - p.kind.ay)));
        }
    }

    public void clear() {
        active.clear();
    }

    @Override
    public void dispose() {
        for (Kind k : kinds.values()) {
            for (Texture t : k.frames) {
                t.dispose();
            }
        }
    }
}
