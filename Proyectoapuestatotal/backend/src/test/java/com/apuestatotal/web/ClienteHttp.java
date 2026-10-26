package com.apuestatotal.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/** Cliente HTTP mínimo para las pruebas de integración (usa java.net.http del JDK). */
final class ClienteHttp {

    record Resp(int estado, String cuerpo, java.net.http.HttpHeaders cabeceras) {
        JsonElement json() {
            return JsonParser.parseString(cuerpo);
        }
    }

    private final HttpClient http = HttpClient.newHttpClient();
    private final String base;

    ClienteHttp(int puerto) {
        this.base = "http://localhost:" + puerto;
    }

    Resp enviar(String metodo, String ruta, String token, String json, String... cabecerasExtra) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + ruta));
            if (token != null) {
                b.header("Authorization", "Bearer " + token);
            }
            for (int i = 0; i + 1 < cabecerasExtra.length; i += 2) {
                b.header(cabecerasExtra[i], cabecerasExtra[i + 1]);
            }
            b.header("Content-Type", "application/json");
            b.method(metodo, json == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
            HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Resp(r.statusCode(), r.body(), r.headers());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    Resp get(String ruta, String token) {
        return enviar("GET", ruta, token, null);
    }

    Resp post(String ruta, String token, String json) {
        return enviar("POST", ruta, token, json);
    }
}
