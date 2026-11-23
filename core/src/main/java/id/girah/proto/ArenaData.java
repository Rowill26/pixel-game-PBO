package id.girah.proto;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Isi satu arena, dibaca dari {@code arena/<nama>/arena.json}: lapisan parallax, tokoh,
 * obor, lampu tetap, dan pengaturan per waktu (siang/malam).
 *
 * <p>Koordinat di JSON memakai sistem gambar 640x360 (y ke bawah). Di sini diubah ke
 * koordinat dunia LibGDX (y ke atas).
 */
public class ArenaData implements Disposable {

    public static final float VIEW_W = 640f;
    public static final float VIEW_H = 360f;
    /** Kanvas lapisan 720x380 = layar 640x360 + kelonggaran untuk parallax. */
    public static final float LAYER_X = -40f;
    public static final float LAYER_Y = -10f;

    /** Satu lapisan latar. */
    public static class Layer {
        public Texture texture;
        public Texture normals;
        public float parallax;
        public float lit;
    }

    /** Pengaturan cahaya dan warna untuk satu waktu. */
    public static class Mode {
        public String name;
        public Texture sky;
        public float[] ambient;
        public float[] sun;
        public float[] sunDir;
        public boolean torches;
        public boolean shafts;
        public String particles;
        public float sat;
        public float[] tint;
        public float[] lift;
        public float vignette;
    }

    public final String key;
    public final String title;
    public final List<Layer> layers = new ArrayList<>();
    public final Layer front;
    public final Map<String, Mode> modes = new LinkedHashMap<>();
    public final String startMode;
    public final List<float[]> torches = new ArrayList<>();
    public final List<PointLight> staticLights = new ArrayList<>();
    public final List<Actor25D> actors = new ArrayList<>();
    private final Array<Texture> owned = new Array<>();

    public ArenaData(String key, Map<String, Texture[][]> spriteCache) {
        this.key = key;
        FileHandle dir = Gdx.files.internal("arena/" + key);
        JsonValue root = new JsonReader().parse(dir.child("arena.json"));
        this.title = root.getString("name");
        this.startMode = root.getString("startMode");

        for (JsonValue m = root.get("modes").child; m != null; m = m.next) {
            Mode mode = new Mode();
            mode.name = m.name;
            mode.sky = load(dir.child(m.getString("sky")));
            mode.ambient = m.get("ambient").asFloatArray();
            mode.sun = m.get("sun").asFloatArray();
            mode.sunDir = m.get("sunDir").asFloatArray();
            mode.torches = m.getBoolean("torches");
            mode.shafts = m.getBoolean("shafts");
            mode.particles = m.getString("particles");
            JsonValue g = m.get("grade");
            mode.sat = g.getFloat("sat");
            mode.tint = g.get("tint").asFloatArray();
            mode.lift = g.get("lift").asFloatArray();
            mode.vignette = g.getFloat("vignette");
            modes.put(mode.name, mode);
        }
        for (JsonValue l = root.get("layers").child; l != null; l = l.next) {
            layers.add(layer(dir, l));
        }
        front = layer(dir, root.get("front"));
        for (JsonValue t = root.get("torches").child; t != null; t = t.next) {
            torches.add(new float[] {t.getFloat("x"), VIEW_H - t.getFloat("y")});
        }
        JsonValue sl = root.get("staticLights");
        if (sl != null) {
            for (JsonValue l = sl.child; l != null; l = l.next) {
                float[] c = l.get("color").asFloatArray();
                staticLights.add(new PointLight(l.getFloat("x"), VIEW_H - l.getFloat("y"), l.getFloat("z"),
                        c[0], c[1], c[2], l.getFloat("radius"), l.getFloat("intensity"), l.getFloat("flicker")));
            }
        }
        for (JsonValue a = root.get("actors").child; a != null; a = a.next) {
            Actor25D actor = buildActor(a, a, spriteCache);
            // optional "nextForm": the boss changes into this form when its HP runs out (Calon Arang fase 2)
            JsonValue next = a.get("nextForm");
            if (next != null) {
                actor.nextForm = buildActor(next, a, spriteCache);
            }
            actors.add(actor);
        }
    }

    /**
     * Membuat satu tokoh. {@code a} memberi sprite, nama, mata, tongkat; {@code base} memberi posisi,
     * sisi, dan id (sama dengan {@code a} kecuali untuk wujud berikutnya).
     */
    private static Actor25D buildActor(JsonValue a, JsonValue base, Map<String, Texture[][]> spriteCache) {
        String sprite = a.getString("sprite");
        Texture[][] tex = spriteCache.computeIfAbsent(sprite, ArenaData::loadSprite);
        JsonValue eyes = a.get("eyes");
        JsonValue staff = a.get("staff");
        Actor25D actor = new Actor25D(base.getString("id"), a.getString("name", base.getString("name")),
                base.getString("side"), tex[0], tex[1], base.getFloat("x"), VIEW_H - base.getFloat("y"),
                a.getBoolean("float", false), a.getBoolean("boss", base.getBoolean("boss", false)),
                eyes == null ? null : eyes.asIntArray(), staff == null ? null : staff.asIntArray());
        actor.sprite = sprite;
        JsonValue eyeColor = a.get("eyeColor");
        if (eyeColor != null) {
            actor.eyeColor = eyeColor.asFloatArray();
        }
        // optional sprites/<sprite>_staff.json: staff tip per frame ({"idle": [[x, y], null, ...], ...})
        FileHandle staffFile = Gdx.files.internal("sprites/" + sprite + "_staff.json");
        JsonValue staffFrames = staffFile.exists() ? new JsonReader().parse(staffFile) : null;
        setStaffFrames(actor, staffFrames, "idle", tex[0]);
        // optional sprites/<sprite>_smoke.json: where incense smoke rises from, per frame (same format)
        FileHandle smokeFile = Gdx.files.internal("sprites/" + sprite + "_smoke.json");
        JsonValue smokeFrames = smokeFile.exists() ? new JsonReader().parse(smokeFile) : null;
        setSmokeFrames(actor, smokeFrames, "idle", tex[0]);
        for (Map.Entry<String, Float> anim : ACTION_SECONDS.entrySet()) {
            Texture[][] frames = spriteCache.computeIfAbsent(sprite + "#" + anim.getKey(),
                    k -> loadFrames(sprite + "_" + anim.getKey()));
            if (frames != null) {
                actor.addAnimation(anim.getKey(), frames[0], frames[1], anim.getValue());
                FileHandle timing = Gdx.files.internal("sprites/" + sprite + "_" + anim.getKey() + "_timing.json");
                if (timing.exists()) {
                    JsonValue tj = new JsonReader().parse(timing);
                    actor.setTiming(anim.getKey(), tj.get("durations").asFloatArray(), tj.getInt("impact", -1));
                }
                setStaffFrames(actor, staffFrames, anim.getKey(), frames[0]);
                setSmokeFrames(actor, smokeFrames, anim.getKey(), frames[0]);
            }
        }
        return actor;
    }

    /** Posisi ujung tongkat per frame (koordinat pixel sprite, y ke bawah); {@code null} = cahaya padam. */
    private static void setStaffFrames(Actor25D actor, JsonValue staffFrames, String anim, Texture[] frames) {
        JsonValue list = staffFrames == null ? null : staffFrames.get(anim);
        if (list == null) {
            return;
        }
        int i = 0;
        for (JsonValue p = list.child; p != null && i < frames.length; p = p.next, i++) {
            actor.setStaffFrame(frames[i], p.isNull() ? null : p.asIntArray());
        }
    }

    private static void setSmokeFrames(Actor25D actor, JsonValue smokeFrames, String anim, Texture[] frames) {
        JsonValue list = smokeFrames == null ? null : smokeFrames.get(anim);
        if (list == null) {
            return;
        }
        int i = 0;
        for (JsonValue p = list.child; p != null && i < frames.length; p = p.next, i++) {
            actor.setSmokeFrame(frames[i], p.isNull() ? null : p.asIntArray());
        }
    }

    private Layer layer(FileHandle dir, JsonValue l) {
        Layer layer = new Layer();
        String file = l.getString("file");
        layer.texture = load(dir.child(file));
        layer.parallax = l.getFloat("parallax");
        layer.lit = l.getFloat("lit", 1f);
        if (l.getBoolean("normal", false)) {
            layer.normals = load(dir.child(file.replace(".png", "_n.png")));
        }
        return layer;
    }

    private Texture load(FileHandle file) {
        Texture t = new Texture(file);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        owned.add(t);
        return t;
    }

    /**
     * Memuat sprite tokoh. Jika ada {@code <nama>_idle_0.png}, {@code _idle_1.png}, ... maka
     * semua frame itu dipakai sebagai animasi idle; jika tidak, dipakai {@code <nama>.png}.
     * Setiap frame butuh normal map dengan akhiran {@code _n.png}.
     *
     * @return {@code [0]} = frame gambar, {@code [1]} = frame normal map
     */
    private static Texture[][] loadSprite(String name) {
        Texture[][] idle = loadFrames(name + "_idle");
        return idle != null ? idle : loadFiles(List.of("sprites/" + name));
    }

    /** Animasi aksi yang dicari untuk setiap tokoh, dengan lama satu frame (detik), sesuai brief. */
    private static final Map<String, Float> ACTION_SECONDS = new LinkedHashMap<>();

    static {
        ACTION_SECONDS.put("serang", 0.09f);
        ACTION_SECONDS.put("jurus", 0.10f);
        ACTION_SECONDS.put("terluka", 0.10f);
        ACTION_SECONDS.put("gugur", 0.14f);
    }

    /** Memuat {@code <prefix>_0.png}, {@code _1.png}, ... atau {@code null} jika tidak ada. */
    private static Texture[][] loadFrames(String prefix) {
        List<String> files = new ArrayList<>();
        for (int i = 0; Gdx.files.internal("sprites/" + prefix + "_" + i + ".png").exists(); i++) {
            files.add("sprites/" + prefix + "_" + i);
        }
        return files.isEmpty() ? null : loadFiles(files);
    }

    private static Texture[][] loadFiles(List<String> files) {
        Texture[] diffuse = new Texture[files.size()];
        Texture[] normal = new Texture[files.size()];
        for (int i = 0; i < files.size(); i++) {
            diffuse[i] = loadShared(files.get(i) + ".png");
            normal[i] = loadShared(files.get(i) + "_n.png");
        }
        return new Texture[][] {diffuse, normal};
    }

    private static Texture loadShared(String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return t;
    }

    public Mode mode(String name) {
        Mode m = name == null ? null : modes.get(name);
        return m != null ? m : modes.get(startMode);
    }

    @Override
    public void dispose() {
        for (Texture t : owned) {
            t.dispose();
        }
    }
}
