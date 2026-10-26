package com.apuestatotal.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.apuestatotal.dao.jdbc.BilleteraDaoJdbc;
import com.apuestatotal.dao.jdbc.FabricaConexiones;
import com.apuestatotal.dao.jdbc.GestorConexiones;
import com.apuestatotal.dao.jdbc.InicializadorEsquema;
import com.apuestatotal.dao.jdbc.UsuarioDaoJdbc;
import com.apuestatotal.model.Rol;
import com.apuestatotal.model.Usuario;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransaccionJdbcTest {

    private GestorConexiones gestor;
    private BilleteraDao billeteras;
    private long usuarioId;

    @BeforeEach
    void preparar() {
        FabricaConexiones fabrica = new FabricaConexiones(
                "jdbc:h2:mem:tx_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        InicializadorEsquema.ejecutar(fabrica);
        gestor = new GestorConexiones(fabrica);
        billeteras = new BilleteraDaoJdbc(gestor);
        usuarioId = new UsuarioDaoJdbc(gestor).guardar(
                new Usuario(0, "hash", "12****78", Rol.CLIENTE, Instant.now())).id();
        billeteras.crear(usuarioId, new BigDecimal("100.00"));
    }

    private BigDecimal saldo() {
        return billeteras.saldoDe(usuarioId).orElseThrow();
    }

    @Test
    void siTodoSaleBienSeConfirma() {
        gestor.ejecutarSinResultado(() -> {
            billeteras.debitar(usuarioId, new BigDecimal("30.00"));
            billeteras.acreditar(usuarioId, new BigDecimal("5.00"));
        });
        assertEquals(0, new BigDecimal("75.00").compareTo(saldo()));
    }

    @Test
    void siFallaAlgoSeDeshaceTodo() {
        assertThrows(IllegalStateException.class, () -> gestor.ejecutarSinResultado(() -> {
            billeteras.debitar(usuarioId, new BigDecimal("40.00"));
            throw new IllegalStateException("falla a mitad de la operación");
        }));
        assertEquals(0, new BigDecimal("100.00").compareTo(saldo()), "el rollback devolvió el dinero");
    }

    @Test
    void lasTransaccionesAnidadasReutilizanLaConexion() {
        gestor.ejecutarSinResultado(() -> gestor.ejecutarSinResultado(() ->
                billeteras.debitar(usuarioId, new BigDecimal("1.00"))));
        assertEquals(0, new BigDecimal("99.00").compareTo(saldo()));
    }

    @Test
    void unaViolacionDeRestriccionSeTraduceAUnaExcepcionGenerica() {
        var ex = assertThrows(com.apuestatotal.excepcion.AccesoDatosException.class,
                () -> billeteras.crear(999_999, BigDecimal.ONE)); // usuario inexistente: viola la FK
        assertEquals(500, ex.codigoHttp());
        assertEquals("Error de acceso a datos.", ex.getMessage(), "no se filtra el detalle SQL al cliente");
    }
}
