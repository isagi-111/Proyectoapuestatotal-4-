package com.apuestatotal.dao.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Crea conexiones JDBC. Las credenciales llegan por configuración (variables de entorno),
 * nunca escritas en el código.
 */
public final class FabricaConexiones {

    private final String url;
    private final String usuario;
    private final String clave;

    /**
     * @param url     URL JDBC
     * @param usuario usuario de la base de datos
     * @param clave   contraseña de la base de datos
     */
    public FabricaConexiones(String url, String usuario, String clave) {
        this.url = url;
        this.usuario = usuario;
        this.clave = clave;
    }

    /**
     * @return una conexión nueva
     * @throws SQLException si no se puede conectar
     */
    public Connection abrir() throws SQLException {
        return DriverManager.getConnection(url, usuario, clave);
    }
}
