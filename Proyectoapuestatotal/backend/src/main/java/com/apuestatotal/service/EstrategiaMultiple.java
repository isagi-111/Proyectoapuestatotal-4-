package com.apuestatotal.service;

import com.apuestatotal.excepcion.ValidacionException;
import com.apuestatotal.model.Apuesta;
import com.apuestatotal.model.EstadoApuesta;
import com.apuestatotal.model.Seleccion;
import com.apuestatotal.model.TipoApuesta;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/** Apuesta MÚLTIPLE: una sola apuesta cuya cuota es el producto de todas las cuotas. */
public final class EstrategiaMultiple implements EstrategiaApuesta {

    @Override
    public TipoApuesta tipo() {
        return TipoApuesta.MULTIPLE;
    }

    @Override
    public List<Apuesta> construir(long usuarioId, BigDecimal monto, List<Seleccion> selecciones, Instant ahora) {
        if (selecciones.size() < 2) {
            throw new ValidacionException("Una apuesta múltiple necesita al menos 2 selecciones.");
        }
        BigDecimal producto = selecciones.stream().map(Seleccion::cuota).reduce(BigDecimal.ONE, BigDecimal::multiply);
        return List.of(new Apuesta(0, usuarioId, TipoApuesta.MULTIPLE, EstadoApuesta.PENDIENTE, monto,
                producto.setScale(2, RoundingMode.HALF_UP),
                monto.multiply(producto).setScale(2, RoundingMode.HALF_UP), ahora, selecciones));
    }
}
