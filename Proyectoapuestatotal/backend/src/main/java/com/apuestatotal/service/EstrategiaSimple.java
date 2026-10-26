package com.apuestatotal.service;

import com.apuestatotal.model.Apuesta;
import com.apuestatotal.model.EstadoApuesta;
import com.apuestatotal.model.Seleccion;
import com.apuestatotal.model.TipoApuesta;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/** Apuesta SIMPLE: cada selección es una apuesta independiente con el mismo monto. */
public final class EstrategiaSimple implements EstrategiaApuesta {

    @Override
    public TipoApuesta tipo() {
        return TipoApuesta.SIMPLE;
    }

    @Override
    public List<Apuesta> construir(long usuarioId, BigDecimal monto, List<Seleccion> selecciones, Instant ahora) {
        return selecciones.stream()
                .map(s -> new Apuesta(0, usuarioId, TipoApuesta.SIMPLE, EstadoApuesta.PENDIENTE, monto,
                        s.cuota().setScale(2, RoundingMode.HALF_UP),
                        monto.multiply(s.cuota()).setScale(2, RoundingMode.HALF_UP), ahora, List.of(s)))
                .toList();
    }
}
