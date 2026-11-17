package id.girah.karakter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EnergiBatinHabisExceptionTest {

    @Test
    void menyimpanDetailDanPesan() {
        EnergiBatinHabisException ex = new EnergiBatinHabisException("Calon Arang", 30, 12);
        assertEquals("Calon Arang", ex.getNamaKarakter());
        assertEquals(30, ex.getDibutuhkan());
        assertEquals(12, ex.getTersedia());
        assertEquals(18, ex.getKekurangan());
        assertEquals("Calon Arang kehabisan energi batin: butuh 30, tersisa 12", ex.getMessage());
    }

    @Test
    void merupakanCheckedException() {
        Exception ex = new EnergiBatinHabisException("X", 1, 0);
        assertFalse(ex instanceof RuntimeException);
    }
}
