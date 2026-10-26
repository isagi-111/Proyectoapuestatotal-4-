package com.apuestatotal.web;

/** Un controlador registra sus rutas en el enrutador (abierto a extensión, cerrado a modificación). */
public interface Controlador {

    /** @param enrutador enrutador donde se registran las rutas */
    void registrar(Enrutador enrutador);
}
