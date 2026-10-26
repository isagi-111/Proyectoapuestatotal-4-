package com.apuestatotal.dao.memoria;

import com.apuestatotal.dao.UsuarioDao;
import com.apuestatotal.excepcion.ConflictoException;
import com.apuestatotal.model.Usuario;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** {@link UsuarioDao} en memoria. */
public final class UsuarioDaoMemoria implements UsuarioDao {

    private final Map<Long, Usuario> porId = new ConcurrentHashMap<>();
    private final AtomicLong secuencia = new AtomicLong();

    @Override
    public synchronized Usuario guardar(Usuario usuario) {
        if (buscarPorDniHash(usuario.dniHash()).isPresent()) {
            throw new ConflictoException("El DNI ya está registrado.");
        }
        Usuario guardado = usuario.conId(secuencia.incrementAndGet());
        porId.put(guardado.id(), guardado);
        return guardado;
    }

    @Override
    public Optional<Usuario> buscarPorId(long id) {
        return Optional.ofNullable(porId.get(id));
    }

    @Override
    public Optional<Usuario> buscarPorDniHash(String dniHash) {
        return porId.values().stream().filter(u -> u.dniHash().equals(dniHash)).findFirst();
    }
}
