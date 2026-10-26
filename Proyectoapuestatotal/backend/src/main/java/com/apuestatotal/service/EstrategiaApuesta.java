package com.apuestatotal.service;

import com.apuestatotal.model.Apuesta;
import com.apuestatotal.model.Seleccion;
import com.apuestatotal.model.TipoApuesta;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Estrategia para construir apuestas de un tipo concreto (patrón Strategy). Agregar un
 * tipo nuevo (por ejemplo SISTEMA) significa crear otra clase, sin tocar el servicio
 * (principio abierto/cerrado).
 */
public interface EstrategiaApuesta {

    /** @return tipo de apuesta que sabe construir */
    TipoApuesta tipo();

    /**
     * Construye las apuestas (sin id) a partir de las selecciones ya validadas.
     *
     * @param usuarioId   dueño
     * @param monto       monto por apuesta
     * @param selecciones selecciones con cuota tomada del servidor
     * @param ahora       momento de creación
     * @return apuestas listas para guardar
     */
    List<Apuesta> construir(long usuarioId, BigDecimal monto, List<Seleccion> selecciones, Instant ahora);
}
