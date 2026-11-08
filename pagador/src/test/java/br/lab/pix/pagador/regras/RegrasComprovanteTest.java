package br.lab.pix.pagador.regras;

import br.lab.pix.pagador.dominio.Comprovante;
import br.lab.pix.pagador.dominio.DadosComprovante;
import br.lab.pix.pagador.dominio.DadosDict;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RegrasComprovanteTest {

    final RegrasComprovante regras = new RegrasComprovante();

    final DadosDict recebedor = new DadosDict("pagamentos@fornecedor-a.com.br", "J", "33444555000172",
            "FORNECEDOR A COMERCIO LTDA", "Fornecedor A", "11111111", "Banco Recebedor A", "4321", "887766", "CACC", false);

    DadosComprovante dados(boolean agendamento) {
        return new DadosComprovante(agendamento, "EMPRESA PAGADORA LTDA", "11222333000181", "PSP Lab", recebedor,
                "PEDIDO2026X1", new BigDecimal("950.00"), "E12345678202611081855abc12345678",
                agendamento ? null : Instant.parse("2026-11-08T18:55:30Z"),
                agendamento ? LocalDate.of(2026, 11, 13) : null,
                agendamento ? LocalDate.of(2026, 11, 8) : null,
                Map.of("Valor original", "R$ 1.000,00", "Desconto", "R$ 50,00"));
    }

    String tudo(Comprovante c) {
        return c.titulo() + " " + c.campos().keySet() + " " + c.campos().values();
    }

    @Test
    void comprovanteDePagamentoTemOsCamposMinimos() {
        Comprovante c = regras.montarComprovante(dados(false));
        String texto = tudo(c);
        assertThat(c.titulo()).containsIgnoringCase("Pix confirmado");
        assertThat(texto).contains("EMPRESA PAGADORA LTDA", "11222333000181", "PSP Lab");
        assertThat(texto).contains("Fornecedor A", "33444555000172", "Banco Recebedor A");
        assertThat(texto).contains("PEDIDO2026X1", "950,00", "E12345678202611081855abc12345678");
        assertThat(texto).contains("R$ 1.000,00", "R$ 50,00");
    }

    @Test
    void dataEHoraEmBrasiliaComSegundos() {
        // 18:55:30 UTC = 15:55:30 em Brasília
        assertThat(tudo(regras.montarComprovante(dados(false)))).contains("08/11/2026 15:55:30");
    }

    @Test
    void nuncaMostraChaveAgenciaEConta() {
        String texto = tudo(regras.montarComprovante(dados(false)));
        assertThat(texto).doesNotContain("pagamentos@fornecedor-a.com.br").doesNotContain("887766").doesNotContain("4321");
    }

    @Test
    void comprovanteDeAgendamentoSeDiferencia() {
        Comprovante c = regras.montarComprovante(dados(true));
        String texto = tudo(c);
        assertThat(c.titulo()).contains("Agendamento Pix");
        assertThat(texto).contains("13/11/2026", "08/11/2026", "23:59");
        assertThat(texto).doesNotContain("pagamentos@fornecedor-a.com.br").doesNotContain("887766");
    }
}
