package com.apuestatotal.dao.memoria;

import com.apuestatotal.dao.BilleteraDao;
import com.apuestatotal.excepcion.NoEncontradoException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** {@link BilleteraDao} en memoria. */
public final class BilleteraDaoMemoria implements BilleteraDao {

    private final Map<Long, BigDecimal> saldos = new HashMap<>();

    @Override
    public synchronized void crear(long usuarioId, BigDecimal saldoInicial) {
        saldos.put(usuarioId, saldoInicial);
    }

    @Override
    public synchronized Optional<BigDecimal> saldoDe(long usuarioId) {
        return Optional.ofNullable(saldos.get(usuarioId));
    }

    @Override
    public synchronized boolean debitar(long usuarioId, BigDecimal monto) {
        BigDecimal actual = saldos.get(usuarioId);
        if (actual == null || actual.compareTo(monto) < 0) {
            return false;
        }
        saldos.put(usuarioId, actual.subtract(monto));
        return true;
    }

    @Override
    public synchronized void acreditar(long usuarioId, BigDecimal monto) {
        BigDecimal actual = saldos.get(usuarioId);
        if (actual == null) {
            throw new NoEncontradoException("La billetera no existe.");
        }
        saldos.put(usuarioId, actual.add(monto));
    }
}
