package br.lab.pix.pagador.dominio;

public enum AcaoPosTimeout {
    MARCAR_LIQUIDADO,
    MARCAR_REJEITADO,
    CONSULTAR_NOVAMENTE,
    ESTORNAR_SEM_LIQUIDACAO,
    REENVIAR_ORDEM
}
