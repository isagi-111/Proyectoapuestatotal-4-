package com.apuestatotal.service;

import com.apuestatotal.dao.ApuestaDao;
import com.apuestatotal.dao.BilleteraDao;
import com.apuestatotal.dao.PartidoDao;
import com.apuestatotal.dao.UnidadDeTrabajo;
import com.apuestatotal.excepcion.ConflictoException;
import com.apuestatotal.excepcion.NoEncontradoException;
import com.apuestatotal.excepcion.SaldoInsuficienteException;
import com.apuestatotal.excepcion.ValidacionException;
import com.apuestatotal.model.Apuesta;
import com.apuestatotal.model.EstadoApuesta;
import com.apuestatotal.model.Partido;
import com.apuestatotal.model.Seleccion;
import com.apuestatotal.model.TipoApuesta;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Implementación de {@link ApuestaService}. */
public final class ApuestaServiceImpl implements ApuestaService {

    private final ApuestaDao apuestas;
    private final PartidoDao partidos;
    private final BilleteraDao billeteras;
    private final UnidadDeTrabajo transaccion;
    private final Map<TipoApuesta, EstrategiaApuesta> estrategias;
    private final LimitesApuesta limites;
    private final Clock reloj;

    /**
     * @param apuestas     DAO de apuestas
     * @param partidos     DAO de partidos
     * @param billeteras   DAO de billeteras
     * @param transaccion  unidad de trabajo
     * @param estrategias  estrategia por cada tipo de apuesta disponible
     * @param limites      reglas de negocio
     * @param reloj        fuente de tiempo
     */
    public ApuestaServiceImpl(ApuestaDao apuestas, PartidoDao partidos, BilleteraDao billeteras,
                              UnidadDeTrabajo transaccion, Map<TipoApuesta, EstrategiaApuesta> estrategias,
                              LimitesApuesta limites, Clock reloj) {
        this.apuestas = apuestas;
        this.partidos = partidos;
        this.billeteras = billeteras;
        this.transaccion = transaccion;
        this.estrategias = Map.copyOf(estrategias);
        this.limites = limites;
        this.reloj = reloj;
    }

    @Override
    public List<Apuesta> colocar(long usuarioId, SolicitudApuesta solicitud) {
        validar(solicitud);
        EstrategiaApuesta estrategia = estrategias.get(solicitud.tipo());
        if (estrategia == null) {
            throw new ValidacionException("El tipo de apuesta " + solicitud.tipo() + " todavía no está disponible.");
        }
        BigDecimal monto = solicitud.monto().setScale(2, java.math.RoundingMode.UNNECESSARY);

        return transaccion.ejecutar(() -> {
            List<Seleccion> selecciones = solicitud.items().stream().map(this::aSeleccion).toList();
            List<Apuesta> nuevas = estrategia.construir(usuarioId, monto, selecciones, reloj.instant());
            for (Apuesta a : nuevas) {
                if (a.gananciaPotencial().compareTo(limites.gananciaMaxima()) > 0) {
                    throw new ValidacionException("La ganancia potencial máxima por apuesta es S/ "
                            + limites.gananciaMaxima().toPlainString() + ".");
                }
            }
            BigDecimal total = nuevas.stream().map(Apuesta::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (!billeteras.debitar(usuarioId, total)) {
                throw new SaldoInsuficienteException("Saldo insuficiente. Necesitas S/ " + total.toPlainString()
                        + ". Recarga saldo simulado o baja el monto.");
            }
            return nuevas.stream().map(apuestas::guardar).toList();
        });
    }

    @Override
    public List<Apuesta> listar(long usuarioId, Optional<EstadoApuesta> estado) {
        List<Apuesta> todas = apuestas.listarPorUsuario(usuarioId);
        return estado.map(e -> todas.stream().filter(a -> a.estado() == e).toList()).orElse(todas);
    }

    private Seleccion aSeleccion(SolicitudApuesta.Item item) {
        Partido partido = partidos.buscarPorId(item.partidoId())
                .orElseThrow(() -> new NoEncontradoException("El partido " + item.partidoId() + " no existe."));
        if (!partido.apostable()) {
            throw new ConflictoException("El partido " + partido.descripcion() + " ya finalizó.");
        }
        return new Seleccion(partido.id(), item.opcion(), partido.cuota(item.opcion()), partido.descripcion());
    }

    private void validar(SolicitudApuesta s) {
        if (s == null || s.tipo() == null) {
            throw new ValidacionException("El tipo de apuesta es obligatorio.");
        }
        if (s.monto() == null) {
            throw new ValidacionException("El monto es obligatorio.");
        }
        if (s.monto().stripTrailingZeros().scale() > 2) {
            throw new ValidacionException("El monto admite como máximo 2 decimales.");
        }
        if (s.monto().compareTo(limites.montoMinimo()) < 0 || s.monto().compareTo(limites.montoMaximo()) > 0) {
            throw new ValidacionException("El monto debe estar entre S/ " + limites.montoMinimo().toPlainString()
                    + " y S/ " + limites.montoMaximo().toPlainString() + ".");
        }
        if (s.items() == null || s.items().isEmpty()) {
            throw new ValidacionException("El cupón no tiene selecciones.");
        }
        if (s.items().size() > limites.maxSelecciones()) {
            throw new ValidacionException("Máximo " + limites.maxSelecciones() + " selecciones por cupón.");
        }
        Set<Long> vistos = new HashSet<>();
        for (SolicitudApuesta.Item item : s.items()) {
            if (item == null || item.opcion() == null) {
                throw new ValidacionException("Hay una selección incompleta.");
            }
            if (!vistos.add(item.partidoId())) {
                throw new ValidacionException("No puedes elegir dos veces el mismo partido en un cupón.");
            }
        }
    }
}
