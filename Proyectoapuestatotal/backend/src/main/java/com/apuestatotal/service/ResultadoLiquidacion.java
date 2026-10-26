package com.apuestatotal.service;

import com.apuestatotal.model.Partido;

/**
 * Resumen de la liquidación al finalizar un partido.
 *
 * @param partido     partido ya finalizado
 * @param ganadas     apuestas que pasaron a GANADA (y se pagaron)
 * @param perdidas    apuestas que pasaron a PERDIDA
 * @param pendientes  apuestas que siguen esperando otros partidos
 */
public record ResultadoLiquidacion(Partido partido, int ganadas, int perdidas, int pendientes) {
}
