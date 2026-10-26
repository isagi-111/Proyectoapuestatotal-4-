package com.apuestatotal.dao;

import com.apuestatotal.model.Apuesta;
import com.apuestatotal.model.EstadoApuesta;
import java.util.List;

/** Acceso a datos de apuestas y sus selecciones. */
public interface ApuestaDao {

    /**
     * @param apuesta apuesta sin id
     * @return la apuesta con su id asignado
     */
    Apuesta guardar(Apuesta apuesta);

    /**
     * @param usuarioId dueño
     * @return apuestas del usuario, de la más nueva a la más antigua
     */
    List<Apuesta> listarPorUsuario(long usuarioId);

    /**
     * @param partidoId partido
     * @return apuestas PENDIENTES que tienen una selección en ese partido
     */
    List<Apuesta> listarPendientesPorPartido(long partidoId);

    /**
     * @param apuestaId identificador
     * @param estado    estado nuevo
     */
    void actualizarEstado(long apuestaId, EstadoApuesta estado);
}
