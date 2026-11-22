package id.girah.proto;

/**
 * Lampu titik di dunia (koordinat dunia 640x360, y ke atas). Bisa berkedip (obor)
 * dan bisa sementara (kilatan serangan) dengan umur {@code ttl} detik.
 */
public class PointLight {

    public float x;
    public float y;
    public float z;
    public float r;
    public float g;
    public float b;
    public float radius;
    public float intensity;
    public float flicker;
    public float ttl = -1f;
    public float age;
    public float growPerSecond;
    private final int seed;

    public PointLight(float x, float y, float z, float r, float g, float b, float radius, float intensity, float flicker) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
        this.g = g;
        this.b = b;
        this.radius = radius;
        this.intensity = intensity;
        this.flicker = flicker;
        this.seed = (int) (x * 31 + y * 17);
    }

    public PointLight temporary(float seconds, float grow) {
        this.ttl = seconds;
        this.growPerSecond = grow;
        return this;
    }

    public void update(float dt) {
        if (ttl >= 0) {
            age += dt;
            radius += growPerSecond * dt;
        }
    }

    public boolean isExpired() {
        return ttl >= 0 && age >= ttl;
    }

    /** Intensitas saat ini, termasuk kedipan api dan memudar untuk lampu sementara. */
    public float currentIntensity(float time) {
        float k = intensity;
        if (flicker > 0) {
            double n = Math.sin(time * 13.0 + seed * 1.7) * 0.5 + Math.sin(time * 7.3 + seed * 3.1) * 0.3
                    + Math.sin(time * 23.0 + seed) * 0.2;
            k *= (float) (1.0 + flicker * n);
        }
        if (ttl > 0) {
            k *= Math.max(0f, 1f - age / ttl);
        }
        return k;
    }
}
