package com.apuestatotal.web;

import com.apuestatotal.excepcion.ApuestaTotalException;
import com.apuestatotal.excepcion.NoAutenticadoException;
import com.apuestatotal.excepcion.NoEncontradoException;
import com.apuestatotal.excepcion.PermisoDenegadoException;
import com.apuestatotal.model.Rol;
import com.apuestatotal.seguridad.SesionActiva;
import com.apuestatotal.service.AutenticacionService;
import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enrutador central: aplica en UN solo lugar la seguridad común a todas las rutas
 * (cabeceras seguras, CORS restringido, autenticación por Bearer, autorización por rol,
 * traducción de excepciones a HTTP sin filtrar detalles internos).
 */
public final class Enrutador implements HttpHandler {

    private static final Logger LOG = Logger.getLogger(Enrutador.class.getName());
    private static final String PREFIJO_BEARER = "Bearer ";

    private record Entrada(String metodo, Pattern patron, Acceso acceso, ManejadorRuta manejador) {
    }

    private final List<Entrada> entradas = new ArrayList<>();
    private final AutenticacionService autenticacion;
    private final Gson gson;
    private final String corsOrigen;
    private final int maxCuerpo;

    /**
     * @param autenticacion servicio para validar tokens
     * @param gson          serializador JSON
     * @param corsOrigen    único origen permitido para CORS
     * @param maxCuerpo     tamaño máximo del cuerpo en bytes
     */
    public Enrutador(AutenticacionService autenticacion, Gson gson, String corsOrigen, int maxCuerpo) {
        this.autenticacion = autenticacion;
        this.gson = gson;
        this.corsOrigen = corsOrigen;
        this.maxCuerpo = maxCuerpo;
    }

    /**
     * Registra una ruta.
     *
     * @param metodo    método HTTP
     * @param regex     expresión regular de la ruta completa
     * @param acceso    nivel de acceso exigido
     * @param manejador acción del controlador
     */
    public void agregar(String metodo, String regex, Acceso acceso, ManejadorRuta manejador) {
        entradas.add(new Entrada(metodo, Pattern.compile(regex), acceso, manejador));
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            cabecerasSeguras(ex.getResponseHeaders());
            aplicarCors(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                return;
            }
            enviar(ex, despachar(ex));
        } catch (ApuestaTotalException e) {
            enviarError(ex, e.codigoHttp(), e.getMessage());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error no controlado", e);
            enviarError(ex, 500, "Error interno del servidor.");
        } finally {
            ex.close();
        }
    }

    private Respuesta despachar(HttpExchange ex) throws Exception {
        String metodo = ex.getRequestMethod();
        String ruta = ex.getRequestURI().getPath();
        boolean rutaConocida = false;
        for (Entrada e : entradas) {
            Matcher m = e.patron().matcher(ruta);
            if (!m.matches()) {
                continue;
            }
            rutaConocida = true;
            if (!e.metodo().equals(metodo)) {
                continue;
            }
            String token = extraerToken(ex);
            SesionActiva sesion = null;
            if (e.acceso() != Acceso.PUBLICO) {
                sesion = autenticacion.validar(token)
                        .orElseThrow(() -> new NoAutenticadoException("Debes iniciar sesión."));
                if (e.acceso() == Acceso.ADMIN && sesion.rol() != Rol.ADMIN) {
                    throw new PermisoDenegadoException("No tienes permisos de administrador.");
                }
            }
            return e.manejador().manejar(new Contexto(ex, m, sesion, token, gson, maxCuerpo));
        }
        if (rutaConocida) {
            throw new ApuestaTotalException(405, "Método no permitido.");
        }
        throw new NoEncontradoException("Ruta no encontrada.");
    }

    private static String extraerToken(HttpExchange ex) {
        String cabecera = ex.getRequestHeaders().getFirst("Authorization");
        if (cabecera != null && cabecera.startsWith(PREFIJO_BEARER)) {
            return cabecera.substring(PREFIJO_BEARER.length()).trim();
        }
        return null;
    }

    private static void cabecerasSeguras(Headers h) {
        h.set("Content-Type", "application/json; charset=utf-8");
        h.set("X-Content-Type-Options", "nosniff");
        h.set("X-Frame-Options", "DENY");
        h.set("Cache-Control", "no-store");
        h.set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
        h.set("Referrer-Policy", "no-referrer");
    }

    private void aplicarCors(HttpExchange ex) {
        String origen = ex.getRequestHeaders().getFirst("Origin");
        if (origen != null && origen.equals(corsOrigen)) {
            Headers h = ex.getResponseHeaders();
            h.set("Access-Control-Allow-Origin", origen);
            h.set("Vary", "Origin");
            h.set("Access-Control-Allow-Headers", "Authorization, Content-Type");
            h.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            h.set("Access-Control-Max-Age", "600");
        }
    }

    private void enviar(HttpExchange ex, Respuesta r) throws IOException {
        if (r.cuerpo() == null) {
            ex.sendResponseHeaders(r.estado(), -1);
            return;
        }
        byte[] bytes = gson.toJson(r.cuerpo()).getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(r.estado(), bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    private void enviarError(HttpExchange ex, int estado, String mensaje) {
        try {
            enviar(ex, new Respuesta(estado, Map.of("error", mensaje)));
        } catch (IOException | IllegalStateException e) {
            LOG.log(Level.WARNING, "No se pudo enviar la respuesta de error", e);
        }
    }
}
