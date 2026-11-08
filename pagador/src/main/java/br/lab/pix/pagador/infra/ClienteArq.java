package br.lab.pix.pagador.infra;

import br.lab.pix.pagador.dominio.CertificadoSite;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Lista de certificados de sites de QR Code (CERTQRC) baixada da interface ARQ do BCB (simulada).
 * O manual manda baixar no máximo 1 vez a cada 24h e manter em cache. A revogação é consultada
 * a cada leitura, porque pode acontecer entre uma atualização e outra da lista.
 */
@Component
public class ClienteArq {

    public record ItemCertqrc(String id, List<String> nomes, boolean ev) {
    }

    private final String baseUrl;
    private final ObjectMapper json;
    private volatile Map<String, ItemCertqrc> cache;
    private volatile Instant baixadoEm = Instant.EPOCH;

    public ClienteArq(@Value("${pix.bcb-url}") String baseUrl, ObjectMapper json) {
        this.baseUrl = baseUrl;
        this.json = json;
    }

    /** Certificado que o site apresentou, como o BCB conhece. Nulo = não está na lista. */
    public CertificadoSite certificado(String idCertificado) {
        if (idCertificado == null) {
            return null;
        }
        ItemCertqrc item = lista().get(idCertificado);
        if (item == null) {
            return null;
        }
        return new CertificadoSite(item.nomes(), item.ev(), revogado(idCertificado));
    }

    private Map<String, ItemCertqrc> lista() {
        if (cache == null || Duration.between(baixadoEm, Instant.now()).toHours() >= 24) {
            Http.Resposta r = Http.get(baseUrl + "/bcb/arq/certqrc");
            if (r.status() == 200) {
                try {
                    List<ItemCertqrc> itens = json.readValue(r.corpo(), new TypeReference<>() { });
                    cache = itens.stream().collect(java.util.stream.Collectors.toMap(ItemCertqrc::id, i -> i));
                    baixadoEm = Instant.now();
                } catch (Exception e) {
                    throw new IllegalStateException("Lista CERTQRC ilegível", e);
                }
            }
        }
        return cache == null ? Map.of() : cache;
    }

    private boolean revogado(String idCertificado) {
        Http.Resposta r = Http.get(baseUrl + "/bcb/revogacao/" + idCertificado);
        // Sem resposta sobre revogação, não dá para confiar no certificado.
        return r.status() != 200 || r.corpo().contains("true");
    }
}
