package com.apuestatotal.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apuestatotal.excepcion.ConflictoException;
import com.apuestatotal.excepcion.NoEncontradoException;
import com.apuestatotal.model.Apuesta;
import com.apuestatotal.model.EstadoApuesta;
import com.apuestatotal.model.EstadoPartido;
import com.apuestatotal.model.Opcion;
import com.apuestatotal.model.Partido;
import com.apuestatotal.model.Rol;
import com.apuestatotal.model.Seleccion;
import com.apuestatotal.model.TipoApuesta;
import com.apuestatotal.model.Usuario;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Contrato que TODA implementación de los DAO debe cumplir. Las subclases (memoria y JDBC)
 * heredan los mismos tests: así se demuestra el principio de sustitución de Liskov.
 */
abstract class ContratoDaoTest {

    protected UsuarioDao usuarios;
    protected BilleteraDao billeteras;
    protected PartidoDao partidos;
    protected ApuestaDao apuestas;

    /** Cada subclase deja listos los cuatro DAO con datos vacíos. */
    @BeforeEach
    abstract void preparar() throws Exception;

    private Usuario nuevoUsuario(String hash) {
        return usuarios.guardar(new Usuario(0, hash, "12****78", Rol.CLIENTE, Instant.parse("2026-10-25T12:00:00Z")));
    }

    private Partido nuevoPartido() {
        return partidos.guardar(new Partido(0, "Liga", "Local", "Visita", Instant.parse("2026-10-26T12:00:00Z"),
                EstadoPartido.PROGRAMADO, new BigDecimal("2.51"), new BigDecimal("3.19"), new BigDecimal("2.44"), null, null));
    }

    private Apuesta apuesta(long usuarioId, Partido... ps) {
        List<Seleccion> sel = java.util.Arrays.stream(ps)
                .map(p -> new Seleccion(p.id(), Opcion.LOCAL, p.cuota(Opcion.LOCAL), p.descripcion())).toList();
        return new Apuesta(0, usuarioId, ps.length > 1 ? TipoApuesta.MULTIPLE : TipoApuesta.SIMPLE,
                EstadoApuesta.PENDIENTE, new BigDecimal("10.00"), new BigDecimal("2.51"), new BigDecimal("25.10"),
                Instant.parse("2026-10-25T12:30:00Z"), sel);
    }

    // ---------- usuarios ----------
    @Test
    void usuarioSeGuardaYSeBuscaPorIdYPorHash() {
        Usuario u = nuevoUsuario("hash-1");
        assertTrue(u.id() > 0);
        assertEquals("hash-1", usuarios.buscarPorId(u.id()).orElseThrow().dniHash());
        assertEquals(u.id(), usuarios.buscarPorDniHash("hash-1").orElseThrow().id());
        assertEquals(Rol.CLIENTE, usuarios.buscarPorId(u.id()).orElseThrow().rol());
        assertTrue(usuarios.buscarPorDniHash("no-existe").isEmpty());
        assertTrue(usuarios.buscarPorId(12345).isEmpty());
    }

    @Test
    void usuarioDuplicadoLanzaConflicto() {
        nuevoUsuario("hash-dup");
        assertThrows(ConflictoException.class, () -> nuevoUsuario("hash-dup"));
    }

    @Test
    void usuarioNoEsVulnerableAInyeccionSql() {
        nuevoUsuario("hash-ok");
        assertTrue(usuarios.buscarPorDniHash("x' OR '1'='1").isEmpty());
    }

    // ---------- billetera ----------
    @Test
    void billeteraCreaConsultaDebitaYAcredita() {
        Usuario u = nuevoUsuario("h");
        billeteras.crear(u.id(), new BigDecimal("1000.00"));
        assertEquals(0, new BigDecimal("1000.00").compareTo(billeteras.saldoDe(u.id()).orElseThrow()));
        assertTrue(billeteras.debitar(u.id(), new BigDecimal("250.50")));
        assertEquals(0, new BigDecimal("749.50").compareTo(billeteras.saldoDe(u.id()).orElseThrow()));
        billeteras.acreditar(u.id(), new BigDecimal("0.50"));
        assertEquals(0, new BigDecimal("750.00").compareTo(billeteras.saldoDe(u.id()).orElseThrow()));
    }

    @Test
    void billeteraNoPermiteSaldoNegativo() {
        Usuario u = nuevoUsuario("h");
        billeteras.crear(u.id(), new BigDecimal("100.00"));
        assertFalse(billeteras.debitar(u.id(), new BigDecimal("100.01")));
        assertEquals(0, new BigDecimal("100.00").compareTo(billeteras.saldoDe(u.id()).orElseThrow()));
        assertTrue(billeteras.debitar(u.id(), new BigDecimal("100.00")));
    }

    @Test
    void billeteraInexistente() {
        assertTrue(billeteras.saldoDe(777).isEmpty());
        assertFalse(billeteras.debitar(777, BigDecimal.ONE));
        assertThrows(NoEncontradoException.class, () -> billeteras.acreditar(777, BigDecimal.ONE));
    }

    @Test
    void debitosConcurrentesNuncaSobregiran() throws Exception {
        Usuario u = nuevoUsuario("h");
        billeteras.crear(u.id(), new BigDecimal("100.00"));
        ExecutorService pool = Executors.newFixedThreadPool(8);
        AtomicInteger exitos = new AtomicInteger();
        List<Future<?>> tareas = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tareas.add(pool.submit(() -> {
                if (billeteras.debitar(u.id(), new BigDecimal("10.00"))) {
                    exitos.incrementAndGet();
                }
            }));
        }
        for (Future<?> t : tareas) {
            t.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();
        assertEquals(10, exitos.get());
        assertEquals(0, BigDecimal.ZERO.compareTo(billeteras.saldoDe(u.id()).orElseThrow()));
    }

    // ---------- partidos ----------
    @Test
    void partidoSeGuardaListaYActualiza() {
        Partido p = nuevoPartido();
        assertTrue(p.id() > 0);
        assertEquals(1, partidos.listar().size());
        Partido leido = partidos.buscarPorId(p.id()).orElseThrow();
        assertEquals("Local vs Visita", leido.descripcion());
        assertEquals(0, new BigDecimal("2.51").compareTo(leido.cuotaLocal()));
        assertEquals(Instant.parse("2026-10-26T12:00:00Z"), leido.inicio());
        assertEquals(null, leido.golesLocal());

        partidos.actualizar(leido.finalizado(3, 1));
        Partido fin = partidos.buscarPorId(p.id()).orElseThrow();
        assertEquals(EstadoPartido.FINALIZADO, fin.estado());
        assertEquals(3, fin.golesLocal());
        assertEquals(1, fin.golesVisita());
    }

    @Test
    void actualizarPartidoInexistenteFalla() {
        Partido fantasma = nuevoPartido().conId(9999);
        assertThrows(NoEncontradoException.class, () -> partidos.actualizar(fantasma));
    }

    // ---------- apuestas ----------
    @Test
    void apuestaSeGuardaConSusSeleccionesYSeListaPorUsuario() {
        Usuario u = nuevoUsuario("h1");
        Usuario otro = nuevoUsuario("h2");
        Partido p1 = nuevoPartido();
        Partido p2 = nuevoPartido();
        Apuesta a = apuestas.guardar(apuesta(u.id(), p1, p2));
        apuestas.guardar(apuesta(otro.id(), p1));
        assertTrue(a.id() > 0);

        List<Apuesta> mias = apuestas.listarPorUsuario(u.id());
        assertEquals(1, mias.size());
        assertEquals(2, mias.get(0).selecciones().size());
        assertEquals(TipoApuesta.MULTIPLE, mias.get(0).tipo());
        assertEquals(0, new BigDecimal("25.10").compareTo(mias.get(0).gananciaPotencial()));
        assertEquals(Instant.parse("2026-10-25T12:30:00Z"), mias.get(0).creadaEn());
    }

    @Test
    void apuestasDelUsuarioVienenDeLaMasNuevaALaMasAntigua() {
        Usuario u = nuevoUsuario("h1");
        Partido p = nuevoPartido();
        Apuesta a1 = apuestas.guardar(apuesta(u.id(), p));
        Apuesta a2 = apuestas.guardar(apuesta(u.id(), p));
        List<Apuesta> lista = apuestas.listarPorUsuario(u.id());
        assertEquals(a2.id(), lista.get(0).id());
        assertEquals(a1.id(), lista.get(1).id());
    }

    @Test
    void pendientesPorPartidoYCambioDeEstado() {
        Usuario u = nuevoUsuario("h1");
        Partido p1 = nuevoPartido();
        Partido p2 = nuevoPartido();
        Apuesta a1 = apuestas.guardar(apuesta(u.id(), p1));
        Apuesta a2 = apuestas.guardar(apuesta(u.id(), p1, p2));
        apuestas.guardar(apuesta(u.id(), p2));

        assertEquals(2, apuestas.listarPendientesPorPartido(p1.id()).size());
        apuestas.actualizarEstado(a1.id(), EstadoApuesta.GANADA);
        List<Apuesta> pendientes = apuestas.listarPendientesPorPartido(p1.id());
        assertEquals(1, pendientes.size());
        assertEquals(a2.id(), pendientes.get(0).id());
        assertEquals(2, pendientes.get(0).selecciones().size());
        assertEquals(EstadoApuesta.GANADA, apuestas.listarPorUsuario(u.id()).stream()
                .filter(x -> x.id() == a1.id()).findFirst().orElseThrow().estado());
    }

    @Test
    void actualizarEstadoDeApuestaInexistenteFalla() {
        assertThrows(NoEncontradoException.class, () -> apuestas.actualizarEstado(4242, EstadoApuesta.PERDIDA));
    }
}
