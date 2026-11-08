package br.lab.pix.pagador.dominio;

/** Resultado bruto da consulta ao DICT: status HTTP e, se houver, os dados da chave. */
public record RespostaDict(int httpStatus, DadosDict dados) {
}
