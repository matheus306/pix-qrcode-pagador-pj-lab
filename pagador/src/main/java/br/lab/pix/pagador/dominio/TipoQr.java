package br.lab.pix.pagador.dominio;

/** Tipo do QR Code Pix (Manual de Padrões para Iniciação do Pix, seção 2.4). */
public enum TipoQr {
    ESTATICO,
    DINAMICO,
    COMPOSTO,
    INVALIDO
}
