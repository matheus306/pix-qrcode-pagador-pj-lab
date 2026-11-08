package br.lab.pix.pagador.dominio;

/** Cabeçalho JOSE do payload assinado (Manual de Segurança do SFN, Vol. II, seção 4.2). */
public record CabecalhoJws(String alg, String kid, String jku, String x5t, String x5tS256) {
}
