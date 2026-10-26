package com.apuestatotal.excepcion;

/** El saldo simulado no alcanza para la apuesta (HTTP 422). */
public class SaldoInsuficienteException extends ApuestaTotalException {

    private static final long serialVersionUID = 1L;

    /** @param mensaje mensaje seguro de mostrar al usuario */
    public SaldoInsuficienteException(String mensaje) {
        super(422, mensaje);
    }
}
