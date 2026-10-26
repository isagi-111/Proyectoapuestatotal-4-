package com.apuestatotal.service;

import com.apuestatotal.dao.BilleteraDao;
import com.apuestatotal.dao.UnidadDeTrabajo;
import com.apuestatotal.excepcion.NoEncontradoException;
import com.apuestatotal.excepcion.ValidacionException;
import java.math.BigDecimal;

/** Implementación de {@link BilleteraService}. */
public final class BilleteraServiceImpl implements BilleteraService {

    /** Recarga mínima permitida. */
    public static final BigDecimal RECARGA_MINIMA = new BigDecimal("0.01");
    /** Recarga máxima permitida por operación. */
    public static final BigDecimal RECARGA_MAXIMA = new BigDecimal("10000.00");

    private final BilleteraDao billeteras;
    private final UnidadDeTrabajo transaccion;

    /**
     * @param billeteras  DAO de billeteras
     * @param transaccion unidad de trabajo
     */
    public BilleteraServiceImpl(BilleteraDao billeteras, UnidadDeTrabajo transaccion) {
        this.billeteras = billeteras;
        this.transaccion = transaccion;
    }

    @Override
    public BigDecimal saldo(long usuarioId) {
        return billeteras.saldoDe(usuarioId).orElseThrow(() -> new NoEncontradoException("La billetera no existe."));
    }

    @Override
    public BigDecimal recargar(long usuarioId, BigDecimal monto) {
        if (monto == null) {
            throw new ValidacionException("El monto es obligatorio.");
        }
        if (monto.stripTrailingZeros().scale() > 2) {
            throw new ValidacionException("El monto admite como máximo 2 decimales.");
        }
        BigDecimal normalizado = monto.setScale(2, java.math.RoundingMode.UNNECESSARY);
        if (normalizado.compareTo(RECARGA_MINIMA) < 0 || normalizado.compareTo(RECARGA_MAXIMA) > 0) {
            throw new ValidacionException("La recarga debe estar entre S/ 0.01 y S/ 10000.00.");
        }
        return transaccion.ejecutar(() -> {
            billeteras.acreditar(usuarioId, normalizado);
            return saldo(usuarioId);
        });
    }
}
