package br.lab.pix.pagador.dominio;

import java.math.BigDecimal;

/** Valores digitados pelo pagador na tela de confirmação (null = não digitou). */
public record ValoresInformados(BigDecimal original, BigDecimal saque, BigDecimal troco) {

    public static ValoresInformados nenhum() {
        return new ValoresInformados(null, null, null);
    }
}
