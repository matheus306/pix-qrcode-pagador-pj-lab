package br.lab.pix.pagador.regras;

import br.lab.pix.pagador.dominio.FormaIniciacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class RegrasSplitTributarioTest {

    final RegrasSplitTributario regras = new RegrasSplitTributario();

    @ParameterizedTest
    @EnumSource(value = FormaIniciacao.class, names = {"QRES", "APES", "MANU", "DICT"})
    void pjParaPjNasFormasPermitidas(FormaIniciacao forma) {
        assertThat(regras.podeInformarTributo("11222333000181", "33444555000172", forma)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = FormaIniciacao.class, names = {"QRDN", "APDN", "AUTO", "INIC"})
    void naoEmQrDinamicoNemAutomatico(FormaIniciacao forma) {
        assertThat(regras.podeInformarTributo("11222333000181", "33444555000172", forma)).isFalse();
    }

    @Test
    void soEntrePessoasJuridicas() {
        assertThat(regras.podeInformarTributo("11222333000181", "12345678901", FormaIniciacao.QRES)).isFalse();
        assertThat(regras.podeInformarTributo("12345678901", "33444555000172", FormaIniciacao.QRES)).isFalse();
    }

    @Test
    void cnpjAlfanumericoConta() {
        assertThat(regras.podeInformarTributo("12ABC34501DE35", "33444555000172", FormaIniciacao.QRES)).isTrue();
    }
}
