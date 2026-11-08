package br.lab.pix.pagador.fluxo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * O que o pagador manda ao apertar "Pagar" (ou "Agendar").
 *
 * @param dataPagamento nula ou hoje = pagar agora; data futura = agendar (só cobrança com vencimento)
 */
public record Confirmacao(BigDecimal valor, BigDecimal valorSaque, BigDecimal valorTroco,
                          String respostaPagador, LocalDate dataPagamento) {
}
