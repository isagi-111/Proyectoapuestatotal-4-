package com.apuestatotal.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Apuesta registrada por un usuario.
 *
 * @param id                 identificador (0 si aún no se guardó)
 * @param usuarioId          dueño de la apuesta
 * @param tipo               tipo de apuesta
 * @param estado             estado actual
 * @param monto              dinero simulado apostado
 * @param cuotaTotal         producto de las cuotas
 * @param gananciaPotencial  premio si se acierta todo
 * @param creadaEn           momento en que se registró
 * @param selecciones        selecciones (inmutable)
 */
public record Apuesta(long id, long usuarioId, TipoApuesta tipo, EstadoApuesta estado, BigDecimal monto,
                      BigDecimal cuotaTotal, BigDecimal gananciaPotencial, Instant creadaEn,
                      List<Seleccion> selecciones) {

    /** Copia defensiva de la lista para garantizar inmutabilidad. */
    public Apuesta {
        selecciones = List.copyOf(selecciones);
    }

    /**
     * @param nuevoId identificador asignado por la base de datos
     * @return copia con el identificador indicado
     */
    public Apuesta conId(long nuevoId) {
        return new Apuesta(nuevoId, usuarioId, tipo, estado, monto, cuotaTotal, gananciaPotencial,
                creadaEn, selecciones);
    }

    /**
     * @param nuevoEstado estado nuevo
     * @return copia con el estado indicado
     */
    public Apuesta conEstado(EstadoApuesta nuevoEstado) {
        return new Apuesta(id, usuarioId, tipo, nuevoEstado, monto, cuotaTotal, gananciaPotencial,
                creadaEn, selecciones);
    }
}
