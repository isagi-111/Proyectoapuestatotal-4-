package com.apuestatotal.excepcion;

/** Falta autenticación o la sesión no es válida (HTTP 401). */
public class NoAutenticadoException extends ApuestaTotalException {

    private static final long serialVersionUID = 1L;

    /** @param mensaje mensaje seguro de mostrar al usuario */
    public NoAutenticadoException(String mensaje) {
        super(401, mensaje);
    }
}
