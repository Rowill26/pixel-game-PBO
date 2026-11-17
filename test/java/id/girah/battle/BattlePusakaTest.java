package id.girah.battle;

import static org.junit.jupiter.api.Assertions.*;

import id.girah.karakter.CalonArang;
import id.girah.karakter.Leak;
import id.girah.karakter.Senopati;
import id.girah.karakter.Wiku;
import id.girah.pusaka.KembangKantil;
import id.girah.pusaka.TasPusaka;
import id.girah.pusaka.TirtaSuci;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BattlePusakaTest {

    private Senopati senopati;
    private Wiku wiku;
    private CalonArang calonArang;
    private TasPusaka tas;
    private BattleManager battle;

    @BeforeEach
    void siapkan() {
        senopati = new Senopati("Arya");
        wiku = new Wiku("Dang Hyang");
        calonArang = new CalonArang();
        tas = new TasPusaka();
        tas.tambah(new TirtaSuci(), 1);
        tas.tambah(new KembangKantil(), 1);
        battle = new BattleManager(List.of(senopati, wiku), List.of(calonArang), BattleManager.TANPA_BATAS_RONDE, tas);
        battle.mulai();                                     // Calon Arang 12, Senopati 10, Wiku 9
    }

    @Test
    @DisplayName("Pengelana memakai Tirta Suci: nyawa pulih, tas berkurang, giliran maju")
    void pakaiTirtaSuci() {
        battle.serang(senopati);                            // Calon Arang: 12 kerusakan -> 108
        assertSame(senopati, battle.getAktor());
        assertEquals(List.of(senopati), battle.getSasaranPusaka(new TirtaSuci()), "Wiku masih sehat");

        String narasi = battle.gunakanPusaka(new TirtaSuci(), senopati);

        assertEquals(120, senopati.getNyawa());
        assertEquals(0, tas.jumlah(new TirtaSuci()));
        assertSame(wiku, battle.getAktor());
        assertTrue(battle.getBabad().getIsi().contains(narasi));
    }

    @Test
    void pusakaTidakBergunaDitolakTanpaMenghabiskanGiliran() {
        battle.serang(senopati);
        assertEquals(List.of(), battle.getSasaranPusaka(new KembangKantil()), "Energi semua masih penuh");
        assertThrows(IllegalArgumentException.class, () -> battle.gunakanPusaka(new KembangKantil(), senopati));
        assertSame(senopati, battle.getAktor());
        assertEquals(1, tas.jumlah(new KembangKantil()));
    }

    @Test
    void pusakaYangHabisDitolak() {
        battle.serang(senopati);
        battle.gunakanPusaka(new TirtaSuci(), senopati);    // Tirta Suci habis
        senopati.terimaSerangan(30);
        assertThrows(IllegalStateException.class, () -> battle.gunakanPusaka(new TirtaSuci(), senopati));
    }

    @Test
    void musuhTidakBolehMemakaiTas() {
        senopati.terimaSerangan(30);
        assertSame(calonArang, battle.getAktor());
        assertThrows(IllegalStateException.class, () -> battle.gunakanPusaka(new TirtaSuci(), calonArang));
    }

    @Test
    @DisplayName("Tas yang sama dipakai lintas pertarungan: pusaka terpakai tidak kembali")
    void tasDibawaKePertarunganBerikutnya() {
        battle.serang(senopati);
        battle.gunakanPusaka(new TirtaSuci(), senopati);

        BattleManager babakBerikutnya = new BattleManager(List.of(senopati), List.of(new Leak("Leak")),
                BattleManager.TANPA_BATAS_RONDE, tas);
        assertSame(tas, babakBerikutnya.getTasPusaka());
        assertEquals(0, babakBerikutnya.getTasPusaka().jumlah(new TirtaSuci()));
        assertEquals(1, babakBerikutnya.getTasPusaka().jumlah(new KembangKantil()));
    }

    @Test
    void konstruktorLamaMemakaiTasKosong() {
        BattleManager b = new BattleManager(List.of(new Senopati("X")), List.of(new Leak("Y")));
        assertTrue(b.getTasPusaka().isKosong());
    }
}
