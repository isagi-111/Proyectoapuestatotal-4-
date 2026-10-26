package com.apuestatotal.dao;

import java.math.BigDecimal;
import java.util.Optional;

/** Acceso a datos de la billetera (saldo simulado). */
public interface BilleteraDao {

    /**
     * Crea la billetera de un usuario.
     *
     * @param usuarioId  dueño
     * @param saldoInicial saldo de partida
     */
    void crear(long usuarioId, BigDecimal saldoInicial);

    /**
     * @param usuarioId dueño
     * @return saldo actual, o vacío si no existe la billetera
     */
    Optional<BigDecimal> saldoDe(long usuarioId);

    /**
     * Descuenta saldo de forma ATÓMICA: solo si alcanza.
     *
     * @param usuarioId dueño
     * @param monto     monto a descontar
     * @return {@code true} si se descontó; {@code false} si el saldo no alcanzaba
     */
    boolean debitar(long usuarioId, BigDecimal monto);

    /**
     * Suma saldo.
     *
     * @param usuarioId dueño
     * @param monto     monto a sumar
     * @throws com.apuestatotal.excepcion.NoEncontradoException si no existe la billetera
     */
    void acreditar(long usuarioId, BigDecimal monto);
}
