package com.apuestatotal.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.apuestatotal.excepcion.ValidacionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ValidadorDniTest {

    @Test
    void aceptaOchoDigitos() {
        assertEquals("12345678", ValidadorDni.validar("12345678"));
    }

    @Test
    void recortaEspaciosDeLosExtremos() {
        assertEquals("12345678", ValidadorDni.validar("  12345678 "));
    }

    @Test
    void rechazaNulo() {
        assertThrows(ValidacionException.class, () -> ValidadorDni.validar(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1234567", "123456789", "1234567a", "abcdefgh", "12 45678",
            "1234567\u0663", "1'; DROP TABLE usuario;--", "<script>", "１２３４５６７８"})
    void rechazaTodoLoQueNoSeanOchoDigitosAscii(String entrada) {
        assertThrows(ValidacionException.class, () -> ValidadorDni.validar(entrada));
    }
}
