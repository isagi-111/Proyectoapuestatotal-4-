package com.apuestatotal.web;

import com.apuestatotal.excepcion.ApuestaTotalException;
import com.apuestatotal.excepcion.ValidacionException;
import com.apuestatotal.seguridad.SesionActiva;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;

/** Datos de una petición: parámetros de ruta, cuerpo JSON, sesión e IP. */
public final class Contexto {

    private final HttpExchange intercambio;
    private final Matcher ruta;
    private final SesionActiva sesion;
    private final String token;
    private final Gson gson;
    private final int maxCuerpo;

    Contexto(HttpExchange intercambio, Matcher ruta, SesionActiva sesion, String token, Gson gson, int maxCuerpo) {
        this.intercambio = intercambio;
        this.ruta = ruta;
        this.sesion = sesion;
        this.token = token;
        this.gson = gson;
        this.maxCuerpo = maxCuerpo;
    }

    /**
     * @param grupo número de grupo de la expresión regular de la ruta
     * @return valor capturado
     */
    public String parametroRuta(int grupo) {
        return ruta.group(grupo);
    }

    /**
     * @param nombre nombre del parámetro de la query string
     * @return su valor, si viene
     */
    public Optional<String> parametroQuery(String nombre) {
        String query = intercambio.getRequestURI().getRawQuery();
        if (query == null) {
            return Optional.empty();
        }
        for (String par : query.split("&")) {
            int igual = par.indexOf('=');
            String clave = URLDecoder.decode(igual < 0 ? par : par.substring(0, igual), StandardCharsets.UTF_8);
            if (clave.equals(nombre)) {
                return Optional.of(igual < 0 ? "" : URLDecoder.decode(par.substring(igual + 1), StandardCharsets.UTF_8));
            }
        }
        return Optional.empty();
    }

    /**
     * Lee el cuerpo JSON con un tope de tamaño (defensa contra cuerpos gigantes).
     *
     * @param tipo clase del DTO
     * @param <T>  tipo del DTO
     * @return el DTO deserializado
     */
    public <T> T cuerpo(Class<T> tipo) {
        try (InputStream in = intercambio.getRequestBody()) {
            byte[] bytes = in.readNBytes(maxCuerpo + 1);
            if (bytes.length > maxCuerpo) {
                throw new ApuestaTotalException(413, "El cuerpo de la petición es demasiado grande.");
            }
            if (bytes.length == 0) {
                throw new ValidacionException("Falta el cuerpo JSON de la petición.");
            }
            T valor = gson.fromJson(new String(bytes, StandardCharsets.UTF_8), tipo);
            if (valor == null) {
                throw new ValidacionException("El cuerpo JSON está vacío.");
            }
            return valor;
        } catch (JsonParseException | IllegalStateException e) {
            throw new ValidacionException("El cuerpo no es un JSON válido.");
        } catch (IOException e) {
            throw new ValidacionException("No se pudo leer el cuerpo de la petición.");
        }
    }

    /** @return sesión autenticada (solo en rutas que no son públicas) */
    public SesionActiva sesion() {
        return sesion;
    }

    /** @return id del usuario autenticado */
    public long usuarioId() {
        return sesion.usuarioId();
    }

    /** @return token de la sesión actual */
    public String token() {
        return token;
    }

    /** @return IP real de la conexión (no se confía en cabeceras que el cliente puede falsear) */
    public String ip() {
        return intercambio.getRemoteAddress().getAddress().getHostAddress();
    }
}
