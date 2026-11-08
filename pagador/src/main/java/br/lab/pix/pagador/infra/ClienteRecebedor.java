package br.lab.pix.pagador.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Busca o payload na URL do QR dinâmico.
 * <p>
 * No mundo real a URL é acessada sempre por HTTPS. No laboratório os sites dos PSPs recebedores
 * são o container "simuladores" respondendo por vários nomes, em HTTP, e o simulador informa nos
 * cabeçalhos X-Simulacao-Tls e X-Simulacao-Certificado o que um handshake TLS teria mostrado.
 */
@Component
public class ClienteRecebedor {

    private final int portaRecebedor;

    public ClienteRecebedor(@Value("${pix.simulacao.porta-recebedor}") int portaRecebedor) {
        this.portaRecebedor = portaRecebedor;
    }

    public Http.Resposta buscar(String urlSemProtocolo, Map<String, String> parametros) {
        String url = paraHttp(urlSemProtocolo);
        if (!parametros.isEmpty()) {
            url += "?" + parametros.entrySet().stream()
                    .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
        }
        return Http.get(url);
    }

    /** Converte "host/caminho" (ou "http://host/caminho") para a URL acessível no laboratório. */
    public String paraHttp(String url) {
        String semProtocolo = url.replaceFirst("^[a-zA-Z]+://", "");
        int barra = semProtocolo.indexOf('/');
        String host = barra < 0 ? semProtocolo : semProtocolo.substring(0, barra);
        String caminho = barra < 0 ? "" : semProtocolo.substring(barra);
        String porta = portaRecebedor == 80 ? "" : ":" + portaRecebedor;
        return "http://" + host + porta + caminho;
    }

    public static String host(String urlSemProtocolo) {
        String semProtocolo = urlSemProtocolo.replaceFirst("^[a-zA-Z]+://", "");
        int barra = semProtocolo.indexOf('/');
        return barra < 0 ? semProtocolo : semProtocolo.substring(0, barra);
    }
}
