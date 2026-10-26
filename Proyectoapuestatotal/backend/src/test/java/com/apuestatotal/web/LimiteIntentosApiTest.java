package com.apuestatotal.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.apuestatotal.Aplicacion;
import com.apuestatotal.config.Configuracion;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LimiteIntentosApiTest {

    @Test
    void bloqueaLaFuerzaBrutaSobreElLogin() throws Exception {
        Configuracion cfg = Configuracion.cargar(Map.of(
                "APUESTATOTAL_PORT", "0",
                "APUESTATOTAL_DB_URL", "jdbc:h2:mem:lim_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "APUESTATOTAL_LOGIN_MAX", "3",
                "APUESTATOTAL_REGISTER_MAX", "2"));
        Servidor servidor = Aplicacion.construir(cfg, Clock.systemUTC());
        servidor.iniciar();
        try {
            ClienteHttp api = new ClienteHttp(servidor.puerto());
            for (int i = 0; i < 3; i++) {
                assertEquals(401, api.post("/api/auth/login", null, "{\"dni\":\"3000000" + i + "\"}").estado());
            }
            assertEquals(429, api.post("/api/auth/login", null, "{\"dni\":\"30000009\"}").estado());

            assertEquals(201, api.post("/api/auth/registro", null, "{\"dni\":\"40000001\"}").estado());
            assertEquals(201, api.post("/api/auth/registro", null, "{\"dni\":\"40000002\"}").estado());
            assertEquals(429, api.post("/api/auth/registro", null, "{\"dni\":\"40000003\"}").estado());
        } finally {
            servidor.detener();
        }
    }
}
