package br.lab.pix.pagador.emv;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Leitor TLV do padrão EMV (ID com 2 dígitos, tamanho com 2 dígitos, valor).
 * Só separa os campos: não valida CRC nem regra nenhuma do Pix (isso é com você).
 */
public final class EmvParser {

    private EmvParser() {
    }

    public static EmvDados parse(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("BR Code nulo");
        }
        Map<String, String> campos = new LinkedHashMap<>();
        int i = 0;
        while (i < texto.length()) {
            if (i + 4 > texto.length()) {
                throw new IllegalArgumentException("Campo truncado na posição " + i);
            }
            String id = texto.substring(i, i + 2);
            int tamanho;
            try {
                tamanho = Integer.parseInt(texto.substring(i + 2, i + 4));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Tamanho inválido na posição " + (i + 2));
            }
            int inicio = i + 4;
            int fim = inicio + tamanho;
            if (fim > texto.length()) {
                throw new IllegalArgumentException("Valor do campo " + id + " passa do fim do texto");
            }
            campos.put(id, texto.substring(inicio, fim));
            i = fim;
        }
        return new EmvDados(campos);
    }
}
