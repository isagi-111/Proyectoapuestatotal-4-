package com.apuestatotal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apuestatotal.excepcion.ConflictoException;
import com.apuestatotal.excepcion.NoEncontradoException;
import com.apuestatotal.excepcion.SaldoInsuficienteException;
import com.apuestatotal.excepcion.ValidacionException;
import com.apuestatotal.model.Apuesta;
import com.apuestatotal.model.EstadoApuesta;
import com.apuestatotal.model.Opcion;
import com.apuestatotal.model.TipoApuesta;
import com.apuestatotal.soporte.Escenario;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ApuestaServiceTest {

    private final Escenario e = new Escenario();
    private final long usuario = e.registrar("12345678");

    private SolicitudApuesta cupon(TipoApuesta tipo, String monto, SolicitudApuesta.Item... items) {
        return new SolicitudApuesta(tipo, monto == null ? null : new BigDecimal(monto), List.of(items));
    }

    private SolicitudApuesta.Item item(long partidoId, Opcion o) {
        return new SolicitudApuesta.Item(partidoId, o);
    }

    private void assertSaldo(String esperado) {
        assertEquals(0, new BigDecimal(esperado).compareTo(e.billetera.saldo(usuario)));
    }

    @Test
    void simpleDescuentaElMontoPorCadaSeleccion() {
        List<Apuesta> creadas = e.servicioApuestas.colocar(usuario, cupon(TipoApuesta.SIMPLE, "100",
                item(e.turquiaFrancia.id(), Opcion.LOCAL), item(e.italiaBelgica.id(), Opcion.EMPATE)));
        assertEquals(2, creadas.size());
        assertSaldo("800.00");
        assertTrue(creadas.stream().allMatch(a -> a.id() > 0 && a.estado() == EstadoApuesta.PENDIENTE));
    }

    @Test
    void multipleDescuentaUnaSolaVez() {
        List<Apuesta> creadas = e.servicioApuestas.colocar(usuario, cupon(TipoApuesta.MULTIPLE, "50",
                item(e.turquiaFrancia.id(), Opcion.LOCAL), item(e.italiaBelgica.id(), Opcion.LOCAL)));
        assertEquals(1, creadas.size());
        assertEquals(2, creadas.get(0).selecciones().size());
        assertSaldo("950.00");
    }

    @Test
    void laCuotaLaPoneElServidorNoElCliente() {
        Apuesta a = e.servicioApuestas.colocar(usuario, cupon(TipoApuesta.SIMPLE, "10",
                item(e.turquiaFrancia.id(), Opcion.LOCAL))).get(0);
        assertEquals(0, new BigDecimal("2.51").compareTo(a.selecciones().get(0).cuota()));
        assertEquals(0, new BigDecimal("25.10").compareTo(a.gananciaPotencial()));
    }

    @Test
    void conSaldoInsuficienteNoSeGuardaNadaNiSeDescuenta() {
        assertThrows(SaldoInsuficienteException.class, () -> e.servicioApuestas.colocar(usuario,
                cupon(TipoApuesta.SIMPLE, "600", item(e.turquiaFrancia.id(), Opcion.LOCAL),
                        item(e.italiaBelgica.id(), Opcion.LOCAL))));
        assertSaldo("1000.00");
        assertTrue(e.servicioApuestas.listar(usuario, Optional.empty()).isEmpty());
    }

    @Test
    void noSePuedeApostarAUnPartidoFinalizado() {
        e.servicioPartidos.finalizar(e.turquiaFrancia.id(), 1, 0);
        assertThrows(ConflictoException.class, () -> e.servicioApuestas.colocar(usuario,
                cupon(TipoApuesta.SIMPLE, "10", item(e.turquiaFrancia.id(), Opcion.LOCAL))));
        assertSaldo("1000.00");
    }

    @Test
    void sePuedeApostarEnVivo() {
        assertEquals(1, e.servicioApuestas.colocar(usuario, cupon(TipoApuesta.SIMPLE, "10",
                item(e.hungriaUcrania.id(), Opcion.VISITA))).size());
    }

    @Test
    void partidoInexistente() {
        assertThrows(NoEncontradoException.class, () -> e.servicioApuestas.colocar(usuario,
                cupon(TipoApuesta.SIMPLE, "10", item(9999, Opcion.LOCAL))));
    }

    @Test
    void validaMontos() {
        var it = item(e.turquiaFrancia.id(), Opcion.LOCAL);
        assertThrows(ValidacionException.class, () -> e.servicioApuestas.colocar(usuario, cupon(TipoApuesta.SIMPLE, null, it)));
        assertThrows(ValidacionException.class, () -> e.servicioApuestas.colocar(usuario, cupon(TipoApuesta.SIMPLE, "0.50", it)));
        assertThrows(ValidacionException.class, () -> e.servicioApuestas.colocar(usuario, cupon(TipoApuesta.SIMPLE, "5000.01", it)));
        assertThrows(ValidacionException.class, () -> e.servicioApuestas.colocar(usuario, cupon(TipoApuesta.SIMPLE, "-10", it)));
        assertThrows(ValidacionException.class, () -> e.servicioApuestas.colocar(usuario, cupon(TipoApuesta.SIMPLE, "10.123", it)));
        assertSaldo("1000.00");
    }

    @Test
    void validaElCupon() {
        assertThrows(ValidacionException.class, () -> e.servicioApuestas.colocar(usuario, cupon(TipoApuesta.SIMPLE, "10")));
        var it = item(e.turquiaFrancia.id(), Opcion.LOCAL);
        assertThrows(ValidacionException.class, () -> e.servicioApuestas.colocar(usuario,
                cupon(TipoApuesta.MULTIPLE, "10", it, item(e.turquiaFrancia.id(), Opcion.EMPATE))));
        assertThrows(ValidacionException.class, () -> e.servicioApuestas.colocar(usuario,
                cupon(TipoApuesta.MULTIPLE, "10", it)));
        assertThrows(ValidacionException.class, () -> e.servicioApuestas.colocar(usuario, null));
    }

    @Test
    void sistemaTodaviaNoEstaDisponible() {
        assertThrows(ValidacionException.class, () -> e.servicioApuestas.colocar(usuario,
                cupon(TipoApuesta.SISTEMA, "10", item(e.turquiaFrancia.id(), Opcion.LOCAL))));
    }

    @Test
    void limitaLaCantidadDeSelecciones() {
        var items = new java.util.ArrayList<SolicitudApuesta.Item>();
        for (int i = 1; i <= 11; i++) {
            items.add(item(i, Opcion.LOCAL));
        }
        assertThrows(ValidacionException.class, () -> e.servicioApuestas.colocar(usuario,
                new SolicitudApuesta(TipoApuesta.MULTIPLE, BigDecimal.TEN, items)));
    }

    @Test
    void limitaLaGananciaPotencial() {
        var alto = e.partidos.guardar(e.partido("A", "B", com.apuestatotal.model.EstadoPartido.PROGRAMADO, "99.00", "99.00", "99.00"));
        var alto2 = e.partidos.guardar(e.partido("C", "D", com.apuestatotal.model.EstadoPartido.PROGRAMADO, "99.00", "99.00", "99.00"));
        var alto3 = e.partidos.guardar(e.partido("E", "F", com.apuestatotal.model.EstadoPartido.PROGRAMADO, "99.00", "99.00", "99.00"));
        e.billetera.recargar(usuario, new BigDecimal("5000"));
        assertThrows(ValidacionException.class, () -> e.servicioApuestas.colocar(usuario, cupon(TipoApuesta.MULTIPLE, "5000",
                item(alto.id(), Opcion.LOCAL), item(alto2.id(), Opcion.LOCAL), item(alto3.id(), Opcion.LOCAL))));
    }

    @Test
    void listarFiltraPorEstadoYSoloDelUsuario() {
        long otro = e.registrar("87654321");
        e.servicioApuestas.colocar(usuario, cupon(TipoApuesta.SIMPLE, "10", item(e.turquiaFrancia.id(), Opcion.LOCAL)));
        e.servicioApuestas.colocar(otro, cupon(TipoApuesta.SIMPLE, "10", item(e.turquiaFrancia.id(), Opcion.VISITA)));
        assertEquals(1, e.servicioApuestas.listar(usuario, Optional.empty()).size());
        assertEquals(1, e.servicioApuestas.listar(usuario, Optional.of(EstadoApuesta.PENDIENTE)).size());
        assertEquals(0, e.servicioApuestas.listar(usuario, Optional.of(EstadoApuesta.GANADA)).size());
    }
}
