package com.apuestatotal.excepcion;

/** Se superó el límite de intentos permitido (HTTP 429). */
public class DemasiadosIntentosException extends ApuestaTotalException {

    private static final long serialVersionUID = 1L;

    /** @param mensaje mensaje seguro de mostrar al usuario */
    public DemasiadosIntentosException(String mensaje) {
        super(429, mensaje);
    }
}
