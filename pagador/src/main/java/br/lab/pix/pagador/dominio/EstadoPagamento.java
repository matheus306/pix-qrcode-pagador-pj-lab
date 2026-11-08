package br.lab.pix.pagador.dominio;

/**
 * Estados internos do pagamento no PSP pagador. O BCB não define estes estados:
 * é uma proposta de desenho para cumprir as regras (ver docs/MAQUINA_DE_ESTADOS.md).
 */
public enum EstadoPagamento {
    PRONTO_PARA_CONFIRMACAO,
    CANCELADO_PELO_USUARIO,
    RECUSADO_INTERNO,
    AUTORIZADO,
    ENVIADO_SPI,
    EM_VERIFICACAO,
    LIQUIDADO,
    REJEITADO,
    AGENDADO,
    REVALIDANDO,
    AGUARDANDO_RETENTATIVA,
    NAO_EFETIVADO
}
