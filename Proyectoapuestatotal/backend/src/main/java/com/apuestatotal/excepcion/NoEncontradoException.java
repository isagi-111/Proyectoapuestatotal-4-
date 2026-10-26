package com.apuestatotal.excepcion;

/** El recurso solicitado no existe (HTTP 404). */
public class NoEncontradoException extends ApuestaTotalException {

    private static final long serialVersionUID = 1L;

    /** @param mensaje mensaje seguro de mostrar al usuario */
    public NoEncontradoException(String mensaje) {
        super(404, mensaje);
    }
}
