package com.apuestatotal.config;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

/**
 * Configuración de la aplicación. Orden de prioridad: variables de entorno
 * ({@code APUESTATOTAL_*}) &gt; {@code application.properties}. Los secretos (clave de BD y
 * pepper) solo deben venir de variables de entorno.
 *
 * @param entorno          {@code dev} o {@code prod}
 * @param puerto           puerto HTTP (0 = efímero, útil en pruebas)
 * @param dbUrl            URL JDBC
 * @param dbUsuario        usuario de la BD
 * @param dbClave          contraseña de la BD
 * @param pepper           secreto para la huella HMAC del DNI
 * @param corsOrigen       único origen permitido para CORS
 * @param adminDni         DNI del administrador inicial (puede estar vacío)
 * @param saldoInicial     dinero simulado de bienvenida
 * @param ttlSesion        duración de la sesión
 * @param loginMaxFallos   intentos fallidos de login por ventana
 * @param loginVentana     ventana del límite de login
 * @param registroMax      registros permitidos por ventana e IP
 * @param registroVentana  ventana del límite de registro
 */
public record Configuracion(String entorno, int puerto, String dbUrl, String dbUsuario, String dbClave,
                            byte[] pepper, String corsOrigen, String adminDni, BigDecimal saldoInicial,
                            Duration ttlSesion, int loginMaxFallos, Duration loginVentana,
                            int registroMax, Duration registroVentana) {

    private static final String PEPPER_DEV = "dev-only-pepper-NO-USAR-EN-PRODUCCION-0123456789";

    /**
     * Carga la configuración desde {@code application.properties} y las variables de entorno.
     *
     * @param entorno variables de entorno (inyectable para pruebas)
     * @return la configuración validada
     * @throws IllegalStateException si falta algo obligatorio en producción
     */
    public static Configuracion cargar(Map<String, String> entorno) {
        Properties p = new Properties();
        try (InputStream in = Configuracion.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                p.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer application.properties", e);
        }
        String env = valor(entorno, "APUESTATOTAL_ENV", p, "app.env", "dev");
        boolean prod = "prod".equalsIgnoreCase(env);

        String pepper = valor(entorno, "APUESTATOTAL_PEPPER", p, "app.pepper", "");
        String claveBd = valor(entorno, "APUESTATOTAL_DB_PASSWORD", p, "db.password", "");
        if (prod) {
            if (pepper.length() < 32) {
                throw new IllegalStateException("En producción APUESTATOTAL_PEPPER es obligatorio (mínimo 32 caracteres)");
            }
            if (claveBd.isEmpty()) {
                throw new IllegalStateException("En producción APUESTATOTAL_DB_PASSWORD es obligatorio");
            }
        } else if (pepper.isEmpty()) {
            pepper = PEPPER_DEV;
        }
        return new Configuracion(
                env,
                Integer.parseInt(valor(entorno, "APUESTATOTAL_PORT", p, "server.port", "8080")),
                valor(entorno, "APUESTATOTAL_DB_URL", p, "db.url", "jdbc:h2:mem:apuestatotal;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"),
                valor(entorno, "APUESTATOTAL_DB_USER", p, "db.user", "sa"),
                claveBd,
                pepper.getBytes(StandardCharsets.UTF_8),
                valor(entorno, "APUESTATOTAL_CORS_ORIGIN", p, "cors.origin", "http://localhost:5500"),
                valor(entorno, "APUESTATOTAL_ADMIN_DNI", p, "admin.dni", ""),
                new BigDecimal(valor(entorno, "APUESTATOTAL_WALLET_INITIAL", p, "wallet.initial", "1000.00")),
                Duration.ofMinutes(Long.parseLong(valor(entorno, "APUESTATOTAL_SESSION_MINUTES", p, "session.ttl.minutes", "120"))),
                Integer.parseInt(valor(entorno, "APUESTATOTAL_LOGIN_MAX", p, "login.max.failures", "5")),
                Duration.ofMinutes(Long.parseLong(valor(entorno, "APUESTATOTAL_LOGIN_WINDOW", p, "login.window.minutes", "15"))),
                Integer.parseInt(valor(entorno, "APUESTATOTAL_REGISTER_MAX", p, "register.max", "10")),
                Duration.ofMinutes(Long.parseLong(valor(entorno, "APUESTATOTAL_REGISTER_WINDOW", p, "register.window.minutes", "60"))));
    }

    private static String valor(Map<String, String> env, String claveEnv, Properties p, String claveProp, String defecto) {
        String delEntorno = env.get(claveEnv);
        if (delEntorno != null && !delEntorno.isBlank()) {
            return delEntorno.trim();
        }
        return p.getProperty(claveProp, defecto).trim();
    }

    /** Evita filtrar secretos en logs: nunca imprime la clave de BD ni el pepper. */
    @Override
    public String toString() {
        return "Configuracion[entorno=" + entorno + ", puerto=" + puerto + ", dbUrl=" + dbUrl
                + ", dbUsuario=" + dbUsuario + ", dbClave=***, pepper=***, corsOrigen=" + corsOrigen + "]";
    }
}
