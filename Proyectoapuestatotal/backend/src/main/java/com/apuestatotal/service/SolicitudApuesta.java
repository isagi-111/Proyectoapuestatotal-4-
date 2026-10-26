package com.apuestatotal.service;

import com.apuestatotal.model.Opcion;
import com.apuestatotal.model.TipoApuesta;
import java.math.BigDecimal;
import java.util.List;

/**
 * Cupón enviado por el cliente. Solo trae IDs y opciones: las cuotas SIEMPRE las toma
 * el servidor, para que nadie pueda manipular el precio de una apuesta.
 *
 * @param tipo   tipo de apuesta
 * @param monto  monto por apuesta
 * @param items  selecciones elegidas
 */
public record SolicitudApuesta(TipoApuesta tipo, BigDecimal monto, List<Item> items) {

    /**
     * Una selección del cupón.
     *
     * @param partidoId partido elegido
     * @param opcion    opción elegida
     */
    public record Item(long partidoId, Opcion opcion) {
    }
}
