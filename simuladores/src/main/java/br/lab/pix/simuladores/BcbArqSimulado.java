package br.lab.pix.simuladores;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Interface ARQ do BCB simulada: a lista de certificados de sites de QR Code dinâmico (CERTQRC)
 * e uma consulta de revogação (no mundo real, CRL/OCSP da autoridade certificadora).
 */
@RestController
class BcbArqSimulado {

    record Certificado(String id, List<String> nomes, boolean ev) {
    }

    /** Qual certificado cada "site" apresenta e com que versão de TLS ele conversa. */
    record Site(String certificado, String tls) {
    }

    static final Map<String, Site> SITES = Map.of(
            "pix.recebedor-a.local", new Site("cert-recebedor-a", "TLSv1.3"),
            "qrcode-h.recebedor-a.local", new Site("cert-recebedor-a-homologacao", "TLSv1.2"),
            "pix.recebedor-revogado.local", new Site("cert-revogado", "TLSv1.2"),
            "pix.nao-cadastrado.local", new Site("cert-desconhecido", "TLSv1.2"),
            "pix.tls-antigo.local", new Site("cert-tls-antigo", "TLSv1.1"),
            "pix.wildcard.local", new Site("cert-wildcard", "TLSv1.2"),
            "pix.sem-ev.local", new Site("cert-sem-ev", "TLSv1.2"));

    private static final List<Certificado> CERTQRC = List.of(
            new Certificado("cert-recebedor-a", List.of("pix.recebedor-a.local"), true),
            new Certificado("cert-recebedor-a-homologacao", List.of("qrcode-h.recebedor-a.local"), true),
            new Certificado("cert-revogado", List.of("pix.recebedor-revogado.local"), true),
            new Certificado("cert-tls-antigo", List.of("pix.tls-antigo.local"), true),
            new Certificado("cert-wildcard", List.of("pix.wildcard.local", "*.wildcard.local"), true),
            new Certificado("cert-sem-ev", List.of("pix.sem-ev.local"), false));

    @GetMapping("/bcb/arq/certqrc")
    List<Certificado> certqrc() {
        return CERTQRC;
    }

    @GetMapping("/bcb/revogacao/{id}")
    Map<String, Object> revogacao(@PathVariable String id) {
        return Map.of("id", id, "revogado", "cert-revogado".equals(id));
    }
}
