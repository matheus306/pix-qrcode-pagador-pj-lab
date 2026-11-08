package br.lab.pix.pagador.dominio;

/** Acontecimentos na vida de um Pix Agendado que podem (ou não) gerar notificação. */
public enum EventoAgendamento {
    LIQUIDADO,
    FALHA_SALDO_PRIMEIRA_TENTATIVA,
    FALHA_SALDO_ULTIMA_TENTATIVA,
    FALHA_LIMITE_AGENDADO,
    CHAVE_MUDOU_OU_INEXISTENTE,
    COBRANCA_NAO_PAGAVEL,
    FALHA_OPERACIONAL_INTERMEDIARIA,
    FALHA_OPERACIONAL_ULTIMA
}
