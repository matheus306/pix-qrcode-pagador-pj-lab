package br.lab.pix.pagador.dominio;

/** Resultado da revalidação de um agendamento no dia da liquidação. */
public enum DecisaoRevalidacao {
    SEGUIR,
    NAO_EFETIVAR_COBRANCA_NAO_PAGAVEL,
    NAO_EFETIVAR_CHAVE_MUDOU,
    NAO_EFETIVAR_VALOR_DIVERGENTE,
    FALHA_OPERACIONAL
}
