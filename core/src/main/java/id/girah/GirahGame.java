package id.girah;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Titik masuk game. Masih kosong: nanti diganti menjadi {@code extends Game}
 * yang memegang layar-layar babak dan pertarungan.
 */
public class GirahGame extends ApplicationAdapter {

    /** Judul resmi game, sama dengan judul dokumen cerita. Dipakai jendela dan Babad. */
    public static final String JUDUL = "Calon Arang: Bayang-Bayang Girah";

    @Override
    public void render() {
        ScreenUtils.clear(0.08f, 0.05f, 0.1f, 1f);
    }
}
