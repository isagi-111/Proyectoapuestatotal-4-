package com.apuestatotal.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apuestatotal.excepcion.ValidacionException;
import com.apuestatotal.soporte.Escenario;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ModeloTest {

    @Test
    void opcionSegunMarcador() {
        assertEquals(Opcion.LOCAL, Opcion.deResultado(2, 1));
        assertEquals(Opcion.EMPATE, Opcion.deResultado(1, 1));
        assertEquals(Opcion.VISITA, Opcion.deResultado(0, 3));
    }

    @Test
    void opcionDeTextoEsEstricta() {
        assertEquals(Opcion.LOCAL, Opcion.deTexto(" local "));
        assertThrows(ValidacionException.class, () -> Opcion.deTexto("GANA"));
        assertThrows(ValidacionException.class, () -> Opcion.deTexto(null));
    }

    @Test
    void tipoYEstadoDeTexto() {
        assertEquals(TipoApuesta.MULTIPLE, TipoApuesta.deTexto("multiple"));
        assertThrows(ValidacionException.class, () -> TipoApuesta.deTexto("xyz"));
        assertEquals(EstadoApuesta.PENDIENTE, EstadoApuesta.deTexto("pendiente"));
        assertThrows(ValidacionException.class, () -> EstadoApuesta.deTexto("' OR 1=1 --"));
        assertThrows(ValidacionException.class, () -> EstadoApuesta.deTexto(null));
    }

    @Test
    void partidoDevuelveLaCuotaDeCadaOpcion() {
        Partido p = new Escenario().turquiaFrancia;
        assertEquals(0, p.cuota(Opcion.LOCAL).compareTo(new java.math.BigDecimal("2.51")));
        assertEquals(0, p.cuota(Opcion.EMPATE).compareTo(new java.math.BigDecimal("3.19")));
        assertEquals(0, p.cuota(Opcion.VISITA).compareTo(new java.math.BigDecimal("2.44")));
        assertEquals("Turquía vs Francia", p.descripcion());
    }

    @Test
    void partidoFinalizadoTieneResultadoYYaNoEsApostable() {
        Partido p = new Escenario().turquiaFrancia;
        assertTrue(p.apostable());
        assertEquals(Optional.empty(), p.resultado());
        Partido fin = p.finalizado(2, 0);
        assertFalse(fin.apostable());
        assertEquals(Optional.of(Opcion.LOCAL), fin.resultado());
        assertEquals(EstadoPartido.PROGRAMADO, p.estado(), "el original no cambia: es inmutable");
    }

    @Test
    void apuestaCopiaDefensivaDeSelecciones() {
        var lista = new java.util.ArrayList<Seleccion>();
        Apuesta a = new Apuesta(0, 1, TipoApuesta.SIMPLE, EstadoApuesta.PENDIENTE, java.math.BigDecimal.ONE,
                java.math.BigDecimal.ONE, java.math.BigDecimal.ONE, java.time.Instant.EPOCH, lista);
        lista.add(new Seleccion(1, Opcion.LOCAL, java.math.BigDecimal.ONE, "x"));
        assertEquals(0, a.selecciones().size());
        assertThrows(UnsupportedOperationException.class, () -> a.selecciones().add(null));
    }
}
