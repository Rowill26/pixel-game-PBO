package id.girah.proto;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;

/** Memuat shader dari folder {@code shaders/} dan langsung gagal dengan pesan jelas jika error. */
public final class Shaders {

    private Shaders() {
    }

    public static ShaderProgram load(String vert, String frag) {
        ShaderProgram.pedantic = false;
        ShaderProgram p = new ShaderProgram(Gdx.files.internal("shaders/" + vert), Gdx.files.internal("shaders/" + frag));
        if (!p.isCompiled()) {
            throw new GdxRuntimeException("Shader " + frag + " gagal dikompilasi:\n" + p.getLog());
        }
        return p;
    }
}
