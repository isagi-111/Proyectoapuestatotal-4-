package com.apuestatotal.dao;

import java.util.function.Supplier;

/**
 * Abstracción de una transacción. Los servicios (capa de negocio) la usan para que varias
 * operaciones de DAO se confirmen o se deshagan juntas, SIN conocer JDBC (inversión de
 * dependencias).
 */
public interface UnidadDeTrabajo {

    /**
     * Ejecuta el trabajo dentro de una transacción. Si lanza una excepción se hace rollback.
     * Si ya hay una transacción activa en el hilo, se reutiliza.
     *
     * @param trabajo operación a ejecutar
     * @param <T>     tipo del resultado
     * @return el resultado del trabajo
     */
    <T> T ejecutar(Supplier<T> trabajo);

    /**
     * Variante sin resultado.
     *
     * @param trabajo operación a ejecutar
     */
    default void ejecutarSinResultado(Runnable trabajo) {
        ejecutar(() -> {
            trabajo.run();
            return null;
        });
    }
}
