package id.girah.proto;

import com.badlogic.gdx.math.MathUtils;

/**
 * Kamera "diorama": bergoyang pelan, mendekat ke titik fokus saat aksi, dan bergetar saat
 * serangan mengenai sasaran. Nilai camX/camY adalah pergeseran dari tengah layar.
 */
public class CameraRig {

    public float camX;
    public float camY;
    public float zoom = 1f;
    public boolean enabled = true;
    private float focusX;
    private float focusY;
    private float targetZoom = 1f;
    private float shake;

    public void focus(float worldX, float worldY, float zoomLevel) {
        focusX = MathUtils.clamp(worldX - ArenaData.VIEW_W / 2f, -30f, 30f);
        focusY = MathUtils.clamp(worldY - ArenaData.VIEW_H / 2f, -10f, 10f);
        targetZoom = zoomLevel;
    }

    public void release() {
        focusX = 0f;
        focusY = 0f;
        targetZoom = 1f;
    }

    public void shake(float amount) {
        shake = Math.max(shake, amount);
    }

    public void update(float dt, float time) {
        if (!enabled) {
            camX = 0f;
            camY = 0f;
            zoom = 1f;
            shake = 0f;
            return;
        }
        float k = 1f - (float) Math.pow(0.03, dt);
        float swayX = (float) Math.sin(time * 0.23) * 7f;
        float swayY = (float) Math.sin(time * 0.37) * 2.5f;
        camX += (focusX + swayX - camX) * k;
        camY += (focusY + swayY - camY) * k;
        float breathe = 1f + (float) Math.sin(time * 0.31) * 0.008f;
        zoom += (targetZoom * breathe - zoom) * k;
        shake = Math.max(0f, shake - dt * 18f);
    }

    public float shakeX() {
        return enabled ? MathUtils.random(-shake, shake) : 0f;
    }

    public float shakeY() {
        return enabled ? MathUtils.random(-shake, shake) * 0.6f : 0f;
    }
}
