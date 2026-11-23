package id.girah.proto;

import com.badlogic.gdx.graphics.Texture;
import java.util.HashMap;
import java.util.Map;

/**
 * Satu tokoh di bidang main: sprite (satu gambar atau beberapa frame animasi) + normal map,
 * posisi kaki di dunia, dan keadaan animasi sederhana (melayang, kilatan saat terkena,
 * maju saat menyerang).
 */
public class Actor25D {

    /** Lama satu frame animasi idle (detik), sesuai brief. */
    public static final float IDLE_FRAME_SECONDS = 0.2f;

    public final String id;
    /** Nama file sprite (untuk ikon giliran di HUD). */
    public String sprite = "";
    public final String name;
    public final String side;
    private final Texture[] frames;
    private final Texture[] normalFrames;
    public final float x;
    public final float y;
    public final boolean floating;
    public final boolean boss;
    public final int[] eyes;
    public final int[] staff;
    public final float phase;
    /** Wujud berikutnya saat nyawa habis (Calon Arang: manusia -> ritual), atau {@code null}. */
    public Actor25D nextForm;
    /** Warna cahaya mata (r, g, b); bawaan ungu pucat seperti mata leak. */
    public float[] eyeColor = {0.7f, 0.6f, 1f};

    /** Frame yang sedang tampil. */
    public Texture texture;
    public Texture normals;

    public float offsetX;
    /** Geseran kedalaman saat maju ke sasaran di baris lain (y dunia, ke atas = menjauh). */
    public float offsetY;
    public float knock;
    public float flash;
    public float hp = 1f;
    public float energy = 1f;
    private float animTime;

    /** Animasi aksi sekali jalan (serang, jurus, terluka, gugur). */
    private static final class Clip {
        final Texture[] frames;
        final Texture[] normals;
        final float seconds;
        /** Lama tiap frame (detik); bawaannya semua sama dengan {@link #seconds}. */
        float[] durations;
        /** Frame benturan (saat kerusakan muncul), atau -1 jika tidak ditentukan. */
        int impact = -1;

        Clip(Texture[] frames, Texture[] normals, float seconds) {
            this.frames = frames;
            this.normals = normals;
            this.seconds = seconds;
            this.durations = new float[frames.length];
            java.util.Arrays.fill(durations, seconds);
        }

        float total() {
            float t = 0f;
            for (float d : durations) {
                t += d;
            }
            return t;
        }

        /** Indeks frame pada waktu {@code t}, atau {@code frames.length} jika sudah lewat. */
        int frameAt(float t) {
            for (int i = 0; i < durations.length; i++) {
                t -= durations[i];
                if (t < 0f) {
                    return i;
                }
            }
            return durations.length;
        }
    }

    private final Map<String, Clip> clips = new HashMap<>();
    /** Ujung tongkat per frame, jika tongkatnya ikut bergerak; nilai {@code null} = cahaya padam. */
    private final Map<Texture, int[]> staffByFrame = new HashMap<>();
    /** Titik asal asap dupa per frame (pedupaan pengikut); tidak ada di peta = tanpa asap. */
    private final Map<Texture, int[]> smokeByFrame = new HashMap<>();
    private Clip playing;
    private float playTime;
    private boolean holdLast;

    public Actor25D(String id, String name, String side, Texture[] frames, Texture[] normalFrames, float x, float y,
                    boolean floating, boolean boss, int[] eyes, int[] staff) {
        this.id = id;
        this.name = name;
        this.side = side;
        this.frames = frames;
        this.normalFrames = normalFrames;
        this.texture = frames[0];
        this.normals = normalFrames[0];
        this.x = x;
        this.y = y;
        this.floating = floating;
        this.boss = boss;
        this.eyes = eyes;
        this.staff = staff;
        this.phase = (float) (Math.abs(id.hashCode() % 628) / 100.0);
    }

    public void addAnimation(String name, Texture[] frames, Texture[] normals, float secondsPerFrame) {
        clips.put(name, new Clip(frames, normals, secondsPerFrame));
    }

    public void setStaffFrame(Texture frame, int[] tip) {
        staffByFrame.put(frame, tip);
    }

    /** Ujung tongkat pada frame yang sedang tampil, atau {@code null} jika tidak ada cahaya tongkat. */
    public int[] staffPoint() {
        return staffByFrame.containsKey(texture) ? staffByFrame.get(texture) : staff;
    }

    public void setSmokeFrame(Texture frame, int[] point) {
        smokeByFrame.put(frame, point);
    }

    /** Asal asap dupa pada frame yang sedang tampil (pixel sprite, y ke bawah), atau {@code null}. */
    public int[] smokePoint() {
        return smokeByFrame.get(texture);
    }

    /** {@code true} saat animasi jurus sedang diputar (asap lebih tebal). */
    public boolean isPlaying(String name) {
        return playing != null && playing == clips.get(name);
    }

    /** Waktu per frame dan frame benturan dari {@code <sprite>_<anim>_timing.json} (game feel). */
    public void setTiming(String name, float[] durations, int impact) {
        Clip c = clips.get(name);
        if (c == null) {
            return;
        }
        for (int i = 0; i < Math.min(durations.length, c.durations.length); i++) {
            c.durations[i] = durations[i];
        }
        c.impact = impact;
    }

    /** Detik sejak awal animasi sampai frame benturan, atau -1 jika tidak ditentukan. */
    public float impactTime(String name) {
        Clip c = clips.get(name);
        if (c == null || c.impact < 0) {
            return -1f;
        }
        float t = 0f;
        for (int i = 0; i < c.impact && i < c.durations.length; i++) {
            t += c.durations[i];
        }
        return t;
    }

    public boolean hasAnimation(String name) {
        return clips.containsKey(name);
    }

    /**
     * Memutar animasi aksi sekali, lalu kembali ke idle. Untuk "gugur", frame terakhir ditahan
     * sampai {@link #stopAnimation()} dipanggil.
     *
     * @return lama animasi dalam detik, atau 0 jika tokoh tidak punya animasi itu
     */
    public float play(String name) {
        Clip c = clips.get(name);
        if (c == null) {
            return 0f;
        }
        playing = c;
        playTime = 0f;
        holdLast = "gugur".equals(name);
        return c.total();
    }

    public void stopAnimation() {
        playing = null;
        holdLast = false;
    }

    /** {@code true} saat animasi aksi apa pun sedang diputar (mata menyala disembunyikan). */
    public boolean isActing() {
        return playing != null;
    }

    public boolean isDown() {
        return holdLast && playing != null;
    }

    public boolean isEnemy() {
        return "girah".equals(side);
    }

    public boolean isParty() {
        return "pengelana".equals(side);
    }

    /** {@code true} jika tokoh punya animasi idle sungguhan (lebih dari satu frame). */
    public boolean isAnimated() {
        return frames.length > 1;
    }

    /** Posisi kaki saat ini (termasuk maju/terdorong). */
    public float footX() {
        return x + offsetX + knock;
    }

    /**
     * Ketinggian gambar di atas tanah: tokoh melayang naik-turun; tokoh tanpa animasi
     * bernapas 1px; tokoh beranimasi sudah bernapas lewat frame-nya sendiri.
     */
    public float lift(float time) {
        if (floating) {
            return 8f + (float) Math.sin(time * 2.2 + phase) * 2.5f;
        }
        if (isAnimated()) {
            return 0f;
        }
        return Math.sin(time * 1.6 + phase) > 0.6 ? 0.5f : 0f;
    }

    public float width() {
        return texture.getWidth();
    }

    public float height() {
        return texture.getHeight();
    }

    /** Sudut kiri-bawah gambar di dunia. Kaki ada 3px di atas tepi bawah kanvas sprite. */
    public float drawX() {
        return footX() - width() / 2f;
    }

    /** Posisi kaki di kedalaman saat ini (termasuk maju ke baris sasaran). */
    public float footY() {
        return y + offsetY;
    }

    public float drawY(float time) {
        return footY() - 3f + lift(time);
    }

    public void update(float dt) {
        flash = Math.max(0f, flash - dt * 4f);
        knock *= (float) Math.pow(0.0005, dt);
        animTime += dt;
        if (playing != null) {
            playTime += dt;
            int k = playing.frameAt(playTime);
            if (k >= playing.frames.length) {
                if (holdLast) {
                    k = playing.frames.length - 1;
                } else {
                    playing = null;
                }
            }
            if (playing != null) {
                texture = playing.frames[k];
                normals = playing.normals[k];
                return;
            }
        }
        int i = (int) (animTime / IDLE_FRAME_SECONDS + phase * 3) % frames.length;
        texture = frames[i];
        normals = normalFrames[i];
    }
}
