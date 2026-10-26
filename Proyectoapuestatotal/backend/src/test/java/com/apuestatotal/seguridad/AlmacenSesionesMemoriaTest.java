package com.apuestatotal.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apuestatotal.model.Rol;
import com.apuestatotal.soporte.RelojMutable;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AlmacenSesionesMemoriaTest {

    private final RelojMutable reloj = new RelojMutable();
    private final AlmacenSesiones almacen = new AlmacenSesionesMemoria(reloj, Duration.ofMinutes(30));

    @Test
    void tokenValidoDevuelveLaSesion() {
        String token = almacen.crear(7, Rol.CLIENTE);
        SesionActiva s = almacen.validar(token).orElseThrow();
        assertEquals(7, s.usuarioId());
        assertEquals(Rol.CLIENTE, s.rol());
    }

    @Test
    void cadaTokenEsDistintoYLargo() {
        String a = almacen.crear(1, Rol.CLIENTE);
        String b = almacen.crear(1, Rol.CLIENTE);
        assertNotEquals(a, b);
        assertTrue(a.length() >= 43, "256 bits en Base64 URL deben dar al menos 43 caracteres");
    }

    @Test
    void laSesionExpiraConElTiempo() {
        String token = almacen.crear(1, Rol.CLIENTE);
        reloj.avanzar(Duration.ofMinutes(29));
        assertTrue(almacen.validar(token).isPresent());
        reloj.avanzar(Duration.ofMinutes(2));
        assertEquals(Optional.empty(), almacen.validar(token));
    }

    @Test
    void cerrarInvalidaElToken() {
        String token = almacen.crear(1, Rol.ADMIN);
        almacen.cerrar(token);
        assertEquals(Optional.empty(), almacen.validar(token));
    }

    @Test
    void tokensBasuraSeRechazan() {
        assertEquals(Optional.empty(), almacen.validar(null));
        assertEquals(Optional.empty(), almacen.validar(""));
        assertEquals(Optional.empty(), almacen.validar("inventado"));
        assertEquals(Optional.empty(), almacen.validar("x".repeat(500)));
    }
}
