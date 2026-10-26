package com.apuestatotal.excepcion;

/** El usuario no tiene permisos para la operación (HTTP 403). */
public class PermisoDenegadoException extends ApuestaTotalException {

    private static final long serialVersionUID = 1L;

    /** @param mensaje mensaje seguro de mostrar al usuario */
    public PermisoDenegadoException(String mensaje) {
        super(403, mensaje);
    }
}
