package com.apuestatotal.dao;

import com.apuestatotal.model.Usuario;
import java.util.Optional;

/** Acceso a datos de usuarios (patrón DAO). */
public interface UsuarioDao {

    /**
     * Guarda un usuario nuevo.
     *
     * @param usuario usuario sin id
     * @return el usuario con su id asignado
     * @throws com.apuestatotal.excepcion.ConflictoException si el DNI ya existe
     */
    Usuario guardar(Usuario usuario);

    /**
     * @param id identificador
     * @return el usuario, si existe
     */
    Optional<Usuario> buscarPorId(long id);

    /**
     * @param dniHash huella HMAC del DNI
     * @return el usuario, si existe
     */
    Optional<Usuario> buscarPorDniHash(String dniHash);
}
