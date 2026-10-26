package com.apuestatotal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.apuestatotal.excepcion.NoEncontradoException;
import com.apuestatotal.excepcion.ValidacionException;
import com.apuestatotal.soporte.Escenario;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BilleteraServiceTest {

    private final Escenario e = new Escenario();
    private final long usuario = e.registrar("12345678");

    @Test
    void recargarSumaAlSaldo() {
        BigDecimal nuevo = e.billetera.recargar(usuario, new BigDecimal("500"));
        assertEquals(0, new BigDecimal("1500.00").compareTo(nuevo));
        assertEquals(0, new BigDecimal("1500.00").compareTo(e.billetera.saldo(usuario)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5", "0.001", "10000.01", "100000"})
    void rechazaMontosFueraDeRango(String monto) {
        assertThrows(ValidacionException.class, () -> e.billetera.recargar(usuario, new BigDecimal(monto)));
        assertEquals(0, new BigDecimal("1000.00").compareTo(e.billetera.saldo(usuario)));
    }

    @Test
    void rechazaMontoNulo() {
        assertThrows(ValidacionException.class, () -> e.billetera.recargar(usuario, null));
    }

    @Test
    void saldoDeUsuarioInexistenteFalla() {
        assertThrows(NoEncontradoException.class, () -> e.billetera.saldo(9999));
    }
}
