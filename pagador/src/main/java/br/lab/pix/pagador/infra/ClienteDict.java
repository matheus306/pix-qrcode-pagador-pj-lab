package br.lab.pix.pagador.infra;

import br.lab.pix.pagador.dominio.DadosDict;
import br.lab.pix.pagador.dominio.RespostaDict;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Consulta ao DICT (simulado). Não interpreta nada: quem decide é a regra R21. */
@Component
public class ClienteDict {

    private final String baseUrl;
    private final ObjectMapper json;

    public ClienteDict(@Value("${pix.dict-url}") String baseUrl, ObjectMapper json) {
        this.baseUrl = baseUrl;
        this.json = json;
    }

    public RespostaDict consultar(String chave) {
        Http.Resposta r = Http.get(baseUrl + "/dict/chaves/" + URLEncoder.encode(chave, StandardCharsets.UTF_8));
        if (r.status() != 200) {
            return new RespostaDict(r.status(), null);
        }
        try {
            return new RespostaDict(200, json.readValue(r.corpo(), DadosDict.class));
        } catch (Exception e) {
            return new RespostaDict(0, null);
        }
    }
}
