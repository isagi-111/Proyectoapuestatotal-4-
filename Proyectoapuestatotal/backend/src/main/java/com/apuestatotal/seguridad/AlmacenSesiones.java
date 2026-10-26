package com.apuestatotal.seguridad;

import com.apuestatotal.model.Rol;
import java.util.Optional;

/** Guarda y valida las sesiones (tokens) de los usuarios. */
public interface AlmacenSesiones {

    /**
     * Crea una sesión nueva.
     *
     * @param usuarioId dueño
     * @param rol       rol del dueño
     * @return token opaco que se entrega al cliente
     */
    String crear(long usuarioId, Rol rol);

    /**
     * @param token token enviado por el cliente
     * @return la sesión si el token existe y no expiró
     */
    Optional<SesionActiva> validar(String token);

    /**
     * Invalida el token (cierre de sesión).
     *
     * @param token token a invalidar
     */
    void cerrar(String token);
}
