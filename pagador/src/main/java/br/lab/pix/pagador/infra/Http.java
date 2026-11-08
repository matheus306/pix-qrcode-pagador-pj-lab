package br.lab.pix.pagador.infra;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** GET simples. Status 0 = não conseguiu falar com o servidor. */
public final class Http {

    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private Http() {
    }

    public record Resposta(int status, String corpo, HttpResponse<String> bruta) {
        public String cabecalho(String nome) {
            return bruta == null ? null : bruta.headers().firstValue(nome).orElse(null);
        }
    }

    public static Resposta get(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<String> resp = CLIENTE.send(req, HttpResponse.BodyHandlers.ofString());
            return new Resposta(resp.statusCode(), resp.body(), resp);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Resposta(0, null, null);
        } catch (Exception e) {
            return new Resposta(0, null, null);
        }
    }
}
