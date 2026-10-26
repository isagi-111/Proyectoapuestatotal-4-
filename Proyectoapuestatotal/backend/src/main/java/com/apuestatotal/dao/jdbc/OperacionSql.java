package com.apuestatotal.dao.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Operación que usa una conexión JDBC y puede lanzar {@link SQLException}.
 *
 * @param <T> tipo del resultado
 */
@FunctionalInterface
public interface OperacionSql<T> {

    /**
     * @param conexion conexión a usar
     * @return resultado de la operación
     * @throws SQLException ante un error SQL
     */
    T aplicar(Connection conexion) throws SQLException;
}
