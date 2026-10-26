package com.apuestatotal.soporte;

import com.apuestatotal.dao.ApuestaDao;
import com.apuestatotal.dao.BilleteraDao;
import com.apuestatotal.dao.PartidoDao;
import com.apuestatotal.dao.UsuarioDao;
import com.apuestatotal.dao.memoria.ApuestaDaoMemoria;
import com.apuestatotal.dao.memoria.BilleteraDaoMemoria;
import com.apuestatotal.dao.memoria.PartidoDaoMemoria;
import com.apuestatotal.dao.memoria.UnidadDeTrabajoMemoria;
import com.apuestatotal.dao.memoria.UsuarioDaoMemoria;
import com.apuestatotal.model.EstadoPartido;
import com.apuestatotal.model.Partido;
import com.apuestatotal.model.TipoApuesta;
import com.apuestatotal.seguridad.AlmacenSesionesMemoria;
import com.apuestatotal.seguridad.HasheadorDniHmac;
import com.apuestatotal.seguridad.LimitadorIntentos;
import com.apuestatotal.service.ApuestaService;
import com.apuestatotal.service.ApuestaServiceImpl;
import com.apuestatotal.service.AutenticacionService;
import com.apuestatotal.service.AutenticacionServiceImpl;
import com.apuestatotal.service.BilleteraService;
import com.apuestatotal.service.BilleteraServiceImpl;
import com.apuestatotal.service.EstrategiaMultiple;
import com.apuestatotal.service.EstrategiaSimple;
import com.apuestatotal.service.LiquidadorApuestas;
import com.apuestatotal.service.LimitesApuesta;
import com.apuestatotal.service.PartidoService;
import com.apuestatotal.service.PartidoServiceImpl;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

/** Arma los servicios con DAO en memoria (sin base de datos) para pruebas unitarias rápidas. */
public final class Escenario {

    public final RelojMutable reloj = new RelojMutable();
    public final UsuarioDao usuarios = new UsuarioDaoMemoria();
    public final BilleteraDao billeteras = new BilleteraDaoMemoria();
    public final PartidoDao partidos = new PartidoDaoMemoria();
    public final ApuestaDao apuestas = new ApuestaDaoMemoria();
    public final AutenticacionService autenticacion;
    public final BilleteraService billetera;
    public final ApuestaService servicioApuestas;
    public final PartidoService servicioPartidos;

    public final Partido turquiaFrancia;
    public final Partido italiaBelgica;
    public final Partido hungriaUcrania;

    public Escenario() {
        var uow = new UnidadDeTrabajoMemoria();
        autenticacion = new AutenticacionServiceImpl(usuarios, billeteras,
                new HasheadorDniHmac("pepper-de-pruebas-0123456789".getBytes()),
                new AlmacenSesionesMemoria(reloj, Duration.ofHours(2)),
                new LimitadorIntentos(3, Duration.ofMinutes(15), reloj),
                new LimitadorIntentos(3, Duration.ofMinutes(60), reloj),
                uow, reloj, new BigDecimal("1000.00"));
        billetera = new BilleteraServiceImpl(billeteras, uow);
        servicioApuestas = new ApuestaServiceImpl(apuestas, partidos, billeteras, uow,
                Map.of(TipoApuesta.SIMPLE, new EstrategiaSimple(), TipoApuesta.MULTIPLE, new EstrategiaMultiple()),
                LimitesApuesta.porDefecto(), reloj);
        servicioPartidos = new PartidoServiceImpl(partidos, apuestas, billeteras, uow, new LiquidadorApuestas());

        turquiaFrancia = partidos.guardar(partido("Turquía", "Francia", EstadoPartido.PROGRAMADO, "2.51", "3.19", "2.44"));
        italiaBelgica = partidos.guardar(partido("Italia", "Bélgica", EstadoPartido.PROGRAMADO, "2.31", "3.18", "3.65"));
        hungriaUcrania = partidos.guardar(partido("Hungría", "Ucrania", EstadoPartido.EN_VIVO, "2.97", "3.38", "2.17"));
    }

    public Partido partido(String local, String visita, EstadoPartido estado, String cl, String ce, String cv) {
        return new Partido(0, "Clasificatorias Mundial", local, visita, reloj.instant().plus(Duration.ofDays(1)),
                estado, new BigDecimal(cl), new BigDecimal(ce), new BigDecimal(cv), null, null);
    }

    /** Registra un usuario y devuelve su id. */
    public long registrar(String dni) {
        return autenticacion.registrar(dni, "ip-" + dni).id();
    }
}
