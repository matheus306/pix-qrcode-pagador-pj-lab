package br.lab.pix.pagador.dominio;

/** Por que uma tentativa de liquidar um agendamento não deu certo. */
public enum MotivoFalha {
    SALDO_INSUFICIENTE,
    LIMITE_AGENDADO_INSUFICIENTE,
    FALHA_OPERACIONAL
}
