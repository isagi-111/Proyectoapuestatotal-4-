package com.apuestatotal.web;

import com.apuestatotal.service.PartidoService;
import com.apuestatotal.vista.Solicitudes;
import com.apuestatotal.vista.Vistas;
import java.util.Map;

/** Controlador (MVC) de partidos y de su liquidación (solo administrador). */
public final class PartidoController implements Controlador {

    private final PartidoService servicio;

    /** @param servicio servicio de partidos */
    public PartidoController(PartidoService servicio) {
        this.servicio = servicio;
    }

    @Override
    public void registrar(Enrutador r) {
        r.agregar("GET", "/api/salud", Acceso.PUBLICO, ctx -> Respuesta.ok(Map.of("estado", "ok")));
        r.agregar("GET", "/api/partidos", Acceso.PUBLICO, ctx -> Respuesta.ok(
                servicio.listar().stream().map(Vistas.PartidoVista::de).toList()));
        r.agregar("POST", "/api/admin/partidos/(\\d{1,18})/finalizar", Acceso.ADMIN, ctx -> {
            Solicitudes.Resultado resultado = ctx.cuerpo(Solicitudes.Resultado.class);
            long id = Long.parseLong(ctx.parametroRuta(1));
            return Respuesta.ok(Vistas.LiquidacionVista.de(
                    servicio.finalizar(id, resultado.golesLocal(), resultado.golesVisita())));
        });
    }
}
