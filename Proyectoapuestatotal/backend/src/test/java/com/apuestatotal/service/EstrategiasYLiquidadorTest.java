package com.apuestatotal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.apuestatotal.excepcion.ValidacionException;
import com.apuestatotal.model.Apuesta;
import com.apuestatotal.model.EstadoApuesta;
import com.apuestatotal.model.Opcion;
import com.apuestatotal.model.Partido;
import com.apuestatotal.model.Seleccion;
import com.apuestatotal.model.TipoApuesta;
import com.apuestatotal.soporte.Escenario;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EstrategiasYLiquidadorTest {

    private final Escenario e = new Escenario();

    private static Seleccion sel(Partido p, Opcion o) {
        return new Seleccion(p.id(), o, p.cuota(o), p.descripcion());
    }

    @Test
    void simpleCreaUnaApuestaPorSeleccion() {
        var lista = new EstrategiaSimple().construir(1, new BigDecimal("100.00"),
                List.of(sel(e.turquiaFrancia, Opcion.LOCAL), sel(e.italiaBelgica, Opcion.VISITA)), Instant.EPOCH);
        assertEquals(2, lista.size());
        assertEquals(0, new BigDecimal("251.00").compareTo(lista.get(0).gananciaPotencial()));
        assertEquals(0, new BigDecimal("365.00").compareTo(lista.get(1).gananciaPotencial()));
        assertEquals(TipoApuesta.SIMPLE, lista.get(0).tipo());
    }

    @Test
    void multipleMultiplicaLasCuotas() {
        var lista = new EstrategiaMultiple().construir(1, new BigDecimal("10.00"),
                List.of(sel(e.turquiaFrancia, Opcion.LOCAL), sel(e.italiaBelgica, Opcion.LOCAL)), Instant.EPOCH);
        assertEquals(1, lista.size());
        // 2.51 * 2.31 = 5.7981
        assertEquals(0, new BigDecimal("5.80").compareTo(lista.get(0).cuotaTotal()));
        assertEquals(0, new BigDecimal("57.98").compareTo(lista.get(0).gananciaPotencial()));
    }

    @Test
    void multipleExigeAlMenosDosSelecciones() {
        assertThrows(ValidacionException.class, () -> new EstrategiaMultiple().construir(1, BigDecimal.TEN,
                List.of(sel(e.turquiaFrancia, Opcion.LOCAL)), Instant.EPOCH));
    }

    private Apuesta apuestaDe(Seleccion... s) {
        return new Apuesta(1, 1, TipoApuesta.MULTIPLE, EstadoApuesta.PENDIENTE, BigDecimal.TEN, BigDecimal.TEN,
                BigDecimal.TEN, Instant.EPOCH, List.of(s));
    }

    @Test
    void liquidadorPendienteSiFaltaUnPartido() {
        Partido fin = e.turquiaFrancia.finalizado(2, 0);
        Apuesta a = apuestaDe(sel(fin, Opcion.LOCAL), sel(e.italiaBelgica, Opcion.LOCAL));
        var estado = new LiquidadorApuestas().evaluar(a, id -> id == fin.id() ? Optional.of(fin) : Optional.of(e.italiaBelgica));
        assertEquals(EstadoApuesta.PENDIENTE, estado);
    }

    @Test
    void liquidadorPerdidaSiAlgunaFallaAunqueFalteOtro() {
        Partido fin = e.turquiaFrancia.finalizado(0, 1);
        Apuesta a = apuestaDe(sel(fin, Opcion.LOCAL), sel(e.italiaBelgica, Opcion.LOCAL));
        var estado = new LiquidadorApuestas().evaluar(a, id -> id == fin.id() ? Optional.of(fin) : Optional.of(e.italiaBelgica));
        assertEquals(EstadoApuesta.PERDIDA, estado);
    }

    @Test
    void liquidadorGanadaSiTodasAciertan() {
        Partido p1 = e.turquiaFrancia.finalizado(2, 0);
        Partido p2 = e.italiaBelgica.finalizado(1, 1);
        Apuesta a = apuestaDe(sel(p1, Opcion.LOCAL), sel(p2, Opcion.EMPATE));
        var estado = new LiquidadorApuestas().evaluar(a, id -> Optional.of(id == p1.id() ? p1 : p2));
        assertEquals(EstadoApuesta.GANADA, estado);
    }
}
