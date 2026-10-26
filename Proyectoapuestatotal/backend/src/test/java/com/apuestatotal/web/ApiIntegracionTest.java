package com.apuestatotal.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apuestatotal.Aplicacion;
import com.apuestatotal.config.Configuracion;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Pruebas de extremo a extremo: levantan el servidor real (H2 en memoria) y llaman a la API
 * por HTTP, recorriendo Controlador -> Servicio -> DAO -> Base de datos.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiIntegracionTest {

    private static final String ADMIN_DNI = "99999999";

    private Servidor servidor;
    private ClienteHttp api;

    @BeforeAll
    void arrancar() throws Exception {
        Configuracion cfg = Configuracion.cargar(Map.of(
                "APUESTATOTAL_PORT", "0",
                "APUESTATOTAL_DB_URL", "jdbc:h2:mem:api_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "APUESTATOTAL_ADMIN_DNI", ADMIN_DNI,
                "APUESTATOTAL_CORS_ORIGIN", "http://localhost:5500",
                "APUESTATOTAL_REGISTER_MAX", "1000",
                "APUESTATOTAL_LOGIN_MAX", "1000"));
        servidor = Aplicacion.construir(cfg, Clock.systemUTC());
        servidor.iniciar();
        api = new ClienteHttp(servidor.puerto());
    }

    @AfterAll
    void detener() {
        servidor.detener();
    }

    // ----- ayudas -----
    private String registrarYEntrar(String dni) {
        assertEquals(201, api.post("/api/auth/registro", null, "{\"dni\":\"" + dni + "\"}").estado());
        return entrar(dni);
    }

    private String entrar(String dni) {
        var r = api.post("/api/auth/login", null, "{\"dni\":\"" + dni + "\"}");
        assertEquals(200, r.estado(), r.cuerpo());
        return r.json().getAsJsonObject().get("token").getAsString();
    }

    private long idPartido(String local) {
        JsonArray lista = api.get("/api/partidos", null).json().getAsJsonArray();
        for (JsonElement e : lista) {
            if (e.getAsJsonObject().get("local").getAsString().equals(local)) {
                return e.getAsJsonObject().get("id").getAsLong();
            }
        }
        throw new AssertionError("No existe el partido de " + local);
    }

    private BigDecimal saldo(String token) {
        return api.get("/api/billetera", token).json().getAsJsonObject().get("saldo").getAsBigDecimal();
    }

    private String cupon(String tipo, String monto, long... partidosLocal) {
        StringBuilder sb = new StringBuilder("{\"tipo\":\"" + tipo + "\",\"monto\":" + monto + ",\"selecciones\":[");
        for (int i = 0; i < partidosLocal.length; i++) {
            sb.append(i > 0 ? "," : "").append("{\"partidoId\":").append(partidosLocal[i]).append(",\"opcion\":\"LOCAL\"}");
        }
        return sb.append("]}").toString();
    }

    // ----- pruebas -----
    @Test
    void saludYCabecerasDeSeguridad() {
        var r = api.get("/api/salud", null);
        assertEquals(200, r.estado());
        assertEquals("nosniff", r.cabeceras().firstValue("X-Content-Type-Options").orElse(""));
        assertEquals("DENY", r.cabeceras().firstValue("X-Frame-Options").orElse(""));
        assertEquals("no-store", r.cabeceras().firstValue("Cache-Control").orElse(""));
        assertTrue(r.cabeceras().firstValue("Content-Security-Policy").isPresent());
    }

    @Test
    void registroSoloConDniYValidaciones() {
        assertEquals(400, api.post("/api/auth/registro", null, "{\"dni\":\"123\"}").estado());
        assertEquals(400, api.post("/api/auth/registro", null, "{\"dni\":\"1234567a\"}").estado());
        assertEquals(400, api.post("/api/auth/registro", null, "{}").estado());
        assertEquals(400, api.post("/api/auth/registro", null, "esto no es json").estado());
        assertEquals(400, api.post("/api/auth/registro", null, "{\"dni\":\"1'; DROP TABLE usuario;--\"}").estado());
        var ok = api.post("/api/auth/registro", null, "{\"dni\":\"10000001\"}");
        assertEquals(201, ok.estado());
        assertEquals("10****01", ok.json().getAsJsonObject().get("dniMascara").getAsString());
        assertEquals(409, api.post("/api/auth/registro", null, "{\"dni\":\"10000001\"}").estado());
    }

    @Test
    void loginConDniYRespuestaSinDatosSensibles() {
        api.post("/api/auth/registro", null, "{\"dni\":\"10000002\"}");
        var r = api.post("/api/auth/login", null, "{\"dni\":\"10000002\"}");
        assertEquals(200, r.estado());
        var json = r.json().getAsJsonObject();
        assertEquals("CLIENTE", json.get("rol").getAsString());
        assertEquals(0, new BigDecimal("1000.00").compareTo(json.get("saldo").getAsBigDecimal()));
        assertFalse(r.cuerpo().contains("10000002"), "el DNI completo nunca vuelve en la respuesta");
        assertFalse(r.cuerpo().toLowerCase().contains("hash"));
        assertEquals(401, api.post("/api/auth/login", null, "{\"dni\":\"20000000\"}").estado());
    }

    @Test
    void rutasProtegidasExigenToken() {
        assertEquals(401, api.get("/api/billetera", null).estado());
        assertEquals(401, api.get("/api/billetera", "token-inventado").estado());
        assertEquals(401, api.get("/api/apuestas", null).estado());
        assertEquals(401, api.post("/api/apuestas", null, cupon("SIMPLE", "10", 1)).estado());
        assertEquals(401, api.post("/api/billetera/recarga", null, "{\"monto\":10}").estado());
    }

    @Test
    void partidosSonPublicos() {
        var r = api.get("/api/partidos", null);
        assertEquals(200, r.estado());
        assertTrue(r.json().getAsJsonArray().size() >= 4);
    }

    @Test
    void apostarDescuentaSaldoYLaApuestaSeMantieneAlCerrarYVolverAIniciarSesion() {
        String token = registrarYEntrar("10000003");
        long p1 = idPartido("Italia");
        long p2 = idPartido("Hungría");

        var r = api.post("/api/apuestas", token, cupon("SIMPLE", "100", p1, p2));
        assertEquals(201, r.estado(), r.cuerpo());
        assertEquals(2, r.json().getAsJsonObject().get("apuestas").getAsJsonArray().size());
        assertEquals(0, new BigDecimal("800").compareTo(saldo(token)));

        assertEquals(204, api.post("/api/auth/logout", token, null).estado());
        assertEquals(401, api.get("/api/apuestas", token).estado(), "el token viejo ya no sirve");

        String tokenNuevo = entrar("10000003");
        JsonArray historial = api.get("/api/apuestas", tokenNuevo).json().getAsJsonArray();
        assertEquals(2, historial.size(), "las apuestas siguen ahí después de cerrar e iniciar sesión");
        assertEquals("PENDIENTE", historial.get(0).getAsJsonObject().get("estado").getAsString());
        assertEquals(0, new BigDecimal("800").compareTo(saldo(tokenNuevo)), "el saldo también se mantiene");
        assertNotNull(historial.get(0).getAsJsonObject().get("selecciones"));
    }

    @Test
    void laCuotaSiempreLaDecideElServidor() {
        String token = registrarYEntrar("10000004");
        long p = idPartido("Italia");
        String manipulado = "{\"tipo\":\"SIMPLE\",\"monto\":10,\"selecciones\":[{\"partidoId\":" + p
                + ",\"opcion\":\"LOCAL\",\"cuota\":99.99}],\"gananciaPotencial\":999999}";
        var r = api.post("/api/apuestas", token, manipulado);
        assertEquals(201, r.estado(), r.cuerpo());
        var ap = r.json().getAsJsonObject().get("apuestas").getAsJsonArray().get(0).getAsJsonObject();
        assertEquals(0, new BigDecimal("2.31").compareTo(
                ap.get("selecciones").getAsJsonArray().get(0).getAsJsonObject().get("cuota").getAsBigDecimal()));
        assertEquals(0, new BigDecimal("23.10").compareTo(ap.get("gananciaPotencial").getAsBigDecimal()));
    }

    @Test
    void erroresDeApuesta() {
        String token = registrarYEntrar("10000005");
        long p = idPartido("Italia");
        assertEquals(422, api.post("/api/apuestas", token, cupon("SIMPLE", "5000", p)).estado());
        assertEquals(400, api.post("/api/apuestas", token, cupon("SIMPLE", "-5", p)).estado());
        assertEquals(400, api.post("/api/apuestas", token, cupon("SIMPLE", "0", p)).estado());
        assertEquals(400, api.post("/api/apuestas", token, cupon("SIMPLE", "10.555", p)).estado());
        assertEquals(400, api.post("/api/apuestas", token, cupon("RARO", "10", p)).estado());
        assertEquals(400, api.post("/api/apuestas", token, cupon("SIMPLE", "10")).estado());
        assertEquals(404, api.post("/api/apuestas", token, cupon("SIMPLE", "10", 987654)).estado());
        assertEquals(400, api.post("/api/apuestas", token, "{\"tipo\":\"SIMPLE\",\"monto\":\"abc\",\"selecciones\":[]}").estado());
        assertEquals(400, api.post("/api/apuestas", token,
                "{\"tipo\":\"SIMPLE\",\"monto\":10,\"selecciones\":[{\"partidoId\":" + p + ",\"opcion\":\"CUALQUIERA\"}]}").estado());
    }

    @Test
    void saldoInsuficienteDevuelve422YRecargarLoResuelve() {
        String token = registrarYEntrar("10000006");
        long p = idPartido("Italia");
        for (int i = 0; i < 4; i++) {
            assertEquals(201, api.post("/api/apuestas", token, cupon("SIMPLE", "250", p)).estado());
        }
        var r = api.post("/api/apuestas", token, cupon("SIMPLE", "1", p));
        assertEquals(422, r.estado());
        assertTrue(r.json().getAsJsonObject().get("error").getAsString().contains("Saldo insuficiente"));

        var recarga = api.post("/api/billetera/recarga", token, "{\"monto\":500}");
        assertEquals(200, recarga.estado());
        assertEquals(0, new BigDecimal("500").compareTo(saldo(token)));
        assertEquals(201, api.post("/api/apuestas", token, cupon("SIMPLE", "100", p)).estado());
    }

    @Test
    void recargaValidaMontos() {
        String token = registrarYEntrar("10000007");
        assertEquals(400, api.post("/api/billetera/recarga", token, "{\"monto\":-10}").estado());
        assertEquals(400, api.post("/api/billetera/recarga", token, "{\"monto\":0}").estado());
        assertEquals(400, api.post("/api/billetera/recarga", token, "{\"monto\":10000.01}").estado());
        assertEquals(400, api.post("/api/billetera/recarga", token, "{}").estado());
        assertEquals(0, new BigDecimal("1000").compareTo(saldo(token)));
    }

    @Test
    void soloElAdministradorFinalizaPartidosYSeLiquidaAutomaticamente() {
        String ganador = registrarYEntrar("10000008");
        String perdedor = registrarYEntrar("10000009");
        long suecia = idPartido("Suecia");
        api.post("/api/apuestas", ganador, cupon("SIMPLE", "100", suecia)); // LOCAL, cuota 2.03
        api.post("/api/apuestas", perdedor, "{\"tipo\":\"SIMPLE\",\"monto\":100,\"selecciones\":["
                + "{\"partidoId\":" + suecia + ",\"opcion\":\"VISITA\"}]}");

        String ruta = "/api/admin/partidos/" + suecia + "/finalizar";
        assertEquals(401, api.post(ruta, null, "{\"golesLocal\":2,\"golesVisita\":0}").estado());
        assertEquals(403, api.post(ruta, ganador, "{\"golesLocal\":2,\"golesVisita\":0}").estado());

        String admin = entrar(ADMIN_DNI);
        assertEquals(400, api.post(ruta, admin, "{\"golesLocal\":-1,\"golesVisita\":0}").estado());
        var fin = api.post(ruta, admin, "{\"golesLocal\":2,\"golesVisita\":0}");
        assertEquals(200, fin.estado(), fin.cuerpo());
        assertEquals(1, fin.json().getAsJsonObject().get("ganadas").getAsInt());
        assertEquals(1, fin.json().getAsJsonObject().get("perdidas").getAsInt());
        assertEquals(409, api.post(ruta, admin, "{\"golesLocal\":2,\"golesVisita\":0}").estado());

        assertEquals(0, new BigDecimal("1103.00").compareTo(saldo(ganador))); // 900 + 100 * 2.03
        assertEquals(0, new BigDecimal("900").compareTo(saldo(perdedor)));
        assertEquals("GANADA", api.get("/api/apuestas?estado=GANADA", ganador).json().getAsJsonArray()
                .get(0).getAsJsonObject().get("estado").getAsString());
        assertEquals(0, api.get("/api/apuestas?estado=PENDIENTE", ganador).json().getAsJsonArray().size());
        assertEquals(409, api.post("/api/apuestas", ganador, cupon("SIMPLE", "10", suecia)).estado(),
                "ya no se puede apostar a un partido finalizado");
    }

    @Test
    void filtroDeEstadoInvalidoYAtaquesBasicos() {
        String token = registrarYEntrar("10000010");
        assertEquals(400, api.get("/api/apuestas?estado=PENDIENTE'%20OR%20'1'='1", token).estado());
        assertEquals(404, api.get("/api/no-existe", null).estado());
        assertEquals(405, api.enviar("DELETE", "/api/partidos", null, null).estado());
        assertEquals(404, api.get("/api/admin/partidos/abc/finalizar", null).estado());
    }

    @Test
    void cuerpoDemasiadoGrandeSeRechaza() {
        String enorme = "{\"dni\":\"" + "1".repeat(20_000) + "\"}";
        assertEquals(413, api.post("/api/auth/registro", null, enorme).estado());
    }

    @Test
    void corsSoloParaElOrigenConfigurado() {
        var permitido = api.enviar("GET", "/api/salud", null, null, "Origin", "http://localhost:5500");
        assertEquals("http://localhost:5500", permitido.cabeceras().firstValue("Access-Control-Allow-Origin").orElse(""));
        var otro = api.enviar("GET", "/api/salud", null, null, "Origin", "http://sitio-malicioso.com");
        assertTrue(otro.cabeceras().firstValue("Access-Control-Allow-Origin").isEmpty());
        var preflight = api.enviar("OPTIONS", "/api/apuestas", null, null, "Origin", "http://localhost:5500");
        assertEquals(204, preflight.estado());
    }

    @Test
    void erroresNoFiltranDetallesInternos() {
        var r = api.post("/api/auth/registro", null, "{\"dni\":\"abc\"}");
        assertFalse(r.cuerpo().contains("Exception"));
        assertFalse(r.cuerpo().contains("com.apuestatotal"));
        assertFalse(r.cuerpo().contains("at "));
    }
}
