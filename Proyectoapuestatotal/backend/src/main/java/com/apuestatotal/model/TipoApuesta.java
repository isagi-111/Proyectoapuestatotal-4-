package com.apuestatotal.model;

import com.apuestatotal.excepcion.ValidacionException;

/** Tipos de apuesta que maneja el cupón. */
public enum TipoApuesta {
    /** Cada selección es una apuesta independiente. */
    SIMPLE,
    /** Varias selecciones en una sola apuesta (las cuotas se multiplican). */
    MULTIPLE,
    /** Reservado para una versión futura. */
    SISTEMA;

    /**
     * Convierte texto a tipo de apuesta.
     *
     * @param texto valor recibido del cliente
     * @return el tipo correspondiente
     * @throws ValidacionException si es nulo o desconocido
     */
    public static TipoApuesta deTexto(String texto) {
        if (texto == null) {
            throw new ValidacionException("El tipo de apuesta es obligatorio.");
        }
        try {
            return valueOf(texto.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ValidacionException("Tipo de apuesta inválido. Usa SIMPLE, MULTIPLE o SISTEMA.");
        }
    }
}
