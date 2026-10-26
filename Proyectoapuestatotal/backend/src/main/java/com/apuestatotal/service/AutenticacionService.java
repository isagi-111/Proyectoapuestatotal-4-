package com.apuestatotal.service;

import com.apuestatotal.model.Usuario;
import com.apuestatotal.seguridad.SesionActiva;
import java.util.Optional;

/** Registro e inicio de sesión únicamente con DNI. */
public interface AutenticacionService {

    /**
     * Registra un usuario nuevo con su DNI y le crea la billetera con dinero simulado.
     *
     * @param dni DNI de 8 dígitos
     * @param ip  IP del cliente (para limitar registros masivos)
     * @return el usuario creado
     */
    Usuario registrar(String dni, String ip);

    /**
     * Inicia sesión con el DNI.
     *
     * @param dni DNI de 8 dígitos
     * @param ip  IP del cliente (para limitar intentos fallidos)
     * @return datos de la sesión nueva
     */
    SesionIniciada iniciarSesion(String dni, String ip);

    /**
     * Cierra la sesión.
     *
     * @param token token a invalidar
     */
    void cerrarSesion(String token);

    /**
     * @param token token recibido
     * @return la sesión si es válida
     */
    Optional<SesionActiva> validar(String token);

    /**
     * Crea el usuario administrador si todavía no existe (se usa al arrancar).
     *
     * @param dni DNI del administrador
     */
    void asegurarAdministrador(String dni);
}
