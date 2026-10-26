package com.apuestatotal.excepcion;

/** La operación entra en conflicto con el estado actual (HTTP 409). */
public class ConflictoException extends ApuestaTotalException {

    private static final long serialVersionUID = 1L;

    /** @param mensaje mensaje seguro de mostrar al usuario */
    public ConflictoException(String mensaje) {
        super(409, mensaje);
    }
}
