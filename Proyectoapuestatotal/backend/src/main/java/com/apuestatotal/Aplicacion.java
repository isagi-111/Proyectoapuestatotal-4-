package com.apuestatotal;

import com.apuestatotal.config.Configuracion;
import com.apuestatotal.dao.ApuestaDao;
import com.apuestatotal.dao.BilleteraDao;
import com.apuestatotal.dao.PartidoDao;
import com.apuestatotal.dao.UsuarioDao;
import com.apuestatotal.dao.jdbc.ApuestaDaoJdbc;
import com.apuestatotal.dao.jdbc.BilleteraDaoJdbc;
import com.apuestatotal.dao.jdbc.FabricaConexiones;
import com.apuestatotal.dao.jdbc.GestorConexiones;
import com.apuestatotal.dao.jdbc.InicializadorEsquema;
import com.apuestatotal.dao.jdbc.PartidoDaoJdbc;
import com.apuestatotal.dao.jdbc.UsuarioDaoJdbc;
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
import com.apuestatotal.service.EstrategiaApuesta;
import com.apuestatotal.service.EstrategiaMultiple;
import com.apuestatotal.service.EstrategiaSimple;
import com.apuestatotal.service.LiquidadorApuestas;
import com.apuestatotal.service.LimitesApuesta;
import com.apuestatotal.service.PartidoService;
import com.apuestatotal.service.PartidoServiceImpl;
import com.apuestatotal.service.SemillaDatos;
import com.apuestatotal.web.ApuestaController;
import com.apuestatotal.web.AutenticacionController;
import com.apuestatotal.web.BilleteraController;
import com.apuestatotal.web.Enrutador;
import com.apuestatotal.web.PartidoController;
import com.apuestatotal.web.Servidor;
import com.google.gson.Gson;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Raíz de composición: el ÚNICO lugar donde se crean las clases concretas y se conectan
 * entre sí (inyección de dependencias manual). Todo lo demás depende de interfaces.
 */
public final class Aplicacion {

    private static final Logger LOG = Logger.getLogger(Aplicacion.class.getName());
    private static final int MAX_CUERPO_BYTES = 10 * 1024;

    private Aplicacion() {
    }

    /**
     * Punto de entrada.
     *
     * @param args sin uso
     * @throws IOException si no se puede abrir el puerto
     */
    public static void main(String[] args) throws IOException {
        Configuracion config = Configuracion.cargar(System.getenv());
        Servidor servidor = construir(config, Clock.systemUTC());
        servidor.iniciar();
        LOG.info("Apuesta Total backend escuchando en http://localhost:" + servidor.puerto()
                + " (entorno: " + config.entorno() + ")");
    }

    /**
     * Arma toda la aplicación (también la usan las pruebas de integración).
     *
     * @param config configuración
     * @param reloj  fuente de tiempo
     * @return servidor listo para iniciar
     * @throws IOException si no se puede abrir el puerto
     */
    public static Servidor construir(Configuracion config, Clock reloj) throws IOException {
        // --- Persistencia (DAO) ---
        FabricaConexiones fabrica = new FabricaConexiones(config.dbUrl(), config.dbUsuario(), config.dbClave());
        InicializadorEsquema.ejecutar(fabrica);
        GestorConexiones gestor = new GestorConexiones(fabrica);
        UsuarioDao usuarios = new UsuarioDaoJdbc(gestor);
        BilleteraDao billeteras = new BilleteraDaoJdbc(gestor);
        PartidoDao partidos = new PartidoDaoJdbc(gestor);
        ApuestaDao apuestas = new ApuestaDaoJdbc(gestor);
        SemillaDatos.cargarSiVacio(partidos, reloj);

        // --- Seguridad ---
        var hasheador = new HasheadorDniHmac(config.pepper());
        var sesiones = new AlmacenSesionesMemoria(reloj, config.ttlSesion());
        var limiteLogin = new LimitadorIntentos(config.loginMaxFallos(), config.loginVentana(), reloj);
        var limiteRegistro = new LimitadorIntentos(config.registroMax(), config.registroVentana(), reloj);

        // --- Servicios (lógica de negocio) ---
        AutenticacionService autenticacion = new AutenticacionServiceImpl(usuarios, billeteras, hasheador,
                sesiones, limiteLogin, limiteRegistro, gestor, reloj, config.saldoInicial());
        BilleteraService billetera = new BilleteraServiceImpl(billeteras, gestor);
        Map<TipoApuesta, EstrategiaApuesta> estrategias = List.<EstrategiaApuesta>of(
                new EstrategiaSimple(), new EstrategiaMultiple()).stream()
                .collect(Collectors.toMap(EstrategiaApuesta::tipo, Function.identity()));
        ApuestaService servicioApuestas = new ApuestaServiceImpl(apuestas, partidos, billeteras, gestor,
                estrategias, LimitesApuesta.porDefecto(), reloj);
        PartidoService servicioPartidos = new PartidoServiceImpl(partidos, apuestas, billeteras, gestor,
                new LiquidadorApuestas());

        if (!config.adminDni().isEmpty()) {
            autenticacion.asegurarAdministrador(config.adminDni());
        }

        // --- Controladores (MVC) ---
        Enrutador enrutador = new Enrutador(autenticacion, new Gson(), config.corsOrigen(), MAX_CUERPO_BYTES);
        List.of(new AutenticacionController(autenticacion),
                new BilleteraController(billetera),
                new PartidoController(servicioPartidos),
                new ApuestaController(servicioApuestas, billetera))
                .forEach(c -> c.registrar(enrutador));

        return new Servidor(config.puerto(), enrutador);
    }
}
