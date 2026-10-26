package com.apuestatotal.dao.jdbc;

import com.apuestatotal.dao.ApuestaDao;
import com.apuestatotal.excepcion.NoEncontradoException;
import com.apuestatotal.model.Apuesta;
import com.apuestatotal.model.EstadoApuesta;
import com.apuestatotal.model.Opcion;
import com.apuestatotal.model.Seleccion;
import com.apuestatotal.model.TipoApuesta;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** {@link ApuestaDao} sobre JDBC (tablas {@code apuesta} y {@code apuesta_seleccion}). */
public final class ApuestaDaoJdbc implements ApuestaDao {

    private static final String SELECT = "SELECT a.id, a.usuario_id, a.tipo, a.estado, a.monto, a.cuota_total, "
            + "a.ganancia_potencial, a.creada_en, s.partido_id, s.opcion, s.cuota, s.descripcion "
            + "FROM apuesta a JOIN apuesta_seleccion s ON s.apuesta_id = a.id ";

    private final GestorConexiones gestor;

    /** @param gestor gestor de conexiones/transacciones */
    public ApuestaDaoJdbc(GestorConexiones gestor) {
        this.gestor = gestor;
    }

    @Override
    public Apuesta guardar(Apuesta apuesta) {
        return gestor.conConexion(c -> {
            String sql = "INSERT INTO apuesta (usuario_id, tipo, estado, monto, cuota_total, ganancia_potencial, creada_en) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";
            long id;
            try (PreparedStatement ps = c.prepareStatement(sql, new String[] {"id"})) {
                ps.setLong(1, apuesta.usuarioId());
                ps.setString(2, apuesta.tipo().name());
                ps.setString(3, apuesta.estado().name());
                ps.setBigDecimal(4, apuesta.monto());
                ps.setBigDecimal(5, apuesta.cuotaTotal());
                ps.setBigDecimal(6, apuesta.gananciaPotencial());
                ps.setObject(7, Tiempo.aSql(apuesta.creadaEn()));
                ps.executeUpdate();
                try (ResultSet claves = ps.getGeneratedKeys()) {
                    claves.next();
                    id = claves.getLong(1);
                }
            }
            String sqlSel = "INSERT INTO apuesta_seleccion (apuesta_id, partido_id, opcion, cuota, descripcion) "
                    + "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sqlSel)) {
                for (Seleccion s : apuesta.selecciones()) {
                    ps.setLong(1, id);
                    ps.setLong(2, s.partidoId());
                    ps.setString(3, s.opcion().name());
                    ps.setBigDecimal(4, s.cuota());
                    ps.setString(5, s.descripcion());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return apuesta.conId(id);
        });
    }

    @Override
    public List<Apuesta> listarPorUsuario(long usuarioId) {
        return gestor.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement(SELECT + "WHERE a.usuario_id = ? ORDER BY a.id DESC")) {
                ps.setLong(1, usuarioId);
                return leer(ps);
            }
        });
    }

    @Override
    public List<Apuesta> listarPendientesPorPartido(long partidoId) {
        return gestor.conConexion(c -> {
            String sql = SELECT + "WHERE a.estado = 'PENDIENTE' AND a.id IN "
                    + "(SELECT apuesta_id FROM apuesta_seleccion WHERE partido_id = ?) ORDER BY a.id";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, partidoId);
                return leer(ps);
            }
        });
    }

    @Override
    public void actualizarEstado(long apuestaId, EstadoApuesta estado) {
        int filas = gestor.conConexion(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE apuesta SET estado = ? WHERE id = ?")) {
                ps.setString(1, estado.name());
                ps.setLong(2, apuestaId);
                return ps.executeUpdate();
            }
        });
        if (filas != 1) {
            throw new NoEncontradoException("La apuesta no existe.");
        }
    }

    /** Lee filas apuesta+selección y las agrupa conservando el orden de la consulta. */
    private static List<Apuesta> leer(PreparedStatement ps) throws SQLException {
        Map<Long, Apuesta> cabeceras = new LinkedHashMap<>();
        Map<Long, List<Seleccion>> selecciones = new LinkedHashMap<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long id = rs.getLong("id");
                if (!cabeceras.containsKey(id)) {
                    cabeceras.put(id, new Apuesta(id, rs.getLong("usuario_id"),
                            TipoApuesta.valueOf(rs.getString("tipo")),
                            EstadoApuesta.valueOf(rs.getString("estado")),
                            rs.getBigDecimal("monto"), rs.getBigDecimal("cuota_total"),
                            rs.getBigDecimal("ganancia_potencial"),
                            Tiempo.deSql(rs.getObject("creada_en", OffsetDateTime.class)), List.of()));
                    selecciones.put(id, new ArrayList<>());
                }
                BigDecimal cuota = rs.getBigDecimal("cuota");
                selecciones.get(id).add(new Seleccion(rs.getLong("partido_id"),
                        Opcion.valueOf(rs.getString("opcion")), cuota, rs.getString("descripcion")));
            }
        }
        List<Apuesta> resultado = new ArrayList<>();
        for (Map.Entry<Long, Apuesta> e : cabeceras.entrySet()) {
            Apuesta a = e.getValue();
            resultado.add(new Apuesta(a.id(), a.usuarioId(), a.tipo(), a.estado(), a.monto(), a.cuotaTotal(),
                    a.gananciaPotencial(), a.creadaEn(), selecciones.get(e.getKey())));
        }
        return resultado;
    }
}
