package com.apuestatotal.dao.jdbc;

import com.apuestatotal.dao.UnidadDeTrabajo;
import com.apuestatotal.excepcion.AccesoDatosException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementación JDBC de {@link UnidadDeTrabajo}. Mantiene la conexión de la transacción
 * activa en un {@link ThreadLocal}; así los DAO comparten la misma conexión sin que el
 * servicio tenga que pasarla (separación de capas).
 */
public final class GestorConexiones implements UnidadDeTrabajo {

    private static final Logger LOG = Logger.getLogger(GestorConexiones.class.getName());

    private final FabricaConexiones fabrica;
    private final ThreadLocal<Connection> actual = new ThreadLocal<>();

    /** @param fabrica origen de las conexiones */
    public GestorConexiones(FabricaConexiones fabrica) {
        this.fabrica = fabrica;
    }

    @Override
    public <T> T ejecutar(Supplier<T> trabajo) {
        if (actual.get() != null) {
            return trabajo.get(); // transacción ya abierta: se reutiliza
        }
        Connection conexion = null;
        boolean confirmada = false;
        try {
            conexion = fabrica.abrir();
            conexion.setAutoCommit(false);
            actual.set(conexion);
            T resultado = trabajo.get();
            conexion.commit();
            confirmada = true;
            return resultado;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error SQL en la transacción", e);
            throw new AccesoDatosException("Error de acceso a datos.", e);
        } finally {
            actual.remove();
            if (conexion != null) {
                if (!confirmada) {
                    deshacer(conexion);
                }
                cerrar(conexion);
            }
        }
    }

    /**
     * Ejecuta una operación SQL con la conexión de la transacción activa o, si no hay
     * ninguna, con una conexión nueva que se cierra al terminar (try-with-resources).
     *
     * @param operacion operación SQL
     * @param <T>       tipo del resultado
     * @return resultado de la operación
     */
    public <T> T conConexion(OperacionSql<T> operacion) {
        Connection enCurso = actual.get();
        try {
            if (enCurso != null) {
                return operacion.aplicar(enCurso);
            }
            try (Connection nueva = fabrica.abrir()) {
                return operacion.aplicar(nueva);
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error SQL (estado " + e.getSQLState() + ")", e);
            throw new AccesoDatosException("Error de acceso a datos.", e);
        }
    }

    private static void deshacer(Connection conexion) {
        try {
            conexion.rollback();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "No se pudo hacer rollback", e);
        }
    }

    private static void cerrar(Connection conexion) {
        try {
            conexion.close();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "No se pudo cerrar la conexión", e);
        }
    }
}
