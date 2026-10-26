package com.apuestatotal.excepcion;

/** Datos de entrada inválidos (HTTP 400). */
public class ValidacionException extends ApuestaTotalException {

    private static final long serialVersionUID = 1L;

    /** @param mensaje mensaje seguro de mostrar al usuario */
    public ValidacionException(String mensaje) {
        super(400, mensaje);
    }
}
