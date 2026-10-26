package com.apuestatotal.dao.jdbc;

import com.apuestatotal.excepcion.AccesoDatosException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Crea las tablas leyendo {@code schema.sql} del classpath (idempotente). */
public final class InicializadorEsquema {

    private InicializadorEsquema() {
    }

    /**
     * Ejecuta todas las sentencias de {@code schema.sql}.
     *
     * @param fabrica origen de conexiones
     */
    public static void ejecutar(FabricaConexiones fabrica) {
        String script = leerScript();
        try (Connection c = fabrica.abrir(); Statement st = c.createStatement()) {
            for (String sentencia : script.split(";")) {
                String sql = sentencia.trim();
                if (!sql.isEmpty()) {
                    st.execute(sql);
                }
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo crear el esquema.", e);
        }
    }

    private static String leerScript() {
        try (InputStream in = InicializadorEsquema.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("No se encontró schema.sql en el classpath");
            }
            StringBuilder sb = new StringBuilder();
            for (String linea : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                if (!linea.trim().startsWith("--")) {
                    sb.append(linea).append('\n');
                }
            }
            return sb.toString();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer schema.sql", e);
        }
    }
}
