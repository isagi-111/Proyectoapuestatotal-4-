package com.apuestatotal.dao.memoria;

import com.apuestatotal.dao.ApuestaDao;
import com.apuestatotal.excepcion.NoEncontradoException;
import com.apuestatotal.model.Apuesta;
import com.apuestatotal.model.EstadoApuesta;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** {@link ApuestaDao} en memoria. */
public final class ApuestaDaoMemoria implements ApuestaDao {

    private final Map<Long, Apuesta> porId = new ConcurrentHashMap<>();
    private final AtomicLong secuencia = new AtomicLong();

    @Override
    public Apuesta guardar(Apuesta apuesta) {
        Apuesta guardada = apuesta.conId(secuencia.incrementAndGet());
        porId.put(guardada.id(), guardada);
        return guardada;
    }

    @Override
    public List<Apuesta> listarPorUsuario(long usuarioId) {
        return porId.values().stream()
                .filter(a -> a.usuarioId() == usuarioId)
                .sorted(Comparator.comparingLong(Apuesta::id).reversed())
                .toList();
    }

    @Override
    public List<Apuesta> listarPendientesPorPartido(long partidoId) {
        return porId.values().stream()
                .filter(a -> a.estado() == EstadoApuesta.PENDIENTE)
                .filter(a -> a.selecciones().stream().anyMatch(s -> s.partidoId() == partidoId))
                .sorted(Comparator.comparingLong(Apuesta::id))
                .toList();
    }

    @Override
    public void actualizarEstado(long apuestaId, EstadoApuesta estado) {
        Apuesta actual = porId.get(apuestaId);
        if (actual == null) {
            throw new NoEncontradoException("La apuesta no existe.");
        }
        porId.put(apuestaId, actual.conEstado(estado));
    }
}
