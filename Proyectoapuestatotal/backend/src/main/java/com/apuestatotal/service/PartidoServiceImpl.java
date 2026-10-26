package com.apuestatotal.service;

import com.apuestatotal.dao.ApuestaDao;
import com.apuestatotal.dao.BilleteraDao;
import com.apuestatotal.dao.PartidoDao;
import com.apuestatotal.dao.UnidadDeTrabajo;
import com.apuestatotal.excepcion.ConflictoException;
import com.apuestatotal.excepcion.NoEncontradoException;
import com.apuestatotal.excepcion.ValidacionException;
import com.apuestatotal.model.Apuesta;
import com.apuestatotal.model.EstadoApuesta;
import com.apuestatotal.model.EstadoPartido;
import com.apuestatotal.model.Partido;
import java.util.List;

/** Implementación de {@link PartidoService}. */
public final class PartidoServiceImpl implements PartidoService {

    private static final int GOLES_MAXIMOS = 99;

    private final PartidoDao partidos;
    private final ApuestaDao apuestas;
    private final BilleteraDao billeteras;
    private final UnidadDeTrabajo transaccion;
    private final LiquidadorApuestas liquidador;

    /**
     * @param partidos     DAO de partidos
     * @param apuestas     DAO de apuestas
     * @param billeteras   DAO de billeteras
     * @param transaccion  unidad de trabajo
     * @param liquidador   evaluador de apuestas
     */
    public PartidoServiceImpl(PartidoDao partidos, ApuestaDao apuestas, BilleteraDao billeteras,
                              UnidadDeTrabajo transaccion, LiquidadorApuestas liquidador) {
        this.partidos = partidos;
        this.apuestas = apuestas;
        this.billeteras = billeteras;
        this.transaccion = transaccion;
        this.liquidador = liquidador;
    }

    @Override
    public List<Partido> listar() {
        return partidos.listar();
    }

    @Override
    public ResultadoLiquidacion finalizar(long partidoId, Integer golesLocal, Integer golesVisita) {
        if (golesLocal == null || golesVisita == null
                || golesLocal < 0 || golesVisita < 0 || golesLocal > GOLES_MAXIMOS || golesVisita > GOLES_MAXIMOS) {
            throw new ValidacionException("Los goles deben ser números entre 0 y " + GOLES_MAXIMOS + ".");
        }
        return transaccion.ejecutar(() -> {
            Partido partido = partidos.buscarPorId(partidoId)
                    .orElseThrow(() -> new NoEncontradoException("El partido no existe."));
            if (partido.estado() == EstadoPartido.FINALIZADO) {
                throw new ConflictoException("El partido ya fue finalizado.");
            }
            Partido terminado = partido.finalizado(golesLocal, golesVisita);
            partidos.actualizar(terminado);

            int ganadas = 0;
            int perdidas = 0;
            int pendientes = 0;
            for (Apuesta apuesta : apuestas.listarPendientesPorPartido(partidoId)) {
                EstadoApuesta nuevo = liquidador.evaluar(apuesta, partidos::buscarPorId);
                switch (nuevo) {
                    case GANADA -> {
                        apuestas.actualizarEstado(apuesta.id(), nuevo);
                        billeteras.acreditar(apuesta.usuarioId(), apuesta.gananciaPotencial());
                        ganadas++;
                    }
                    case PERDIDA -> {
                        apuestas.actualizarEstado(apuesta.id(), nuevo);
                        perdidas++;
                    }
                    default -> pendientes++;
                }
            }
            return new ResultadoLiquidacion(terminado, ganadas, perdidas, pendientes);
        });
    }
}
