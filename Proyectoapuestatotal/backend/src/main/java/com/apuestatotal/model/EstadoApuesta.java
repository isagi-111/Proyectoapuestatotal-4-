package com.apuestatotal.model;

import com.apuestatotal.excepcion.ValidacionException;

/** Estado de una apuesta registrada. */
public enum EstadoApuesta {
    /** Aún hay partidos sin terminar. */
    PENDIENTE,
    /** Todas las selecciones acertaron. */
    GANADA,
    /** Alguna selección falló. */
    PERDIDA;

    /**
     * Convierte texto a estado.
     *
     * @param texto valor recibido del cliente
     * @return el estado correspondiente
     * @throws ValidacionException si es desconocido
     */
    public static EstadoApuesta deTexto(String texto) {
        try {
            return valueOf(texto.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ValidacionException("Estado inválido. Usa PENDIENTE, GANADA o PERDIDA.");
        }
    }
}
