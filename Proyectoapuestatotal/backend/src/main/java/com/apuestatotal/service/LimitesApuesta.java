package com.apuestatotal.service;

import java.math.BigDecimal;

/**
 * Reglas de negocio configurables para apostar.
 *
 * @param montoMinimo         monto mínimo por apuesta
 * @param montoMaximo         monto máximo por apuesta
 * @param maxSelecciones      selecciones máximas por cupón
 * @param gananciaMaxima      tope de ganancia potencial por apuesta
 */
public record LimitesApuesta(BigDecimal montoMinimo, BigDecimal montoMaximo, int maxSelecciones,
                             BigDecimal gananciaMaxima) {

    /** @return límites por defecto del proyecto */
    public static LimitesApuesta porDefecto() {
        return new LimitesApuesta(new BigDecimal("1.00"), new BigDecimal("5000.00"), 10,
                new BigDecimal("1000000.00"));
    }
}
