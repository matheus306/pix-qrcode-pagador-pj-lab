package br.lab.pix.pagador.dominio;

/**
 * Forma de iniciação informada na pacs.008 (Manual de Padrões, seção 3.2).
 * Neste laboratório o pagador usa QRES (QR estático) e QRDN (QR dinâmico).
 */
public enum FormaIniciacao {
    MANU, DICT, QRES, QRDN, INIC, APES, APDN, AUTO
}
