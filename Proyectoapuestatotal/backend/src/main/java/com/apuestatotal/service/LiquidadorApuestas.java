package com.apuestatotal.service;

import com.apuestatotal.model.Apuesta;
import com.apuestatotal.model.EstadoApuesta;
import com.apuestatotal.model.Partido;
import com.apuestatotal.model.Seleccion;
import java.util.Optional;
import java.util.function.LongFunction;

/**
 * Decide si una apuesta quedó ganada, perdida o sigue pendiente. Clase con una única
 * responsabilidad (SRP): solo evalúa; guardar y pagar lo hace el servicio.
 */
public final class LiquidadorApuestas {

    /**
     * Evalúa una apuesta con el estado actual de sus partidos.
     *
     * @param apuesta  apuesta a evaluar
     * @param buscador función que devuelve un partido por id
     * @return PERDIDA si alguna selección falló; GANADA si todas acertaron; PENDIENTE si falta algún partido
     */
    public EstadoApuesta evaluar(Apuesta apuesta, LongFunction<Optional<Partido>> buscador) {
        boolean faltan = false;
        for (Seleccion s : apuesta.selecciones()) {
            Optional<com.apuestatotal.model.Opcion> resultado = buscador.apply(s.partidoId()).flatMap(Partido::resultado);
            if (resultado.isEmpty()) {
                faltan = true;
            } else if (resultado.get() != s.opcion()) {
                return EstadoApuesta.PERDIDA;
            }
        }
        return faltan ? EstadoApuesta.PENDIENTE : EstadoApuesta.GANADA;
    }
}
