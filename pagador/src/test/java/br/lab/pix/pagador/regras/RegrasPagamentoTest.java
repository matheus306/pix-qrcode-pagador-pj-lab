package br.lab.pix.pagador.regras;

import br.lab.pix.pagador.dominio.AcaoPosTimeout;
import br.lab.pix.pagador.dominio.ContaPagador;
import br.lab.pix.pagador.dominio.DadosDict;
import br.lab.pix.pagador.dominio.DadosOrdem;
import br.lab.pix.pagador.dominio.EstadoPagamento;
import br.lab.pix.pagador.dominio.FormaIniciacao;
import br.lab.pix.pagador.dominio.Pacs008;
import br.lab.pix.pagador.dominio.StatusConsultaSpi;
import br.lab.pix.pagador.dominio.TipoQr;
import br.lab.pix.pagador.dominio.ValoresDaOrdem;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.Instant;

import static br.lab.pix.pagador.dominio.EstadoPagamento.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegrasPagamentoTest {

    final RegrasPagamento regras = new RegrasPagamento();

    @Nested
    class R26_MaquinaDeEstados {
        @ParameterizedTest
        @CsvSource({
                "PRONTO_PARA_CONFIRMACAO, AUTORIZADO",
                "PRONTO_PARA_CONFIRMACAO, RECUSADO_INTERNO",
                "PRONTO_PARA_CONFIRMACAO, CANCELADO_PELO_USUARIO",
                "PRONTO_PARA_CONFIRMACAO, AGENDADO",
                "AUTORIZADO, ENVIADO_SPI",
                "ENVIADO_SPI, LIQUIDADO",
                "ENVIADO_SPI, REJEITADO",
                "ENVIADO_SPI, EM_VERIFICACAO",
                "EM_VERIFICACAO, LIQUIDADO",
                "EM_VERIFICACAO, REJEITADO",
                "AGENDADO, CANCELADO_PELO_USUARIO",
                "AGENDADO, REVALIDANDO",
                "REVALIDANDO, AUTORIZADO",
                "REVALIDANDO, AGUARDANDO_RETENTATIVA",
                "REVALIDANDO, NAO_EFETIVADO",
                "AGUARDANDO_RETENTATIVA, REVALIDANDO",
                "AGUARDANDO_RETENTATIVA, NAO_EFETIVADO"
        })
        void permitidas(EstadoPagamento de, EstadoPagamento para) {
            assertThat(regras.transicaoPermitida(de, para)).isTrue();
        }

        @ParameterizedTest
        @CsvSource({
                "ENVIADO_SPI, ENVIADO_SPI",
                "EM_VERIFICACAO, ENVIADO_SPI",
                "AGENDADO, LIQUIDADO",
                "AGENDADO, AUTORIZADO",
                "PRONTO_PARA_CONFIRMACAO, ENVIADO_SPI",
                "AUTORIZADO, LIQUIDADO"
        })
        void proibidas(EstadoPagamento de, EstadoPagamento para) {
            assertThat(regras.transicaoPermitida(de, para)).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = EstadoPagamento.class,
                names = {"LIQUIDADO", "REJEITADO", "CANCELADO_PELO_USUARIO", "RECUSADO_INTERNO", "NAO_EFETIVADO"})
        void estadosFinaisNaoSaem(EstadoPagamento finalState) {
            for (EstadoPagamento para : EstadoPagamento.values()) {
                assertThat(regras.transicaoPermitida(finalState, para)).as(finalState + " -> " + para).isFalse();
            }
        }
    }

    @Nested
    class R27_EndToEndId {
        @Test
        void formatoDe32Caracteres() {
            String e2e = regras.gerarEndToEndId("12345678", Instant.parse("2026-11-08T18:55:30Z"), "abc12345678");
            assertThat(e2e).isEqualTo("E12345678202611081855abc12345678").hasSize(32);
        }

        @Test
        void dataEmUtcMesmoQueSejaOutroDiaEmBrasilia() {
            // 01:30 UTC de 09/11 = 22:30 de 08/11 em Brasília. O EndToEndId usa UTC.
            assertThat(regras.gerarEndToEndId("12345678", Instant.parse("2026-11-09T01:30:00Z"), "00000000001"))
                    .startsWith("E12345678202611090130");
        }

        @Test
        void validaEntradas() {
            assertThatThrownBy(() -> regras.gerarEndToEndId("1234567", Instant.now(), "abc12345678"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> regras.gerarEndToEndId("12345678", Instant.now(), "abc-1234567"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> regras.gerarEndToEndId("12345678", Instant.now(), "abc"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class R28_Pacs008 {
        final ContaPagador pagador = new ContaPagador("c", "11222333000181", "EMPRESA PAGADORA LTDA", "12345678", "PSP Lab",
                "0001", "998877", "3550308", BigDecimal.TEN, BigDecimal.ZERO, "Cheque", BigDecimal.TEN);
        final DadosDict recebedor = new DadosDict("pagamentos@fornecedor-a.com.br", "J", "33444555000172",
                "FORNECEDOR A COMERCIO LTDA", "Fornecedor A", "11111111", "Banco Recebedor A", "0001", "123456", "CACC", false);
        final ValoresDaOrdem ipay = new ValoresDaOrdem(new BigDecimal("250.00"), "IPAY", null, null, null, null);

        @Test
        void estaticoSemTxid() {
            Pacs008 p = regras.montarPacs008(new DadosOrdem(TipoQr.ESTATICO, "***", null, recebedor.chave(), ipay,
                    null, "E1", pagador, recebedor));
            assertThat(p.formaIniciacao()).isEqualTo(FormaIniciacao.QRES);
            assertThat(p.txId()).isNull();
            assertThat(p.chave()).isEqualTo("pagamentos@fornecedor-a.com.br");
            assertThat(p.valor()).isEqualByComparingTo("250.00");
            assertThat(p.purpose()).isEqualTo("IPAY");
        }

        @Test
        void estaticoComTxid() {
            Pacs008 p = regras.montarPacs008(new DadosOrdem(TipoQr.ESTATICO, "PEDIDO2026X1", null, recebedor.chave(), ipay,
                    null, "E1", pagador, recebedor));
            assertThat(p.txId()).isEqualTo("PEDIDO2026X1");
        }

        @Test
        void dinamicoUsaTxidDoPayloadERespostaDoPagador() {
            Pacs008 p = regras.montarPacs008(new DadosOrdem(TipoQr.DINAMICO, "TXIDDAIMAGEM", "abcdefghijklmnopqrstuvwxyz0",
                    recebedor.chave(), ipay, "NF 12345", "E1", pagador, recebedor));
            assertThat(p.formaIniciacao()).isEqualTo(FormaIniciacao.QRDN);
            assertThat(p.txId()).isEqualTo("abcdefghijklmnopqrstuvwxyz0");
            assertThat(p.remittanceInformation()).isEqualTo("NF 12345");
        }

        @Test
        void respostaEmBrancoNaoVai() {
            Pacs008 p = regras.montarPacs008(new DadosOrdem(TipoQr.DINAMICO, null, "abcdefghijklmnopqrstuvwxyz0",
                    recebedor.chave(), ipay, "   ", "E1", pagador, recebedor));
            assertThat(p.remittanceInformation()).isNull();
        }

        @Test
        void levaContasEValoresDeTroco() {
            ValoresDaOrdem troco = new ValoresDaOrdem(new BigDecimal("100.00"), "GSCB", new BigDecimal("80.00"),
                    new BigDecimal("20.00"), "AGTEC", "12345678");
            Pacs008 p = regras.montarPacs008(new DadosOrdem(TipoQr.DINAMICO, null, "abcdefghijklmnopqrstuvwxyz0",
                    recebedor.chave(), troco, null, "E123", pagador, recebedor));
            assertThat(p.endToEndId()).isEqualTo("E123");
            assertThat(p.purpose()).isEqualTo("GSCB");
            assertThat(p.valorCompra()).isEqualByComparingTo("80.00");
            assertThat(p.valorRetirada()).isEqualByComparingTo("20.00");
            assertThat(p.modalidadeAgente()).isEqualTo("AGTEC");
            assertThat(p.pagadorCnpj()).isEqualTo("11222333000181");
            assertThat(p.pagadorIspb()).isEqualTo("12345678");
            assertThat(p.pagadorConta()).isEqualTo("998877");
            assertThat(p.recebedorCpfCnpj()).isEqualTo("33444555000172");
            assertThat(p.recebedorIspb()).isEqualTo("11111111");
            assertThat(p.recebedorAgencia()).isEqualTo("0001");
            assertThat(p.recebedorConta()).isEqualTo("123456");
            assertThat(p.recebedorTipoConta()).isEqualTo("CACC");
        }
    }

    @Nested
    class R29_Timeout {
        @Test
        void respostaConhecida() {
            assertThat(regras.decidirAposTimeout(StatusConsultaSpi.LIQUIDADO, 1, 3)).isEqualTo(AcaoPosTimeout.MARCAR_LIQUIDADO);
            assertThat(regras.decidirAposTimeout(StatusConsultaSpi.REJEITADO, 1, 3)).isEqualTo(AcaoPosTimeout.MARCAR_REJEITADO);
        }

        @Test
        void emProcessamentoSempreConsultaDeNovo() {
            assertThat(regras.decidirAposTimeout(StatusConsultaSpi.EM_PROCESSAMENTO, 10, 3)).isEqualTo(AcaoPosTimeout.CONSULTAR_NOVAMENTE);
        }

        @Test
        void naoEncontradoEstornaSoNoFim() {
            assertThat(regras.decidirAposTimeout(StatusConsultaSpi.NAO_ENCONTRADO, 1, 3)).isEqualTo(AcaoPosTimeout.CONSULTAR_NOVAMENTE);
            assertThat(regras.decidirAposTimeout(StatusConsultaSpi.NAO_ENCONTRADO, 3, 3)).isEqualTo(AcaoPosTimeout.ESTORNAR_SEM_LIQUIDACAO);
        }

        @Test
        void nuncaReenvia() {
            for (StatusConsultaSpi s : StatusConsultaSpi.values()) {
                for (int i = 0; i <= 5; i++) {
                    assertThat(regras.decidirAposTimeout(s, i, 3)).isNotEqualTo(AcaoPosTimeout.REENVIAR_ORDEM);
                }
            }
        }
    }

    @Nested
    class R30_MensagemDeRejeicao {
        @Test
        void mensagensEspecificas() {
            assertThat(regras.mensagemDeRejeicao("SALDO")).containsIgnoringCase("saldo");
            assertThat(regras.mensagemDeRejeicao("AC03")).containsIgnoringCase("inexistente");
            assertThat(regras.mensagemDeRejeicao("AC06")).containsIgnoringCase("indisponível");
            assertThat(regras.mensagemDeRejeicao("AB03")).containsIgnoringCase("tempo");
            assertThat(regras.mensagemDeRejeicao("XPTO")).containsIgnoringCase("processamento");
            assertThat(regras.mensagemDeRejeicao(null)).containsIgnoringCase("processamento");
        }
    }
}
