package br.lab.pix.pagador.dominio;

import java.util.List;

/** Chave do JWK Set publicado no endereço do "jku" (Manual de Segurança do SFN, Vol. II, seção 4.2). */
public record ChaveJwk(String kid, String x5t, String x5tS256, List<String> keyOps) {
}
