package com.apuestatotal.service;

import java.math.BigDecimal;

/** Consulta y recarga del saldo SIMULADO. */
public interface BilleteraService {

    /**
     * @param usuarioId dueño
     * @return saldo actual
     */
    BigDecimal saldo(long usuarioId);

    /**
     * Agrega dinero simulado.
     *
     * @param usuarioId dueño
     * @param monto     monto a agregar (entre 0.01 y 10000.00)
     * @return saldo resultante
     */
    BigDecimal recargar(long usuarioId, BigDecimal monto);
}
