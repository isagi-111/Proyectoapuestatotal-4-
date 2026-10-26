package com.apuestatotal.vista;

import com.apuestatotal.model.Apuesta;
import com.apuestatotal.model.Partido;
import com.apuestatotal.model.Seleccion;
import com.apuestatotal.service.ResultadoLiquidacion;
import com.apuestatotal.service.SesionIniciada;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTOs de SALIDA (la "V" de MVC en una API REST: cómo se representa el modelo). Solo exponen
 * lo necesario; por ejemplo, nunca se devuelve el hash del DNI.
 */
public final class Vistas {

    private Vistas() {
    }

    /** Sesión iniciada. */
    public record SesionVista(String token, String dniMascara, String rol, BigDecimal saldo) {
        /**
         * @param s resultado del servicio
         * @return vista JSON
         */
        public static SesionVista de(SesionIniciada s) {
            return new SesionVista(s.token(), s.dniMascara(), s.rol().name(), s.saldo());
        }
    }

    /** Partido con sus cuotas. */
    public record PartidoVista(long id, String liga, String local, String visita, String inicio, String estado,
                               BigDecimal cuotaLocal, BigDecimal cuotaEmpate, BigDecimal cuotaVisita,
                               Integer golesLocal, Integer golesVisita) {
        /**
         * @param p partido del modelo
         * @return vista JSON
         */
        public static PartidoVista de(Partido p) {
            return new PartidoVista(p.id(), p.liga(), p.equipoLocal(), p.equipoVisita(), p.inicio().toString(),
                    p.estado().name(), p.cuotaLocal(), p.cuotaEmpate(), p.cuotaVisita(), p.golesLocal(),
                    p.golesVisita());
        }
    }

    /** Selección de una apuesta. */
    public record SeleccionVista(long partidoId, String partido, String opcion, BigDecimal cuota) {
        static SeleccionVista de(Seleccion s) {
            return new SeleccionVista(s.partidoId(), s.descripcion(), s.opcion().name(), s.cuota());
        }
    }

    /** Apuesta con sus selecciones. */
    public record ApuestaVista(long id, String tipo, String estado, BigDecimal monto, BigDecimal cuotaTotal,
                               BigDecimal gananciaPotencial, String creadaEn, List<SeleccionVista> selecciones) {
        /**
         * @param a apuesta del modelo
         * @return vista JSON
         */
        public static ApuestaVista de(Apuesta a) {
            return new ApuestaVista(a.id(), a.tipo().name(), a.estado().name(), a.monto(), a.cuotaTotal(),
                    a.gananciaPotencial(), a.creadaEn().toString(),
                    a.selecciones().stream().map(SeleccionVista::de).toList());
        }
    }

    /** Respuesta al colocar un cupón. */
    public record ApuestasColocadas(List<ApuestaVista> apuestas, BigDecimal saldo) {
    }

    /** Respuesta con un saldo. */
    public record SaldoVista(BigDecimal saldo) {
    }

    /** Resumen de liquidación. */
    public record LiquidacionVista(PartidoVista partido, int ganadas, int perdidas, int pendientes) {
        /**
         * @param r resultado del servicio
         * @return vista JSON
         */
        public static LiquidacionVista de(ResultadoLiquidacion r) {
            return new LiquidacionVista(PartidoVista.de(r.partido()), r.ganadas(), r.perdidas(), r.pendientes());
        }
    }

    /** Mensaje simple. */
    public record MensajeVista(String mensaje, String dniMascara) {
    }
}
