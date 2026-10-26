package com.apuestatotal.dao;

import com.apuestatotal.dao.memoria.ApuestaDaoMemoria;
import com.apuestatotal.dao.memoria.BilleteraDaoMemoria;
import com.apuestatotal.dao.memoria.PartidoDaoMemoria;
import com.apuestatotal.dao.memoria.UsuarioDaoMemoria;

/** Ejecuta el contrato contra las implementaciones en memoria. */
class DaoMemoriaTest extends ContratoDaoTest {

    @Override
    void preparar() {
        usuarios = new UsuarioDaoMemoria();
        billeteras = new BilleteraDaoMemoria();
        partidos = new PartidoDaoMemoria();
        apuestas = new ApuestaDaoMemoria();
    }
}
