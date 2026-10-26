package com.apuestatotal.dao.jdbc;

import com.apuestatotal.dao.UsuarioDao;
import com.apuestatotal.excepcion.ConflictoException;
import com.apuestatotal.model.Rol;
import com.apuestatotal.model.Usuario;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;

/** {@link UsuarioDao} sobre JDBC. Todas las consultas usan PreparedStatement (anti SQL injection). */
public final class UsuarioDaoJdbc implements UsuarioDao {

    private static final String UNIQUE_VIOLATION = "23505";
    private static final String COLUMNAS = "id, dni_hash, dni_mascara, rol, creado_en";

    private final GestorConexiones gestor;

    /** @param gestor gestor de conexiones/transacciones */
    public UsuarioDaoJdbc(GestorConexiones gestor) {
        this.gestor = gestor;
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        return gestor.conConexion(c -> {
            String sql = "INSERT INTO usuario (dni_hash, dni_mascara, rol, creado_en) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sql, new String[] {"id"})) {
                ps.setString(1, usuario.dniHash());
                ps.setString(2, usuario.dniMascara());
                ps.setString(3, usuario.rol().name());
                ps.setObject(4, Tiempo.aSql(usuario.creadoEn()));
                ps.executeUpdate();
                try (ResultSet claves = ps.getGeneratedKeys()) {
                    claves.next();
                    return usuario.conId(claves.getLong(1));
                }
            } catch (SQLException e) {
                if (UNIQUE_VIOLATION.equals(e.getSQLState())) {
                    throw new ConflictoException("El DNI ya está registrado.");
                }
                throw e;
            }
        });
    }

    @Override
    public Optional<Usuario> buscarPorId(long id) {
        return gestor.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNAS + " FROM usuario WHERE id = ?")) {
                ps.setLong(1, id);
                return primero(ps);
            }
        });
    }

    @Override
    public Optional<Usuario> buscarPorDniHash(String dniHash) {
        return gestor.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNAS + " FROM usuario WHERE dni_hash = ?")) {
                ps.setString(1, dniHash);
                return primero(ps);
            }
        });
    }

    private static Optional<Usuario> primero(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return Optional.empty();
            }
            return Optional.of(new Usuario(
                    rs.getLong("id"),
                    rs.getString("dni_hash"),
                    rs.getString("dni_mascara"),
                    Rol.valueOf(rs.getString("rol")),
                    Tiempo.deSql(rs.getObject("creado_en", OffsetDateTime.class))));
        }
    }
}
