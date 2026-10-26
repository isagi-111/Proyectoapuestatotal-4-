package com.apuestatotal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.apuestatotal.excepcion.ConflictoException;
import com.apuestatotal.excepcion.NoEncontradoException;
import com.apuestatotal.excepcion.ValidacionException;
import com.apuestatotal.model.EstadoApuesta;
import com.apuestatotal.model.Opcion;
import com.apuestatotal.model.TipoApuesta;
import com.apuestatotal.soporte.Escenario;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PartidoServiceTest {

    private final Escenario e = new Escenario();
    private final long usuario = e.registrar("12345678");

    private void apostar(TipoApuesta tipo, String monto, SolicitudApuesta.Item... items) {
        e.servicioApuestas.colocar(usuario, new SolicitudApuesta(tipo, new BigDecimal(monto), List.of(items)));
    }

    private void assertSaldo(String esperado) {
        assertEquals(0, new BigDecimal(esperado).compareTo(e.billetera.saldo(usuario)));
    }

    @Test
    void apuestaGanadoraSePagaAlSaldo() {
        apostar(TipoApuesta.SIMPLE, "100", new SolicitudApuesta.Item(e.turquiaFrancia.id(), Opcion.LOCAL));
        assertSaldo("900.00");
        ResultadoLiquidacion r = e.servicioPartidos.finalizar(e.turquiaFrancia.id(), 2, 1);
        assertEquals(1, r.ganadas());
        assertSaldo("1151.00"); // 900 + 100 * 2.51
        assertEquals(EstadoApuesta.GANADA, e.servicioApuestas.listar(usuario, Optional.empty()).get(0).estado());
    }

    @Test
    void apuestaPerdidaNoPagaNada() {
        apostar(TipoApuesta.SIMPLE, "100", new SolicitudApuesta.Item(e.turquiaFrancia.id(), Opcion.VISITA));
        ResultadoLiquidacion r = e.servicioPartidos.finalizar(e.turquiaFrancia.id(), 2, 1);
        assertEquals(1, r.perdidas());
        assertSaldo("900.00");
        assertEquals(EstadoApuesta.PERDIDA, e.servicioApuestas.listar(usuario, Optional.empty()).get(0).estado());
    }

    @Test
    void multipleSigueEnPendienteHastaQueTerminenTodos() {
        apostar(TipoApuesta.MULTIPLE, "10", new SolicitudApuesta.Item(e.turquiaFrancia.id(), Opcion.LOCAL),
                new SolicitudApuesta.Item(e.italiaBelgica.id(), Opcion.LOCAL));
        assertEquals(1, e.servicioPartidos.finalizar(e.turquiaFrancia.id(), 1, 0).pendientes());
        assertSaldo("990.00");
        assertEquals(1, e.servicioPartidos.finalizar(e.italiaBelgica.id(), 3, 0).ganadas());
        assertSaldo("1047.98"); // 990 + 57.98
    }

    @Test
    void multipleSePierdeEnCuantoFallaUnaSeleccion() {
        apostar(TipoApuesta.MULTIPLE, "10", new SolicitudApuesta.Item(e.turquiaFrancia.id(), Opcion.LOCAL),
                new SolicitudApuesta.Item(e.italiaBelgica.id(), Opcion.LOCAL));
        assertEquals(1, e.servicioPartidos.finalizar(e.turquiaFrancia.id(), 0, 0).perdidas());
        assertSaldo("990.00");
    }

    @Test
    void noSePuedeFinalizarDosVecesNiPagarDosVeces() {
        apostar(TipoApuesta.SIMPLE, "100", new SolicitudApuesta.Item(e.turquiaFrancia.id(), Opcion.LOCAL));
        e.servicioPartidos.finalizar(e.turquiaFrancia.id(), 1, 0);
        assertThrows(ConflictoException.class, () -> e.servicioPartidos.finalizar(e.turquiaFrancia.id(), 1, 0));
        assertSaldo("1151.00");
    }

    @Test
    void validaGolesYExistencia() {
        long id = e.turquiaFrancia.id();
        assertThrows(ValidacionException.class, () -> e.servicioPartidos.finalizar(id, -1, 0));
        assertThrows(ValidacionException.class, () -> e.servicioPartidos.finalizar(id, 100, 0));
        assertThrows(ValidacionException.class, () -> e.servicioPartidos.finalizar(id, null, 0));
        assertThrows(NoEncontradoException.class, () -> e.servicioPartidos.finalizar(9999, 1, 0));
    }

    @Test
    void listaLosPartidos() {
        assertEquals(3, e.servicioPartidos.listar().size());
    }
}
