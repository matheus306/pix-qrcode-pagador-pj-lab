package br.lab.pix.pagador.regras;

import br.lab.pix.pagador.dominio.CalendarioDiasUteis;
import br.lab.pix.pagador.dominio.Cobranca.DetalheRetirada;
import br.lab.pix.pagador.dominio.Cobranca.Retirada;
import br.lab.pix.pagador.dominio.Cobranca.Valor;
import br.lab.pix.pagador.dominio.CodigoRecusa;
import br.lab.pix.pagador.dominio.ContaPagador;
import br.lab.pix.pagador.dominio.ValoresDaOrdem;
import br.lab.pix.pagador.dominio.ValoresInformados;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RegrasCobrancaTest {

    final RegrasCobranca regras = new RegrasCobranca();

    static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    static Valor fixo(String original) {
        return new Valor(original, 0, null, null, null, null, null, null);
    }

    @Nested
    class R11_HttpDoPayload {
        @Test
        void duzentosSegue() {
            assertThat(regras.interpretarHttpDoPayload(200)).isEmpty();
        }

        @Test
        void quatroZeroQuatroEQuatroDezSaoCobrancaEncerrada() {
            assertThat(regras.interpretarHttpDoPayload(404)).get().extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.COBRANCA_NAO_PAGAVEL);
            assertThat(regras.interpretarHttpDoPayload(410)).get().extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.COBRANCA_NAO_PAGAVEL);
        }

        @Test
        void restoEhRecebedorIndisponivel() {
            assertThat(regras.interpretarHttpDoPayload(503)).get().extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.PAYLOAD_INDISPONIVEL);
            assertThat(regras.interpretarHttpDoPayload(0)).get().extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.PAYLOAD_INDISPONIVEL);
        }
    }

    @Nested
    class R12_Status {
        @Test
        void soAtivaPassa() {
            assertThat(regras.validarStatus("ATIVA")).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"CONCLUIDA", "CONCLUÍDA", "REMOVIDO_PELO_USUARIO_RECEBEDOR", "REMOVIDA_PELO_USUARIO_RECEBEDOR",
                "REMOVIDO_PELO_PSP", "REMOVIDA_PELO_PSP", "CONCLUÍDA_POR_OUTRO_ARRANJO", "REMOVIDA_PELO_PSP_BOLETO", "QUALQUER"})
        void restoNaoPaga(String status) {
            assertThat(regras.validarStatus(status)).get().extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.COBRANCA_NAO_PAGAVEL);
        }

        @Test
        void nuloNaoPaga() {
            assertThat(regras.validarStatus(null)).isPresent();
        }
    }

    @Nested
    class R13_Expiracao {
        final Instant criacao = Instant.parse("2026-11-08T12:00:00Z");

        @Test
        void padraoDe24hQuandoNaoInformado() {
            assertThat(regras.validarExpiracao(criacao, null, Instant.parse("2026-11-09T11:59:59Z"))).isEmpty();
            assertThat(regras.validarExpiracao(criacao, null, Instant.parse("2026-11-09T12:00:00Z"))).isEmpty();
            assertThat(regras.validarExpiracao(criacao, null, Instant.parse("2026-11-09T12:00:01Z"))).get()
                    .extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.QR_EXPIRADO);
        }

        @Test
        void usaAExpiracaoInformada() {
            assertThat(regras.validarExpiracao(criacao, 3600, Instant.parse("2026-11-08T12:59:00Z"))).isEmpty();
            assertThat(regras.validarExpiracao(criacao, 3600, Instant.parse("2026-11-08T13:01:00Z"))).isPresent();
        }
    }

    @Nested
    class R14_TxidDinamico {
        @Test
        void entre26e35Alfanumericos() {
            assertThat(regras.validarTxidDinamico("a".repeat(26))).isEmpty();
            assertThat(regras.validarTxidDinamico("A1".repeat(17) + "Z")).isEmpty();
        }

        @Test
        void foraDoPadrao() {
            assertThat(regras.validarTxidDinamico("a".repeat(25))).get().extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.QR_INVALIDO);
            assertThat(regras.validarTxidDinamico("a".repeat(36))).isPresent();
            assertThat(regras.validarTxidDinamico("a".repeat(25) + "-")).isPresent();
            assertThat(regras.validarTxidDinamico(null)).isPresent();
        }
    }

    @Nested
    class R15_FormatoDoValor {
        @ParameterizedTest
        @ValueSource(strings = {"0.00", "1.00", "123.99", "123456789.23", "9999999999.99"})
        void validos(String v) {
            assertThat(regras.valorNoFormato(v)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"1.5", "1,50", "-1.00", "1", ".50", "12345678901.00", "1.000"})
        void invalidos(String v) {
            assertThat(regras.valorNoFormato(v)).isFalse();
        }

        @Test
        void nuloEhInvalido() {
            assertThat(regras.valorNoFormato(null)).isFalse();
        }
    }

    @Nested
    class R16_ValoresInformados {
        @Test
        void valorFixoNaoPodeSerAlterado() {
            assertThat(regras.validarValoresInformados(fixo("250.00"), ValoresInformados.nenhum())).isEmpty();
            assertThat(regras.validarValoresInformados(fixo("250.00"), new ValoresInformados(bd("250"), null, null))).isEmpty();
            assertThat(regras.validarValoresInformados(fixo("250.00"), new ValoresInformados(bd("300.00"), null, null))).get()
                    .extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.VALOR_INVALIDO);
        }

        @Test
        void modalidadeAusenteValeZero() {
            Valor semModalidade = new Valor("250.00", null, null, null, null, null, null, null);
            assertThat(regras.validarValoresInformados(semModalidade, new ValoresInformados(bd("1.00"), null, null))).isPresent();
        }

        @Test
        void valorEditavelComSugestaoZeroObrigaADigitar() {
            Valor editavel = new Valor("0.00", 1, null, null, null, null, null, null);
            assertThat(regras.validarValoresInformados(editavel, ValoresInformados.nenhum())).isPresent();
            assertThat(regras.validarValoresInformados(editavel, new ValoresInformados(bd("120.50"), null, null))).isEmpty();
        }

        @Test
        void saqueFixoNaoPodeMudar() {
            Valor saque = new Valor("0.00", 0, new Retirada(new DetalheRetirada("100.00", 0, "AGTEC", "12345678"), null),
                    null, null, null, null, null);
            assertThat(regras.validarValoresInformados(saque, ValoresInformados.nenhum())).isEmpty();
            assertThat(regras.validarValoresInformados(saque, new ValoresInformados(null, bd("50.00"), null))).isPresent();
        }

        @Test
        void trocoEditavel() {
            Valor troco = new Valor("80.00", 0, new Retirada(null, new DetalheRetirada("0.00", 1, "AGTEC", "12345678")),
                    null, null, null, null, null);
            assertThat(regras.validarValoresInformados(troco, new ValoresInformados(null, null, bd("20.00")))).isEmpty();
        }

        @Test
        void saqueETrocoJuntosNaoExistem() {
            Valor ambos = new Valor("10.00", 0, new Retirada(
                    new DetalheRetirada("10.00", 0, "AGTEC", "12345678"),
                    new DetalheRetirada("10.00", 0, "AGTEC", "12345678")), null, null, null, null, null);
            assertThat(regras.validarValoresInformados(ambos, ValoresInformados.nenhum())).isPresent();
        }
    }

    @Nested
    class R17_ValoresDaOrdem {
        @Test
        void compraNormalEhIpay() {
            ValoresDaOrdem v = regras.valoresDaOrdem(fixo("250.00"), ValoresInformados.nenhum());
            assertThat(v.purpose()).isEqualTo("IPAY");
            assertThat(v.total()).isEqualByComparingTo("250.00");
            assertThat(v.valorRetirada()).isNull();
        }

        @Test
        void valorDigitadoEntraNoTotal() {
            Valor editavel = new Valor("0.00", 1, null, null, null, null, null, null);
            assertThat(regras.valoresDaOrdem(editavel, new ValoresInformados(bd("120.50"), null, null)).total())
                    .isEqualByComparingTo("120.50");
        }

        @Test
        void trocoEhGscbComCompraERetirada() {
            Valor troco = new Valor("80.00", 0, new Retirada(null, new DetalheRetirada("0.00", 1, "AGTEC", "12345678")),
                    null, null, null, null, null);
            ValoresDaOrdem v = regras.valoresDaOrdem(troco, new ValoresInformados(null, null, bd("20.00")));
            assertThat(v.purpose()).isEqualTo("GSCB");
            assertThat(v.total()).isEqualByComparingTo("100.00");
            assertThat(v.valorCompra()).isEqualByComparingTo("80.00");
            assertThat(v.valorRetirada()).isEqualByComparingTo("20.00");
            assertThat(v.modalidadeAgente()).isEqualTo("AGTEC");
            assertThat(v.prestadorServicoSaque()).isEqualTo("12345678");
        }

        @Test
        void saqueEhOthrEAgpssViraAgfss() {
            Valor saque = new Valor("0.00", 0, new Retirada(new DetalheRetirada("100.00", 0, "AGPSS", "12345678"), null),
                    null, null, null, null, null);
            ValoresDaOrdem v = regras.valoresDaOrdem(saque, ValoresInformados.nenhum());
            assertThat(v.purpose()).isEqualTo("OTHR");
            assertThat(v.total()).isEqualByComparingTo("100.00");
            assertThat(v.valorRetirada()).isEqualByComparingTo("100.00");
            assertThat(v.modalidadeAgente()).isEqualTo("AGFSS");
        }
    }

    @Nested
    class R18_ParametrosCobv {
        static ContaPagador conta(String codMun) {
            return new ContaPagador("c", "11222333000181", "EMPRESA", "12345678", "PSP", "0001", "1", codMun,
                    BigDecimal.TEN, BigDecimal.ZERO, "Cheque", BigDecimal.TEN);
        }

        @Test
        void mandaCodMunEDpp() {
            assertThat(regras.parametrosConsultaCobv(conta("3550308"), LocalDate.of(2026, 11, 20)))
                    .containsEntry("codMun", "3550308").containsEntry("DPP", "2026-11-20").hasSize(2);
        }

        @Test
        void semCodMunValidoMandaSoADpp() {
            assertThat(regras.parametrosConsultaCobv(conta(null), LocalDate.of(2026, 11, 20)))
                    .containsOnlyKeys("DPP");
            assertThat(regras.parametrosConsultaCobv(conta("355"), LocalDate.of(2026, 11, 20)))
                    .containsOnlyKeys("DPP");
        }
    }

    @Nested
    class R19_DataLimiteCobv {
        final CalendarioDiasUteis cal = new CalendarioDiasUteis();

        @Test
        void semValidadeOLimiteEhOVencimento() {
            // 13/11/2026 é sexta-feira
            assertThat(regras.dataLimiteCobv(LocalDate.of(2026, 11, 13), 0, cal)).isEqualTo(LocalDate.of(2026, 11, 13));
        }

        @Test
        void limiteNoFimDeSemanaVaiParaSegunda() {
            assertThat(regras.dataLimiteCobv(LocalDate.of(2026, 11, 13), 1, cal)).isEqualTo(LocalDate.of(2026, 11, 16));
            // vencimento no sábado sem validade: prorroga para segunda
            assertThat(regras.dataLimiteCobv(LocalDate.of(2026, 11, 14), 0, cal)).isEqualTo(LocalDate.of(2026, 11, 16));
        }

        @Test
        void pulaFeriado() {
            // 20/11/2026 (sexta) é feriado nacional: 19 + 1 = 20 -> sábado -> domingo -> segunda 23
            assertThat(regras.dataLimiteCobv(LocalDate.of(2026, 11, 19), 1, cal)).isEqualTo(LocalDate.of(2026, 11, 23));
        }

        @Test
        void contaDiasCorridosAPartirDoVencimentoOriginal() {
            assertThat(regras.dataLimiteCobv(LocalDate.of(2026, 11, 13), 30, cal)).isEqualTo(LocalDate.of(2026, 12, 14));
        }
    }

    @Nested
    class R20_CoerenciaValorFinal {
        @Test
        void contaFecha() {
            Valor v = new Valor("1000.00", null, null, null, "50.00", null, null, "950.00");
            assertThat(regras.validarCoerenciaValorFinal(v)).isEmpty();
            Valor atrasada = new Valor("1000.00", null, null, "10.00", null, "3.33", "20.00", "1013.33");
            assertThat(regras.validarCoerenciaValorFinal(atrasada)).isEmpty();
        }

        @Test
        void semOriginalSoConfereOFinal() {
            assertThat(regras.validarCoerenciaValorFinal(new Valor(null, null, null, null, null, null, null, "10.00"))).isEmpty();
        }

        @Test
        void contaNaoFecha() {
            Valor v = new Valor("1000.00", null, null, null, "50.00", null, null, "999.99");
            assertThat(regras.validarCoerenciaValorFinal(v)).get().extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.VALOR_INVALIDO);
        }

        @Test
        void finalInvalido() {
            assertThat(regras.validarCoerenciaValorFinal(new Valor(null, null, null, null, null, null, null, "0.00"))).isPresent();
            assertThat(regras.validarCoerenciaValorFinal(new Valor(null, null, null, null, null, null, null, "10"))).isPresent();
            assertThat(regras.validarCoerenciaValorFinal(new Valor(null, null, null, null, null, null, null, null))).isPresent();
        }
    }
}
