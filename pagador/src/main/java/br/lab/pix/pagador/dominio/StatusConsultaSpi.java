package br.lab.pix.pagador.dominio;

/** Resposta da consulta ao SPI sobre uma ordem que ficou sem retorno. */
public enum StatusConsultaSpi {
    LIQUIDADO,
    REJEITADO,
    EM_PROCESSAMENTO,
    NAO_ENCONTRADO
}
