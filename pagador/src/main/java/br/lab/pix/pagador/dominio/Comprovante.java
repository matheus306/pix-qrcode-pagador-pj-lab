package br.lab.pix.pagador.dominio;

import java.util.Map;

/** Comprovante exibido ao pagador: um título e os campos na ordem em que aparecem. */
public record Comprovante(String titulo, Map<String, String> campos) {
}
