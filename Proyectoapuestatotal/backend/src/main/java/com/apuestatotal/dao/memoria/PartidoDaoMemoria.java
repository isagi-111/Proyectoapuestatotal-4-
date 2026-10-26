package com.apuestatotal.dao.memoria;

import com.apuestatotal.dao.PartidoDao;
import com.apuestatotal.excepcion.NoEncontradoException;
import com.apuestatotal.model.Partido;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** {@link PartidoDao} en memoria. */
public final class PartidoDaoMemoria implements PartidoDao {

    private final Map<Long, Partido> porId = new ConcurrentHashMap<>();
    private final AtomicLong secuencia = new AtomicLong();

    @Override
    public Partido guardar(Partido partido) {
        Partido guardado = partido.conId(secuencia.incrementAndGet());
        porId.put(guardado.id(), guardado);
        return guardado;
    }

    @Override
    public List<Partido> listar() {
        return porId.values().stream()
                .sorted(Comparator.comparing(Partido::inicio).thenComparingLong(Partido::id))
                .toList();
    }

    @Override
    public Optional<Partido> buscarPorId(long id) {
        return Optional.ofNullable(porId.get(id));
    }

    @Override
    public void actualizar(Partido partido) {
        if (porId.replace(partido.id(), partido) == null) {
            throw new NoEncontradoException("El partido no existe.");
        }
    }
}
