package com.apuestatotal.excepcion;

/**
 * Error técnico al acceder a la base de datos (HTTP 500). El mensaje es genérico a
 * propósito: el detalle SQL queda solo en la causa y se escribe en el log del servidor.
 */
public class AccesoDatosException extends ApuestaTotalException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje mensaje genérico
     * @param causa   excepción SQL original
     */
    public AccesoDatosException(String mensaje, Throwable causa) {
        super(500, mensaje, causa);
    }
}
