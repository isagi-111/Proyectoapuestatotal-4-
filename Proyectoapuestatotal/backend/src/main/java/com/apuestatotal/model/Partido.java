package com.apuestatotal.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * Partido con su mercado 1X2.
 *
 * @param id           identificador (0 si aún no se guardó)
 * @param liga         competición
 * @param equipoLocal  nombre del local
 * @param equipoVisita nombre del visitante
 * @param inicio       fecha y hora de inicio
 * @param estado       estado actual
 * @param cuotaLocal   cuota del local
 * @param cuotaEmpate  cuota del empate
 * @param cuotaVisita  cuota del visitante
 * @param golesLocal   goles finales del local (solo si está finalizado)
 * @param golesVisita  goles finales del visitante (solo si está finalizado)
 */
public record Partido(long id, String liga, String equipoLocal, String equipoVisita, Instant inicio,
                      EstadoPartido estado, BigDecimal cuotaLocal, BigDecimal cuotaEmpate,
                      BigDecimal cuotaVisita, Integer golesLocal, Integer golesVisita) {

    /**
     * @param nuevoId identificador asignado por la base de datos
     * @return copia con el identificador indicado
     */
    public Partido conId(long nuevoId) {
        return new Partido(nuevoId, liga, equipoLocal, equipoVisita, inicio, estado,
                cuotaLocal, cuotaEmpate, cuotaVisita, golesLocal, golesVisita);
    }

    /**
     * @param opcion opción 1X2
     * @return la cuota vigente para esa opción
     */
    public BigDecimal cuota(Opcion opcion) {
        return switch (opcion) {
            case LOCAL -> cuotaLocal;
            case EMPATE -> cuotaEmpate;
            case VISITA -> cuotaVisita;
        };
    }

    /** @return texto "Local vs Visita" */
    public String descripcion() {
        return equipoLocal + " vs " + equipoVisita;
    }

    /** @return {@code true} si todavía se puede apostar a este partido */
    public boolean apostable() {
        return estado != EstadoPartido.FINALIZADO;
    }

    /**
     * Devuelve el partido terminado con su marcador (los objetos del modelo son inmutables).
     *
     * @param gl goles del local
     * @param gv goles del visitante
     * @return copia finalizada
     */
    public Partido finalizado(int gl, int gv) {
        return new Partido(id, liga, equipoLocal, equipoVisita, inicio, EstadoPartido.FINALIZADO,
                cuotaLocal, cuotaEmpate, cuotaVisita, gl, gv);
    }

    /** @return la opción ganadora, o vacío si el partido no terminó */
    public Optional<Opcion> resultado() {
        if (estado != EstadoPartido.FINALIZADO || golesLocal == null || golesVisita == null) {
            return Optional.empty();
        }
        return Optional.of(Opcion.deResultado(golesLocal, golesVisita));
    }
}
