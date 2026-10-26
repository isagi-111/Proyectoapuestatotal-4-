package com.apuestatotal.dao.jdbc;

import com.apuestatotal.dao.PartidoDao;
import com.apuestatotal.excepcion.NoEncontradoException;
import com.apuestatotal.model.EstadoPartido;
import com.apuestatotal.model.Partido;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** {@link PartidoDao} sobre JDBC. */
public final class PartidoDaoJdbc implements PartidoDao {

    private static final String SELECT = "SELECT id, liga, equipo_local, equipo_visita, inicio, estado, "
            + "cuota_local, cuota_empate, cuota_visita, goles_local, goles_visita FROM partido";

    private final GestorConexiones gestor;

    /** @param gestor gestor de conexiones/transacciones */
    public PartidoDaoJdbc(GestorConexiones gestor) {
        this.gestor = gestor;
    }

    @Override
    public Partido guardar(Partido p) {
        return gestor.conConexion(c -> {
            String sql = "INSERT INTO partido (liga, equipo_local, equipo_visita, inicio, estado, cuota_local, "
                    + "cuota_empate, cuota_visita, goles_local, goles_visita) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sql, new String[] {"id"})) {
                ps.setString(1, p.liga());
                ps.setString(2, p.equipoLocal());
                ps.setString(3, p.equipoVisita());
                ps.setObject(4, Tiempo.aSql(p.inicio()));
                ps.setString(5, p.estado().name());
                ps.setBigDecimal(6, p.cuotaLocal());
                ps.setBigDecimal(7, p.cuotaEmpate());
                ps.setBigDecimal(8, p.cuotaVisita());
                asignarGoles(ps, 9, p.golesLocal());
                asignarGoles(ps, 10, p.golesVisita());
                ps.executeUpdate();
                try (ResultSet claves = ps.getGeneratedKeys()) {
                    claves.next();
                    return p.conId(claves.getLong(1));
                }
            }
        });
    }

    @Override
    public List<Partido> listar() {
        return gestor.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement(SELECT + " ORDER BY inicio, id");
                 ResultSet rs = ps.executeQuery()) {
                List<Partido> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
                return lista;
            }
        });
    }

    @Override
    public Optional<Partido> buscarPorId(long id) {
        return gestor.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement(SELECT + " WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.<Partido>empty();
                }
            }
        });
    }

    @Override
    public void actualizar(Partido p) {
        int filas = gestor.conConexion(c -> {
            String sql = "UPDATE partido SET estado = ?, cuota_local = ?, cuota_empate = ?, cuota_visita = ?, "
                    + "goles_local = ?, goles_visita = ? WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, p.estado().name());
                ps.setBigDecimal(2, p.cuotaLocal());
                ps.setBigDecimal(3, p.cuotaEmpate());
                ps.setBigDecimal(4, p.cuotaVisita());
                asignarGoles(ps, 5, p.golesLocal());
                asignarGoles(ps, 6, p.golesVisita());
                ps.setLong(7, p.id());
                return ps.executeUpdate();
            }
        });
        if (filas != 1) {
            throw new NoEncontradoException("El partido no existe.");
        }
    }

    private static void asignarGoles(PreparedStatement ps, int indice, Integer goles) throws SQLException {
        if (goles == null) {
            ps.setNull(indice, Types.SMALLINT);
        } else {
            ps.setInt(indice, goles);
        }
    }

    private static Partido mapear(ResultSet rs) throws SQLException {
        return new Partido(
                rs.getLong("id"),
                rs.getString("liga"),
                rs.getString("equipo_local"),
                rs.getString("equipo_visita"),
                Tiempo.deSql(rs.getObject("inicio", OffsetDateTime.class)),
                EstadoPartido.valueOf(rs.getString("estado")),
                rs.getBigDecimal("cuota_local"),
                rs.getBigDecimal("cuota_empate"),
                rs.getBigDecimal("cuota_visita"),
                rs.getObject("goles_local", Integer.class),
                rs.getObject("goles_visita", Integer.class));
    }
}
