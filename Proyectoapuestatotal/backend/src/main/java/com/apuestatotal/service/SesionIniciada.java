package com.apuestatotal.service;

import com.apuestatotal.model.Rol;
import java.math.BigDecimal;

/**
 * Resultado de un inicio de sesión correcto.
 *
 * @param token       token de sesión (se envía en {@code Authorization: Bearer})
 * @param usuarioId   id del usuario
 * @param dniMascara  DNI parcialmente oculto
 * @param rol         rol del usuario
 * @param saldo       saldo simulado actual
 */
public record SesionIniciada(String token, long usuarioId, String dniMascara, Rol rol, BigDecimal saldo) {
}
