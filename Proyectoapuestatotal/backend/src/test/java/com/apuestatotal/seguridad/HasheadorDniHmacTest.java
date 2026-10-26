package com.apuestatotal.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class HasheadorDniHmacTest {

    private final HasheadorDni hasheador = new HasheadorDniHmac("pepper-secreto-0123456789".getBytes());

    @Test
    void mismoDniGeneraMismaHuella() {
        assertEquals(hasheador.hash("12345678"), hasheador.hash("12345678"));
    }

    @Test
    void dnisDistintosGeneranHuellasDistintas() {
        assertNotEquals(hasheador.hash("12345678"), hasheador.hash("12345679"));
    }

    @Test
    void laHuellaTieneLongitudSha256YNoContieneElDni() {
        String h = hasheador.hash("12345678");
        assertEquals(64, h.length());
        assertFalse(h.contains("12345678"));
    }

    @Test
    void otroPepperGeneraOtraHuella() {
        var otro = new HasheadorDniHmac("otro-pepper-secreto-9876543".getBytes());
        assertNotEquals(hasheador.hash("12345678"), otro.hash("12345678"));
    }

    @Test
    void enmascaraElDni() {
        assertEquals("12****78", hasheador.mascara("12345678"));
    }

    @Test
    void rechazaPepperCorto() {
        assertThrows(IllegalArgumentException.class, () -> new HasheadorDniHmac("corto".getBytes()));
    }
}
