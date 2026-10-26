package com.apuestatotal.web;

/** Acción de un controlador asociada a una ruta. */
@FunctionalInterface
public interface ManejadorRuta {

    /**
     * @param contexto datos de la petición
     * @return respuesta a enviar
     * @throws Exception cualquier error (el enrutador lo traduce a HTTP)
     */
    Respuesta manejar(Contexto contexto) throws Exception;
}
