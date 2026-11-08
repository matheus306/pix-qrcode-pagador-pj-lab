package br.lab.pix.simuladores;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** DICT simulado: chave Pix -> dados da conta do recebedor. */
@RestController
class DictSimulado {

    static final String CHAVE_FORNECEDOR_A = "pagamentos@fornecedor-a.com.br";
    static final String CHAVE_ALEATORIA = "123e4567-e12b-12d1-a456-426655440000";
    static final String CHAVE_PF = "+5561999990001";
    static final String CHAVE_FRAUDE = "fraude@golpe.com.br";
    static final String CHAVE_CONTA_ENCERRADA = "conta-encerrada@fornecedor-b.com.br";
    static final String CHAVE_SPI_LENTO = "lento@fornecedor-c.com.br";
    static final String CHAVE_DICT_FORA = "dict-fora@fornecedor-d.com.br";
    static final String CHAVE_SPI_SUMIU = "sumiu@fornecedor-e.com.br";

    private final Map<String, Map<String, Object>> chaves = new ConcurrentHashMap<>();

    DictSimulado() {
        reiniciar();
    }

    @PostMapping("/simulador/dict/reiniciar")
    void reiniciar() {
        chaves.clear();
        pj(CHAVE_FORNECEDOR_A, "33444555000172", "FORNECEDOR A COMERCIO LTDA", "Fornecedor A", false);
        pj(CHAVE_ALEATORIA, "55666777000190", "LOJA EXEMPLO LTDA", null, false);
        pj(CHAVE_FRAUDE, "66777888000105", "GOLPE CERTO LTDA", "Promoções Imperdíveis", true);
        pj(CHAVE_CONTA_ENCERRADA, "77888999000150", "FORNECEDOR B LTDA", "Fornecedor B", false);
        pj(CHAVE_SPI_LENTO, "88999000000140", "FORNECEDOR C LTDA", "Fornecedor C", false);
        pj(CHAVE_SPI_SUMIU, "12ABC34501DE35", "FORNECEDOR E LTDA", "Fornecedor E", false);
        Map<String, Object> pf = base(CHAVE_PF, "F", "12345678901", "Maria Souza", null, false);
        pf.put("ispb", "22222222");
        pf.put("nomePsp", "Banco Recebedor B");
        chaves.put(CHAVE_PF, pf);
    }

    @GetMapping("/dict/chaves/{chave}")
    ResponseEntity<Map<String, Object>> consultar(@PathVariable String chave) {
        if (CHAVE_DICT_FORA.equals(chave)) {
            return ResponseEntity.status(503).build();
        }
        Map<String, Object> dados = chaves.get(chave);
        return dados == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(dados);
    }

    /** Simula a chave ter sido levada para outra empresa (portabilidade/reivindicação). */
    @PostMapping("/simulador/dict/{chave}/trocar-titular")
    Map<String, Object> trocarTitular(@PathVariable String chave) {
        Map<String, Object> d = chaves.get(chave);
        d.put("cpfCnpj", "99888777000166");
        d.put("nome", "OUTRA EMPRESA QUALQUER LTDA");
        d.put("nomeFantasia", "Outra Empresa");
        return d;
    }

    private void pj(String chave, String cnpj, String nome, String fantasia, boolean fraude) {
        chaves.put(chave, base(chave, "J", cnpj, nome, fantasia, fraude));
    }

    private static Map<String, Object> base(String chave, String tipo, String doc, String nome, String fantasia,
                                            boolean fraude) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("chave", chave);
        d.put("tipoPessoa", tipo);
        d.put("cpfCnpj", doc);
        d.put("nome", nome);
        d.put("nomeFantasia", fantasia);
        d.put("ispb", "11111111");
        d.put("nomePsp", "Banco Recebedor A");
        d.put("agencia", "4321");
        d.put("conta", "887766");
        d.put("tipoConta", "CACC");
        d.put("suspeitaFraude", fraude);
        return d;
    }
}
