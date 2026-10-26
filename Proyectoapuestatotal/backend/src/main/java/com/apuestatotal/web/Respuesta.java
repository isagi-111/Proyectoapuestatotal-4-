package com.apuestatotal.web;

/**
 * Respuesta HTTP que devuelve un controlador.
 *
 * @param estado código HTTP
 * @param cuerpo objeto que se serializa a JSON (puede ser {@code null})
 */
public record Respuesta(int estado, Object cuerpo) {

    /**
     * @param cuerpo contenido
     * @return respuesta 200
     */
    public static Respuesta ok(Object cuerpo) {
        return new Respuesta(200, cuerpo);
    }

    /**
     * @param cuerpo contenido
     * @return respuesta 201
     */
    public static Respuesta creado(Object cuerpo) {
        return new Respuesta(201, cuerpo);
    }

    /** @return respuesta 204 sin cuerpo */
    public static Respuesta sinContenido() {
        return new Respuesta(204, null);
    }
}
