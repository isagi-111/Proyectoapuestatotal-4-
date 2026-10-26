package com.apuestatotal.service;

import com.apuestatotal.dao.BilleteraDao;
import com.apuestatotal.dao.UnidadDeTrabajo;
import com.apuestatotal.dao.UsuarioDao;
import com.apuestatotal.excepcion.NoAutenticadoException;
import com.apuestatotal.model.Rol;
import com.apuestatotal.model.Usuario;
import com.apuestatotal.seguridad.AlmacenSesiones;
import com.apuestatotal.seguridad.HasheadorDni;
import com.apuestatotal.seguridad.LimitadorIntentos;
import com.apuestatotal.seguridad.SesionActiva;
import com.apuestatotal.seguridad.ValidadorDni;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;

/** Implementación de {@link AutenticacionService}. Depende solo de abstracciones (DIP). */
public final class AutenticacionServiceImpl implements AutenticacionService {

    private final UsuarioDao usuarios;
    private final BilleteraDao billeteras;
    private final HasheadorDni hasheador;
    private final AlmacenSesiones sesiones;
    private final LimitadorIntentos limiteLogin;
    private final LimitadorIntentos limiteRegistro;
    private final UnidadDeTrabajo transaccion;
    private final Clock reloj;
    private final BigDecimal saldoInicial;

    /**
     * @param usuarios        DAO de usuarios
     * @param billeteras      DAO de billeteras
     * @param hasheador       protección del DNI
     * @param sesiones        almacén de sesiones
     * @param limiteLogin     límite de intentos fallidos de login
     * @param limiteRegistro  límite de registros por IP
     * @param transaccion     unidad de trabajo
     * @param reloj           fuente de tiempo
     * @param saldoInicial    dinero simulado de bienvenida
     */
    public AutenticacionServiceImpl(UsuarioDao usuarios, BilleteraDao billeteras, HasheadorDni hasheador,
                                    AlmacenSesiones sesiones, LimitadorIntentos limiteLogin,
                                    LimitadorIntentos limiteRegistro, UnidadDeTrabajo transaccion,
                                    Clock reloj, BigDecimal saldoInicial) {
        this.usuarios = usuarios;
        this.billeteras = billeteras;
        this.hasheador = hasheador;
        this.sesiones = sesiones;
        this.limiteLogin = limiteLogin;
        this.limiteRegistro = limiteRegistro;
        this.transaccion = transaccion;
        this.reloj = reloj;
        this.saldoInicial = saldoInicial;
    }

    @Override
    public Usuario registrar(String dni, String ip) {
        limiteRegistro.verificar(ip);
        limiteRegistro.registrar(ip);
        String limpio = ValidadorDni.validar(dni);
        return crearUsuario(limpio, Rol.CLIENTE, saldoInicial);
    }

    @Override
    public SesionIniciada iniciarSesion(String dni, String ip) {
        limiteLogin.verificar(ip);
        String limpio = ValidadorDni.validar(dni);
        Optional<Usuario> usuario = usuarios.buscarPorDniHash(hasheador.hash(limpio));
        if (usuario.isEmpty()) {
            limiteLogin.registrar(ip);
            throw new NoAutenticadoException("Ese DNI no está registrado.");
        }
        limiteLogin.limpiar(ip);
        Usuario u = usuario.get();
        BigDecimal saldo = billeteras.saldoDe(u.id()).orElse(BigDecimal.ZERO);
        String token = sesiones.crear(u.id(), u.rol());
        return new SesionIniciada(token, u.id(), u.dniMascara(), u.rol(), saldo);
    }

    @Override
    public void cerrarSesion(String token) {
        sesiones.cerrar(token);
    }

    @Override
    public Optional<SesionActiva> validar(String token) {
        return sesiones.validar(token);
    }

    @Override
    public void asegurarAdministrador(String dni) {
        String limpio = ValidadorDni.validar(dni);
        if (usuarios.buscarPorDniHash(hasheador.hash(limpio)).isEmpty()) {
            crearUsuario(limpio, Rol.ADMIN, BigDecimal.ZERO.setScale(2));
        }
    }

    private Usuario crearUsuario(String dni, Rol rol, BigDecimal saldo) {
        return transaccion.ejecutar(() -> {
            Usuario nuevo = usuarios.guardar(
                    new Usuario(0, hasheador.hash(dni), hasheador.mascara(dni), rol, reloj.instant()));
            billeteras.crear(nuevo.id(), saldo);
            return nuevo;
        });
    }
}
