package com.apuestatotal.dao;

import com.apuestatotal.model.Partido;
import java.util.List;
import java.util.Optional;

/** Acceso a datos de partidos. */
public interface PartidoDao {

    /**
     * @param partido partido sin id
     * @return el partido con su id asignado
     */
    Partido guardar(Partido partido);

    /** @return todos los partidos ordenados por inicio */
    List<Partido> listar();

    /**
     * @param id identificador
     * @return el partido, si existe
     */
    Optional<Partido> buscarPorId(long id);

    /**
     * Actualiza estado, marcador y cuotas de un partido existente.
     *
     * @param partido partido con los datos nuevos
     */
    void actualizar(Partido partido);
}
