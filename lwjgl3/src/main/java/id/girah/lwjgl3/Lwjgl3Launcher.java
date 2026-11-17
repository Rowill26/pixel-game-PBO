package id.girah.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import id.girah.GirahGame;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle(GirahGame.JUDUL);
        config.setWindowedMode(1280, 720);
        config.useVsync(true);
        new Lwjgl3Application(new GirahGame(), config);
    }
}
