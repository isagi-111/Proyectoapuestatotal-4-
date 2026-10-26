package com.apuestatotal.dao.jdbc;

import com.apuestatotal.dao.BilleteraDao;
import com.apuestatotal.excepcion.NoEncontradoException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

/** {@link BilleteraDao} sobre JDBC. El débito es atómico gracias a la condición en el UPDATE. */
public final class BilleteraDaoJdbc implements BilleteraDao {

    private final GestorConexiones gestor;

    /** @param gestor gestor de conexiones/transacciones */
    public BilleteraDaoJdbc(GestorConexiones gestor) {
        this.gestor = gestor;
    }

    @Override
    public void crear(long usuarioId, BigDecimal saldoInicial) {
        gestor.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO billetera (usuario_id, saldo) VALUES (?, ?)")) {
                ps.setLong(1, usuarioId);
                ps.setBigDecimal(2, saldoInicial);
                return ps.executeUpdate();
            }
        });
    }

    @Override
    public Optional<BigDecimal> saldoDe(long usuarioId) {
        return gestor.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT saldo FROM billetera WHERE usuario_id = ?")) {
                ps.setLong(1, usuarioId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(rs.getBigDecimal(1)) : Optional.<BigDecimal>empty();
                }
            }
        });
    }

    @Override
    public boolean debitar(long usuarioId, BigDecimal monto) {
        return gestor.conConexion(c -> {
            String sql = "UPDATE billetera SET saldo = saldo - ? WHERE usuario_id = ? AND saldo >= ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setBigDecimal(1, monto);
                ps.setLong(2, usuarioId);
                ps.setBigDecimal(3, monto);
                return ps.executeUpdate() == 1;
            }
        });
    }

    @Override
    public void acreditar(long usuarioId, BigDecimal monto) {
        int filas = gestor.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE billetera SET saldo = saldo + ? WHERE usuario_id = ?")) {
                ps.setBigDecimal(1, monto);
                ps.setLong(2, usuarioId);
                return ps.executeUpdate();
            }
        });
        if (filas != 1) {
            throw new NoEncontradoException("La billetera no existe.");
        }
    }
}
