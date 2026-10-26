package com.apuestatotal.model;

import com.apuestatotal.excepcion.ValidacionException;

/** Opción del mercado 1X2 sobre la que se apuesta. */
public enum Opcion {
    /** Gana el equipo local. */
    LOCAL,
    /** Empate. */
    EMPATE,
    /** Gana el equipo visitante. */
    VISITA;

    /**
     * Convierte texto a opción de forma estricta (sin aceptar valores desconocidos).
     *
     * @param texto valor recibido del cliente
     * @return la opción correspondiente
     * @throws ValidacionException si el texto es nulo o no existe
     */
    public static Opcion deTexto(String texto) {
        if (texto == null) {
            throw new ValidacionException("La opción es obligatoria (LOCAL, EMPATE o VISITA).");
        }
        try {
            return valueOf(texto.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ValidacionException("Opción inválida. Usa LOCAL, EMPATE o VISITA.");
        }
    }

    /**
     * Calcula la opción ganadora a partir del marcador final.
     *
     * @param golesLocal  goles del local
     * @param golesVisita goles del visitante
     * @return la opción que acertó
     */
    public static Opcion deResultado(int golesLocal, int golesVisita) {
        if (golesLocal > golesVisita) {
            return LOCAL;
        }
        return golesLocal == golesVisita ? EMPATE : VISITA;
    }
}
