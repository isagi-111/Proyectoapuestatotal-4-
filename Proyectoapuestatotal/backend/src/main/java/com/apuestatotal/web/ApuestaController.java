package com.apuestatotal.web;

import com.apuestatotal.excepcion.ValidacionException;
import com.apuestatotal.model.EstadoApuesta;
import com.apuestatotal.model.Opcion;
import com.apuestatotal.model.TipoApuesta;
import com.apuestatotal.service.ApuestaService;
import com.apuestatotal.service.BilleteraService;
import com.apuestatotal.service.SolicitudApuesta;
import com.apuestatotal.vista.Solicitudes;
import com.apuestatotal.vista.Vistas;
import java.util.ArrayList;
import java.util.List;

/** Controlador (MVC) de apuestas: colocar un cupón y listar el historial del usuario. */
public final class ApuestaController implements Controlador {

    private final ApuestaService apuestas;
    private final BilleteraService billetera;

    /**
     * @param apuestas  servicio de apuestas
     * @param billetera servicio de billetera
     */
    public ApuestaController(ApuestaService apuestas, BilleteraService billetera) {
        this.apuestas = apuestas;
        this.billetera = billetera;
    }

    @Override
    public void registrar(Enrutador r) {
        r.agregar("POST", "/api/apuestas", Acceso.CLIENTE, ctx -> {
            SolicitudApuesta solicitud = aSolicitud(ctx.cuerpo(Solicitudes.Cupon.class));
            var creadas = apuestas.colocar(ctx.usuarioId(), solicitud);
            return Respuesta.creado(new Vistas.ApuestasColocadas(
                    creadas.stream().map(Vistas.ApuestaVista::de).toList(), billetera.saldo(ctx.usuarioId())));
        });
        r.agregar("GET", "/api/apuestas", Acceso.CLIENTE, ctx -> {
            var estado = ctx.parametroQuery("estado").map(EstadoApuesta::deTexto);
            return Respuesta.ok(apuestas.listar(ctx.usuarioId(), estado).stream().map(Vistas.ApuestaVista::de).toList());
        });
    }

    private static SolicitudApuesta aSolicitud(Solicitudes.Cupon cupon) {
        if (cupon.selecciones() == null) {
            throw new ValidacionException("El cupón no tiene selecciones.");
        }
        List<SolicitudApuesta.Item> items = new ArrayList<>();
        for (Solicitudes.Seleccion s : cupon.selecciones()) {
            if (s == null || s.partidoId() == null) {
                throw new ValidacionException("Hay una selección incompleta.");
            }
            items.add(new SolicitudApuesta.Item(s.partidoId(), Opcion.deTexto(s.opcion())));
        }
        return new SolicitudApuesta(TipoApuesta.deTexto(cupon.tipo()), cupon.monto(), items);
    }
}
