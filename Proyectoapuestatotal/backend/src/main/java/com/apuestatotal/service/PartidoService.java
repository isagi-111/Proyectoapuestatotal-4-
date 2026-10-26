package com.apuestatotal.service;

import com.apuestatotal.model.Partido;
import java.util.List;

/** Casos de uso de partidos. */
public interface PartidoService {

    /** @return partidos disponibles */
    List<Partido> listar();

    /**
     * Finaliza un partido con su marcador y liquida (gana/pierde y paga) las apuestas afectadas.
     *
     * @param partidoId   partido
     * @param golesLocal  goles del local
     * @param golesVisita goles del visitante
     * @return resumen de la liquidación
     */
    ResultadoLiquidacion finalizar(long partidoId, Integer golesLocal, Integer golesVisita);
}
