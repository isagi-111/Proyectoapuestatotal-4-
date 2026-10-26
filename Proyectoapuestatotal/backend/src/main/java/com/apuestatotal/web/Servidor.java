package com.apuestatotal.web;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Servidor HTTP embebido del JDK ({@code com.sun.net.httpserver}), sin dependencias externas. */
public final class Servidor {

    private final HttpServer http;
    private final ExecutorService hilos = Executors.newFixedThreadPool(16);

    /**
     * @param puerto     puerto a escuchar (0 = efímero)
     * @param enrutador  manejador de todas las rutas
     * @throws IOException si no se puede abrir el puerto
     */
    public Servidor(int puerto, Enrutador enrutador) throws IOException {
        this.http = HttpServer.create(new InetSocketAddress(puerto), 0);
        this.http.createContext("/", enrutador);
        this.http.setExecutor(hilos);
    }

    /** Inicia el servidor. */
    public void iniciar() {
        http.start();
    }

    /** @return puerto real en el que escucha */
    public int puerto() {
        return http.getAddress().getPort();
    }

    /** Detiene el servidor y libera los hilos. */
    public void detener() {
        http.stop(0);
        hilos.shutdownNow();
    }
}
