package com.apuestatotal.seguridad;

import com.apuestatotal.model.Rol;
import java.time.Instant;

/**
 * Sesión autenticada.
 *
 * @param usuarioId dueño de la sesión
 * @param rol       rol del usuario
 * @param expira    momento en que deja de ser válida
 */
public record SesionActiva(long usuarioId, Rol rol, Instant expira) {
}
