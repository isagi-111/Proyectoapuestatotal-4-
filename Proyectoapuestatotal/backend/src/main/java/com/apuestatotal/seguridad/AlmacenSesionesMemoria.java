package com.apuestatotal.seguridad;

import com.apuestatotal.model.Rol;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sesiones en memoria. Los tokens son de 256 bits generados con {@link SecureRandom};
 * en el mapa solo se guarda el SHA-256 del token, así que ni un volcado de memoria
 * entrega tokens utilizables. Cada sesión expira (el reloj se inyecta para poder probarlo).
 */
public final class AlmacenSesionesMemoria implements AlmacenSesiones {

    private static final int LIMITE_PARA_LIMPIAR = 10_000;

    private final SecureRandom azar = new SecureRandom();
    private final Map<String, SesionActiva> sesiones = new ConcurrentHashMap<>();
    private final Clock reloj;
    private final Duration duracion;

    /**
     * @param reloj    fuente de tiempo
     * @param duracion tiempo de vida de cada sesión
     */
    public AlmacenSesionesMemoria(Clock reloj, Duration duracion) {
        this.reloj = reloj;
        this.duracion = duracion;
    }

    @Override
    public String crear(long usuarioId, Rol rol) {
        if (sesiones.size() > LIMITE_PARA_LIMPIAR) {
            Instant ahora = reloj.instant();
            sesiones.values().removeIf(s -> !s.expira().isAfter(ahora));
        }
        byte[] bytes = new byte[32];
        azar.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sesiones.put(huella(token), new SesionActiva(usuarioId, rol, reloj.instant().plus(duracion)));
        return token;
    }

    @Override
    public Optional<SesionActiva> validar(String token) {
        if (token == null || token.isBlank() || token.length() > 100) {
            return Optional.empty();
        }
        String clave = huella(token);
        SesionActiva sesion = sesiones.get(clave);
        if (sesion == null) {
            return Optional.empty();
        }
        if (!sesion.expira().isAfter(reloj.instant())) {
            sesiones.remove(clave);
            return Optional.empty();
        }
        return Optional.of(sesion);
    }

    @Override
    public void cerrar(String token) {
        if (token != null) {
            sesiones.remove(huella(token));
        }
    }

    private static String huella(String token) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
