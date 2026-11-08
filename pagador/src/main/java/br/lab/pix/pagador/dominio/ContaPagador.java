package br.lab.pix.pagador.dominio;

import java.math.BigDecimal;

/** Conta PJ do pagador, com o que as regras precisam saber dela. */
public record ContaPagador(
        String id,
        String cnpj,
        String razaoSocial,
        String ispb,
        String nomePsp,
        String agencia,
        String numero,
        String codMun,
        BigDecimal saldo,
        BigDecimal limiteCredito,
        String nomeLinhaCredito,
        BigDecimal limitePixAgendadoDiario) {

    public ContaPagador comSaldo(BigDecimal novoSaldo) {
        return new ContaPagador(id, cnpj, razaoSocial, ispb, nomePsp, agencia, numero, codMun,
                novoSaldo, limiteCredito, nomeLinhaCredito, limitePixAgendadoDiario);
    }

    public ContaPagador comLimitePixAgendado(BigDecimal novoLimite) {
        return new ContaPagador(id, cnpj, razaoSocial, ispb, nomePsp, agencia, numero, codMun,
                saldo, limiteCredito, nomeLinhaCredito, novoLimite);
    }
}
