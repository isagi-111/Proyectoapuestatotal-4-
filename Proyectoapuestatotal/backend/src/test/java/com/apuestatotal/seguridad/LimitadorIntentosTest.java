package com.apuestatotal.seguridad;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.apuestatotal.excepcion.DemasiadosIntentosException;
import com.apuestatotal.soporte.RelojMutable;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class LimitadorIntentosTest {

    private final RelojMutable reloj = new RelojMutable();
    private final LimitadorIntentos limitador = new LimitadorIntentos(3, Duration.ofMinutes(10), reloj);

    @Test
    void permiteHastaElMaximoYLuegoBloquea() {
        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() -> limitador.verificar("ip"));
            limitador.registrar("ip");
        }
        assertThrows(DemasiadosIntentosException.class, () -> limitador.verificar("ip"));
    }

    @Test
    void elBloqueoEsPorClave() {
        for (int i = 0; i < 3; i++) {
            limitador.registrar("ip-1");
        }
        assertDoesNotThrow(() -> limitador.verificar("ip-2"));
    }

    @Test
    void despuesDeLaVentanaSeDesbloquea() {
        for (int i = 0; i < 3; i++) {
            limitador.registrar("ip");
        }
        reloj.avanzar(Duration.ofMinutes(11));
        assertDoesNotThrow(() -> limitador.verificar("ip"));
    }

    @Test
    void limpiarReiniciaElContador() {
        for (int i = 0; i < 3; i++) {
            limitador.registrar("ip");
        }
        limitador.limpiar("ip");
        assertDoesNotThrow(() -> limitador.verificar("ip"));
    }
}
