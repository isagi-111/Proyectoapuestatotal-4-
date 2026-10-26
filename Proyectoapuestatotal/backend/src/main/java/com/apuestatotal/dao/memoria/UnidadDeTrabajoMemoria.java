package com.apuestatotal.dao.memoria;

import com.apuestatotal.dao.UnidadDeTrabajo;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Unidad de trabajo para los DAO en memoria: serializa las operaciones con un candado
 * reentrante (no hay rollback porque no hay base de datos).
 */
public final class UnidadDeTrabajoMemoria implements UnidadDeTrabajo {

    private final ReentrantLock candado = new ReentrantLock();

    @Override
    public <T> T ejecutar(Supplier<T> trabajo) {
        candado.lock();
        try {
            return trabajo.get();
        } finally {
            candado.unlock();
        }
    }
}
