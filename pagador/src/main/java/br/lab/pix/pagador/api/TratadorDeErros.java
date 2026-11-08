package br.lab.pix.pagador.api;

import br.lab.pix.pagador.fluxo.FalhaDeFluxo;
import br.lab.pix.pagador.regras.RegraNaoImplementada;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class TratadorDeErros {

    /** Fluxo interrompido por uma regra: é a "mensagem na tela" do pagador. */
    @ExceptionHandler(FalhaDeFluxo.class)
    public ResponseEntity<Map<String, Object>> recusa(FalhaDeFluxo e) {
        return ResponseEntity.unprocessableEntity().body(Map.of(
                "codigo", e.getRecusa().codigo(),
                "mensagem", e.getRecusa().mensagem()));
    }

    /** O fluxo chegou numa regra que você ainda não escreveu. */
    @ExceptionHandler(RegraNaoImplementada.class)
    public ResponseEntity<Map<String, Object>> naoImplementada(RegraNaoImplementada e) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of(
                "regra", e.getRegra(),
                "mensagem", e.getMessage(),
                "proximoPasso", "Implemente a regra e rode: mvn -pl pagador test -Dtest='" + filtroDeTeste(e.getRegra()) + "'"));
    }

    static String filtroDeTeste(String regra) {
        int n = Integer.parseInt(regra.substring(1));
        String classe = n <= 6 ? "RegrasLeituraQrTest"
                : n <= 10 ? "RegrasSegurancaQrTest"
                : n <= 20 ? "RegrasCobrancaTest"
                : n <= 25 ? "RegrasExibicaoTest"
                : n <= 30 ? "RegrasPagamentoTest"
                : n <= 36 ? "RegrasAgendamentoTest"
                : n == 37 ? "RegrasComprovanteTest"
                : "RegrasSplitTributarioTest";
        return n >= 37 ? classe : classe + "$" + regra + "*";
    }
}
