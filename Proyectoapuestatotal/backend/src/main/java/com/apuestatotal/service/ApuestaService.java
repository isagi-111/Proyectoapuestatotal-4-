package com.apuestatotal.service;

import com.apuestatotal.model.Apuesta;
import com.apuestatotal.model.EstadoApuesta;
import java.util.List;
import java.util.Optional;

/** Casos de uso de apuestas. */
public interface ApuestaService {

    /**
     * Registra el cupón: valida, descuenta el saldo simulado y guarda las apuestas en una
     * sola transacción.
     *
     * @param usuarioId dueño
     * @param solicitud cupón
     * @return apuestas creadas
     */
    List<Apuesta> colocar(long usuarioId, SolicitudApuesta solicitud);

    /**
     * @param usuarioId dueño
     * @param estado    filtro opcional por estado
     * @return apuestas del usuario (más nuevas primero)
     */
    List<Apuesta> listar(long usuarioId, Optional<EstadoApuesta> estado);
}
