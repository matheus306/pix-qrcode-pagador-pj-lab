package br.lab.pix.pagador.dominio;

/** Tipo de recurso apontado pela URL do QR dinâmico (Manual de Padrões, seção 2.5.2). */
public enum TipoCobranca {
    /** Sem fragmento ou com "/cob/": cobrança imediata. */
    IMEDIATA,
    /** Fragmento "/cobv/": cobrança com vencimento. */
    COM_VENCIMENTO,
    /** Fragmento "/rec/": parâmetros de recorrência (Pix Automático). */
    RECORRENCIA
}
