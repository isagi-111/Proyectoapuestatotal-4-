package com.apuestatotal.excepcion;

/**
 * Excepción base de la aplicación. Lleva el código HTTP que la capa web debe
 * devolver, de modo que el controlador no necesita un {@code if} por cada error
 * (principio abierto/cerrado).
 */
public class ApuestaTotalException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int codigoHttp;

    /**
     * @param codigoHttp código HTTP asociado al error
     * @param mensaje    mensaje seguro de mostrar al usuario (sin datos internos)
     */
    public ApuestaTotalException(int codigoHttp, String mensaje) {
        super(mensaje);
        this.codigoHttp = codigoHttp;
    }

    /**
     * @param codigoHttp código HTTP asociado al error
     * @param mensaje    mensaje seguro de mostrar al usuario
     * @param causa      causa técnica (solo se registra en el log, nunca se envía al cliente)
     */
    public ApuestaTotalException(int codigoHttp, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.codigoHttp = codigoHttp;
    }

    /** @return código HTTP que corresponde a este error */
    public int codigoHttp() {
        return codigoHttp;
    }
}
