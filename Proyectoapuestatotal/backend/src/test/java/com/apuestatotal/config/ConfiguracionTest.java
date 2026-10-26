package com.apuestatotal.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfiguracionTest {

    @Test
    void enDesarrolloUsaValoresPorDefecto() {
        Configuracion c = Configuracion.cargar(Map.of());
        assertEquals("dev", c.entorno());
        assertEquals(8080, c.puerto());
        assertTrue(c.pepper().length >= 16);
        assertEquals(0, new java.math.BigDecimal("1000.00").compareTo(c.saldoInicial()));
    }

    @Test
    void lasVariablesDeEntornoTienenPrioridad() {
        Configuracion c = Configuracion.cargar(Map.of("APUESTATOTAL_PORT", "9090", "APUESTATOTAL_ADMIN_DNI", "11111111"));
        assertEquals(9090, c.puerto());
        assertEquals("11111111", c.adminDni());
    }

    @Test
    void enProduccionExigeSecretos() {
        assertThrows(IllegalStateException.class, () -> Configuracion.cargar(Map.of("APUESTATOTAL_ENV", "prod")));
        assertThrows(IllegalStateException.class, () -> Configuracion.cargar(Map.of(
                "APUESTATOTAL_ENV", "prod", "APUESTATOTAL_PEPPER", "x".repeat(40))));
        Configuracion c = Configuracion.cargar(Map.of("APUESTATOTAL_ENV", "prod",
                "APUESTATOTAL_PEPPER", "x".repeat(40), "APUESTATOTAL_DB_PASSWORD", "clave-segura"));
        assertEquals("prod", c.entorno());
    }

    @Test
    void toStringNoFiltraSecretos() {
        Configuracion c = Configuracion.cargar(Map.of("APUESTATOTAL_ENV", "prod",
                "APUESTATOTAL_PEPPER", "pepper-super-secreto-de-produccion-123", "APUESTATOTAL_DB_PASSWORD", "clave-segura"));
        assertFalse(c.toString().contains("clave-segura"));
        assertFalse(c.toString().contains("pepper-super-secreto"));
    }
}
