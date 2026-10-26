package com.apuestatotal.vista;

import java.math.BigDecimal;
import java.util.List;

/** DTOs de ENTRADA (lo que envía el cliente). Se validan en los servicios. */
public final class Solicitudes {

    private Solicitudes() {
    }

    /**
     * @param dni DNI de 8 dígitos
     */
    public record Dni(String dni) {
    }

    /**
     * @param monto monto a recargar
     */
    public record Recarga(BigDecimal monto) {
    }

    /**
     * @param tipo        SIMPLE, MULTIPLE o SISTEMA
     * @param monto       monto por apuesta
     * @param selecciones selecciones del cupón
     */
    public record Cupon(String tipo, BigDecimal monto, List<Seleccion> selecciones) {
    }

    /**
     * @param partidoId partido elegido
     * @param opcion    LOCAL, EMPATE o VISITA
     */
    public record Seleccion(Long partidoId, String opcion) {
    }

    /**
     * @param golesLocal  goles del local
     * @param golesVisita goles del visitante
     */
    public record Resultado(Integer golesLocal, Integer golesVisita) {
    }
}
