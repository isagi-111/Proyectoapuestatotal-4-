package com.apuestatotal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apuestatotal.excepcion.ConflictoException;
import com.apuestatotal.excepcion.DemasiadosIntentosException;
import com.apuestatotal.excepcion.NoAutenticadoException;
import com.apuestatotal.excepcion.ValidacionException;
import com.apuestatotal.model.Rol;
import com.apuestatotal.model.Usuario;
import com.apuestatotal.soporte.Escenario;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AutenticacionServiceTest {

    private final Escenario e = new Escenario();

    @Test
    void registraConSoloElDniYCreaBilleteraConSaldoSimulado() {
        Usuario u = e.autenticacion.registrar("12345678", "ip");
        assertEquals("12****78", u.dniMascara());
        assertEquals(Rol.CLIENTE, u.rol());
        assertEquals(0, new BigDecimal("1000.00").compareTo(e.billeteras.saldoDe(u.id()).orElseThrow()));
    }

    @Test
    void elDniNuncaSeGuardaEnClaro() {
        Usuario u = e.autenticacion.registrar("12345678", "ip");
        assertNotEquals("12345678", u.dniHash());
        assertFalse(u.dniHash().contains("12345678"));
        assertFalse(u.dniMascara().contains("345678"));
    }

    @Test
    void rechazaDniInvalido() {
        assertThrows(ValidacionException.class, () -> e.autenticacion.registrar("123", "ip"));
        assertThrows(ValidacionException.class, () -> e.autenticacion.registrar("abcdefgh", "ip"));
    }

    @Test
    void rechazaDniDuplicado() {
        e.autenticacion.registrar("12345678", "ip-1");
        assertThrows(ConflictoException.class, () -> e.autenticacion.registrar("12345678", "ip-2"));
    }

    @Test
    void iniciaSesionConElDniYDevuelveToken() {
        e.autenticacion.registrar("12345678", "ip");
        SesionIniciada s = e.autenticacion.iniciarSesion("12345678", "ip");
        assertFalse(s.token().isBlank());
        assertEquals(0, new BigDecimal("1000.00").compareTo(s.saldo()));
        assertTrue(e.autenticacion.validar(s.token()).isPresent());
    }

    @Test
    void dniNoRegistradoNoInicia() {
        assertThrows(NoAutenticadoException.class, () -> e.autenticacion.iniciarSesion("87654321", "ip"));
    }

    @Test
    void cerrarSesionInvalidaElToken() {
        e.autenticacion.registrar("12345678", "ip");
        SesionIniciada s = e.autenticacion.iniciarSesion("12345678", "ip");
        e.autenticacion.cerrarSesion(s.token());
        assertTrue(e.autenticacion.validar(s.token()).isEmpty());
    }

    @Test
    void bloqueaDespuesDeVariosIntentosFallidosPorIp() {
        for (int i = 0; i < 3; i++) {
            assertThrows(NoAutenticadoException.class, () -> e.autenticacion.iniciarSesion("87654321", "ip-x"));
        }
        assertThrows(DemasiadosIntentosException.class, () -> e.autenticacion.iniciarSesion("87654321", "ip-x"));
    }

    @Test
    void unLoginCorrectoReiniciaElContadorDeFallos() {
        e.autenticacion.registrar("12345678", "otra");
        assertThrows(NoAutenticadoException.class, () -> e.autenticacion.iniciarSesion("87654321", "ip-y"));
        e.autenticacion.iniciarSesion("12345678", "ip-y");
        for (int i = 0; i < 3; i++) {
            assertThrows(NoAutenticadoException.class, () -> e.autenticacion.iniciarSesion("87654321", "ip-y"));
        }
    }

    @Test
    void limitaRegistrosMasivosPorIp() {
        e.autenticacion.registrar("10000001", "ip-z");
        e.autenticacion.registrar("10000002", "ip-z");
        e.autenticacion.registrar("10000003", "ip-z");
        assertThrows(DemasiadosIntentosException.class, () -> e.autenticacion.registrar("10000004", "ip-z"));
    }

    @Test
    void crearAdministradorEsIdempotente() {
        e.autenticacion.asegurarAdministrador("99999999");
        e.autenticacion.asegurarAdministrador("99999999");
        SesionIniciada s = e.autenticacion.iniciarSesion("99999999", "ip");
        assertEquals(Rol.ADMIN, s.rol());
    }
}
