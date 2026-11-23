package id.girah.proto;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import id.girah.GirahGame;

/**
 * Titik masuk prototipe arena 2.5D.
 *
 * <p>Argumen opsional:
 * <pre>
 *   --arena hutan|pemakaman   arena awal (bawaan: hutan)
 *   --mode day|night          waktu awal, jika arena punya keduanya
 *   --demo                    aksi serang/jurus berjalan otomatis
 *   --shot file.png           simpan screenshot di frame --frames lalu keluar
 *   --frames N                frame untuk screenshot (bawaan: 120)
 *   --attack-at N             picu serangan di frame N (untuk screenshot)
 *   --jurus-at N              picu jurus di frame N (untuk screenshot)
 * </pre>
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        PrototypeOptions options = PrototypeOptions.parse(args);
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle(GirahGame.JUDUL);
        config.setWindowedMode(ArenaPrototype.SCREEN_W, ArenaPrototype.SCREEN_H);
        config.setResizable(false);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new ArenaPrototype(options), config);
    }
}
