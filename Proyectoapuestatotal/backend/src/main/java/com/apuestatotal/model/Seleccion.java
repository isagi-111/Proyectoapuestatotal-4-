package com.apuestatotal.model;

import java.math.BigDecimal;

/**
 * Selección dentro de una apuesta. La cuota queda "congelada" al momento de apostar.
 *
 * @param partidoId   partido elegido
 * @param opcion      opción elegida
 * @param cuota       cuota vigente cuando se apostó
 * @param descripcion texto "Local vs Visita" (para mostrar en el historial)
 */
public record Seleccion(long partidoId, Opcion opcion, BigDecimal cuota, String descripcion) {
}
