package com.apuestatotal.web;

import com.apuestatotal.model.Usuario;
import com.apuestatotal.service.AutenticacionService;
import com.apuestatotal.vista.Solicitudes;
import com.apuestatotal.vista.Vistas;

/** Controlador (MVC) de registro, inicio y cierre de sesión. */
public final class AutenticacionController implements Controlador {

    private final AutenticacionService servicio;

    /** @param servicio servicio de autenticación */
    public AutenticacionController(AutenticacionService servicio) {
        this.servicio = servicio;
    }

    @Override
    public void registrar(Enrutador r) {
        r.agregar("POST", "/api/auth/registro", Acceso.PUBLICO, ctx -> {
            Usuario u = servicio.registrar(ctx.cuerpo(Solicitudes.Dni.class).dni(), ctx.ip());
            return Respuesta.creado(new Vistas.MensajeVista("Registro exitoso. Ya puedes iniciar sesión.", u.dniMascara()));
        });
        r.agregar("POST", "/api/auth/login", Acceso.PUBLICO, ctx ->
                Respuesta.ok(Vistas.SesionVista.de(servicio.iniciarSesion(ctx.cuerpo(Solicitudes.Dni.class).dni(), ctx.ip()))));
        r.agregar("POST", "/api/auth/logout", Acceso.CLIENTE, ctx -> {
            servicio.cerrarSesion(ctx.token());
            return Respuesta.sinContenido();
        });
    }
}
