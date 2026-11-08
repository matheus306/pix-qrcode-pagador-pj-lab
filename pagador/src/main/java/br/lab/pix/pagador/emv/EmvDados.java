package br.lab.pix.pagador.emv;

import java.util.Map;

/**
 * Campos de primeiro nível de um BR Code (ID -> valor), na ordem em que aparecem.
 * Use {@link #template(String)} para abrir um campo que contém subcampos (ex.: 26, 62, 80).
 */
public record EmvDados(Map<String, String> campos) {

    public String valor(String id) {
        return campos.get(id);
    }

    public boolean contem(String id) {
        return campos.containsKey(id);
    }

    /** Subcampos de um template. Devolve mapa vazio se o ID não existir. */
    public Map<String, String> template(String id) {
        String valor = campos.get(id);
        return valor == null ? Map.of() : EmvParser.parse(valor).campos();
    }
}
