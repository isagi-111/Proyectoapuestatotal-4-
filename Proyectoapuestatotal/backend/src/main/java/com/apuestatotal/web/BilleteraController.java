package com.apuestatotal.web;

import com.apuestatotal.service.BilleteraService;
import com.apuestatotal.vista.Solicitudes;
import com.apuestatotal.vista.Vistas;

/** Controlador (MVC) del saldo simulado. */
public final class BilleteraController implements Controlador {

    private final BilleteraService servicio;

    /** @param servicio servicio de billetera */
    public BilleteraController(BilleteraService servicio) {
        this.servicio = servicio;
    }

    @Override
    public void registrar(Enrutador r) {
        r.agregar("GET", "/api/billetera", Acceso.CLIENTE,
                ctx -> Respuesta.ok(new Vistas.SaldoVista(servicio.saldo(ctx.usuarioId()))));
        r.agregar("POST", "/api/billetera/recarga", Acceso.CLIENTE, ctx -> Respuesta.ok(new Vistas.SaldoVista(
                servicio.recargar(ctx.usuarioId(), ctx.cuerpo(Solicitudes.Recarga.class).monto()))));
    }
}
