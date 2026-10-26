package com.apuestatotal.seguridad;

import com.apuestatotal.excepcion.DemasiadosIntentosException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitador de intentos por ventana deslizante (defensa contra fuerza bruta y registro
 * masivo). Se usa, por ejemplo, con la IP del cliente como clave.
 */
public final class LimitadorIntentos {

    private final int maximo;
    private final Duration ventana;
    private final Clock reloj;
    private final Map<String, Deque<Instant>> intentos = new ConcurrentHashMap<>();

    /**
     * @param maximo  intentos permitidos dentro de la ventana
     * @param ventana duración de la ventana
     * @param reloj   fuente de tiempo
     */
    public LimitadorIntentos(int maximo, Duration ventana, Clock reloj) {
        this.maximo = maximo;
        this.ventana = ventana;
        this.reloj = reloj;
    }

    /**
     * Lanza excepción si la clave ya agotó sus intentos.
     *
     * @param clave identificador (IP, por ejemplo)
     * @throws DemasiadosIntentosException si superó el límite
     */
    public void verificar(String clave) {
        Deque<Instant> cola = intentos.get(clave);
        if (cola == null) {
            return;
        }
        synchronized (cola) {
            purgar(cola);
            if (cola.size() >= maximo) {
                throw new DemasiadosIntentosException("Demasiados intentos. Espera unos minutos e inténtalo de nuevo.");
            }
        }
    }

    /**
     * Registra un intento (fallido) para la clave.
     *
     * @param clave identificador
     */
    public void registrar(String clave) {
        Deque<Instant> cola = intentos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (cola) {
            purgar(cola);
            cola.addLast(reloj.instant());
        }
    }

    /**
     * Borra los intentos de la clave (por ejemplo tras un inicio de sesión correcto).
     *
     * @param clave identificador
     */
    public void limpiar(String clave) {
        intentos.remove(clave);
    }

    private void purgar(Deque<Instant> cola) {
        Instant limite = reloj.instant().minus(ventana);
        while (!cola.isEmpty() && !cola.peekFirst().isAfter(limite)) {
            cola.removeFirst();
        }
    }
}
