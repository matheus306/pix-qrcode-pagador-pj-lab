package br.lab.pix.pagador.dominio;

/** O que a tela de confirmação mostra sobre o recebedor. Não tem agência nem conta, de propósito. */
public record DestinatarioExibido(String nome, String documento, String nomePsp) {
}
