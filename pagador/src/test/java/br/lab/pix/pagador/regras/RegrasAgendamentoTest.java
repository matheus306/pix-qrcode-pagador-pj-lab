package br.lab.pix.pagador.regras;

import br.lab.pix.pagador.dominio.AcaoRetentativa;
import br.lab.pix.pagador.dominio.CodigoRecusa;
import br.lab.pix.pagador.dominio.DadosDict;
import br.lab.pix.pagador.dominio.DecisaoRevalidacao;
import br.lab.pix.pagador.dominio.EventoAgendamento;
import br.lab.pix.pagador.dominio.MotivoFalha;
import br.lab.pix.pagador.dominio.Notificacao;
import br.lab.pix.pagador.dominio.RespostaDict;
import br.lab.pix.pagador.dominio.TipoCobranca;
import br.lab.pix.pagador.dominio.TipoQr;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RegrasAgendamentoTest {

    final RegrasAgendamento regras = new RegrasAgendamento();
    static final LocalDate HOJE = LocalDate.of(2026, 11, 9);

    @Nested
    class R31_PedidoDeAgendamento {
        @Test
        void cobvDinamicoParaDataFutura() {
            assertThat(regras.validarPedidoDeAgendamento(TipoQr.DINAMICO, TipoCobranca.COM_VENCIMENTO,
                    LocalDate.of(2026, 11, 12), HOJE, LocalDate.of(2026, 11, 30))).isEmpty();
        }

        @Test
        void diaNaoUtilPode() {
            // 14/11/2026 é sábado
            assertThat(regras.validarPedidoDeAgendamento(TipoQr.DINAMICO, TipoCobranca.COM_VENCIMENTO,
                    LocalDate.of(2026, 11, 14), HOJE, LocalDate.of(2026, 11, 30))).isEmpty();
        }

        @Test
        void estaticoEImediataNaoSaoAgendaveisPorQr() {
            assertThat(regras.validarPedidoDeAgendamento(TipoQr.ESTATICO, null,
                    LocalDate.of(2026, 11, 12), HOJE, null)).get()
                    .extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.AGENDAMENTO_NAO_PERMITIDO);
            assertThat(regras.validarPedidoDeAgendamento(TipoQr.DINAMICO, TipoCobranca.IMEDIATA,
                    LocalDate.of(2026, 11, 12), HOJE, null)).isPresent();
        }

        @Test
        void precisaSerDataFutura() {
            assertThat(regras.validarPedidoDeAgendamento(TipoQr.DINAMICO, TipoCobranca.COM_VENCIMENTO,
                    HOJE, HOJE, LocalDate.of(2026, 11, 30))).get()
                    .extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.AGENDAMENTO_NAO_PERMITIDO);
        }

        @Test
        void depoisDoLimiteEhVencido() {
            assertThat(regras.validarPedidoDeAgendamento(TipoQr.DINAMICO, TipoCobranca.COM_VENCIMENTO,
                    LocalDate.of(2026, 12, 1), HOJE, LocalDate.of(2026, 11, 30))).get()
                    .extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.QR_VENCIDO);
        }
    }

    @Nested
    class R32_LimitePixAgendado {
        @Test
        void cabeNoLimite() {
            assertThat(regras.validarLimitePixAgendado(new BigDecimal("400"), new BigDecimal("1000"), new BigDecimal("600"))).isEmpty();
        }

        @Test
        void estouraOLimite() {
            assertThat(regras.validarLimitePixAgendado(new BigDecimal("400.01"), new BigDecimal("1000"), new BigDecimal("600"))).get()
                    .extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.LIMITE_PIX_AGENDADO);
        }
    }

    @Nested
    class R33_Cancelamento {
        final LocalDate dia10 = LocalDate.of(2026, 11, 10);

        @Test
        void ateAs2359DoDiaAnteriorEmBrasilia() {
            // 02:59 UTC do dia 10 = 23:59 do dia 9 em Brasília
            assertThat(regras.podeCancelarAgendamento(dia10, ZonedDateTime.parse("2026-11-10T02:59:00Z"))).isTrue();
            // 03:00 UTC do dia 10 = 00:00 do dia 10 em Brasília
            assertThat(regras.podeCancelarAgendamento(dia10, ZonedDateTime.parse("2026-11-10T03:00:00Z"))).isFalse();
        }

        @Test
        void diasAntesPode() {
            assertThat(regras.podeCancelarAgendamento(dia10, ZonedDateTime.parse("2026-11-08T15:00:00-03:00[America/Sao_Paulo]"))).isTrue();
        }
    }

    @Nested
    class R34_RevalidacaoNoDia {
        final DadosDict titular = new DadosDict("k", "J", "33444555000172", "FORNECEDOR A", null, "1", "B", "1", "1", "CACC", false);
        final BigDecimal cem = new BigDecimal("100.00");

        @Test
        void tudoIgualSegue() {
            assertThat(regras.revalidarNoDia("ATIVA", new RespostaDict(200, titular), "33444555000172", cem, new BigDecimal("100")))
                    .isEqualTo(DecisaoRevalidacao.SEGUIR);
        }

        @Test
        void cobrancaPagaNoBoleto() {
            assertThat(regras.revalidarNoDia("REMOVIDA_PELO_PSP_BOLETO", new RespostaDict(200, titular), "33444555000172", cem, cem))
                    .isEqualTo(DecisaoRevalidacao.NAO_EFETIVAR_COBRANCA_NAO_PAGAVEL);
        }

        @Test
        void chaveMudouDeDono() {
            assertThat(regras.revalidarNoDia("ATIVA", new RespostaDict(200, titular), "99888777000166", cem, cem))
                    .isEqualTo(DecisaoRevalidacao.NAO_EFETIVAR_CHAVE_MUDOU);
            assertThat(regras.revalidarNoDia("ATIVA", new RespostaDict(404, null), "33444555000172", cem, cem))
                    .isEqualTo(DecisaoRevalidacao.NAO_EFETIVAR_CHAVE_MUDOU);
        }

        @Test
        void valorMudou() {
            assertThat(regras.revalidarNoDia("ATIVA", new RespostaDict(200, titular), "33444555000172", new BigDecimal("120.00"), cem))
                    .isEqualTo(DecisaoRevalidacao.NAO_EFETIVAR_VALOR_DIVERGENTE);
        }

        @Test
        void dictForaEhFalhaOperacional() {
            assertThat(regras.revalidarNoDia("ATIVA", new RespostaDict(503, null), "33444555000172", cem, cem))
                    .isEqualTo(DecisaoRevalidacao.FALHA_OPERACIONAL);
        }
    }

    @Nested
    class R35_Retentativa {
        @Test
        void faltaDeSaldoTentaDeNovoNoMesmoDia() {
            assertThat(regras.decidirRetentativa(MotivoFalha.SALDO_INSUFICIENTE, 1, 2)).isEqualTo(AcaoRetentativa.RETENTAR_MESMO_DIA);
            assertThat(regras.decidirRetentativa(MotivoFalha.SALDO_INSUFICIENTE, 2, 2)).isEqualTo(AcaoRetentativa.NAO_EFETIVAR);
        }

        @Test
        void falhaOperacionalTambemTenta() {
            assertThat(regras.decidirRetentativa(MotivoFalha.FALHA_OPERACIONAL, 1, 2)).isEqualTo(AcaoRetentativa.RETENTAR_MESMO_DIA);
        }

        @Test
        void limiteNaoTenta() {
            assertThat(regras.decidirRetentativa(MotivoFalha.LIMITE_AGENDADO_INSUFICIENTE, 1, 2)).isEqualTo(AcaoRetentativa.NAO_EFETIVAR);
        }
    }

    @Nested
    class R36_Notificacoes {
        final LocalTime limite = LocalTime.of(21, 0);

        @Test
        void liquidadoComNomeEValor() {
            LocalDateTime agora = LocalDateTime.of(2026, 11, 10, 9, 15);
            Optional<Notificacao> n = regras.notificar(EventoAgendamento.LIQUIDADO, "ONG Vida Animal", new BigDecimal("40.00"), agora, limite);
            assertThat(n).isPresent();
            assertThat(n.get().texto()).contains("ONG Vida Animal").contains("40,00");
            assertThat(n.get().enviarEm()).isEqualTo(agora);
        }

        @Test
        void liquidadoNaMadrugadaEsperaAsSeis() {
            Optional<Notificacao> n = regras.notificar(EventoAgendamento.LIQUIDADO, "ONG", new BigDecimal("40.00"),
                    LocalDateTime.of(2026, 11, 10, 2, 0), limite);
            assertThat(n.get().enviarEm()).isEqualTo(LocalDateTime.of(2026, 11, 10, 6, 0));
        }

        @Test
        void faltaDeSaldoAvisaOHorarioDaNovaTentativa() {
            Optional<Notificacao> n = regras.notificar(EventoAgendamento.FALHA_SALDO_PRIMEIRA_TENTATIVA, "ONG",
                    new BigDecimal("40.00"), LocalDateTime.of(2026, 11, 10, 9, 0), limite);
            assertThat(n.get().texto()).containsIgnoringCase("saldo").contains("21:00");
        }

        @Test
        void falhaOperacionalNoMeioNaoNotifica() {
            assertThat(regras.notificar(EventoAgendamento.FALHA_OPERACIONAL_INTERMEDIARIA, "ONG",
                    new BigDecimal("40.00"), LocalDateTime.of(2026, 11, 10, 9, 0), limite)).isEmpty();
        }

        @ParameterizedTest
        @EnumSource(value = EventoAgendamento.class, names = {"FALHA_SALDO_ULTIMA_TENTATIVA", "FALHA_LIMITE_AGENDADO",
                "CHAVE_MUDOU_OU_INEXISTENTE", "COBRANCA_NAO_PAGAVEL", "FALHA_OPERACIONAL_ULTIMA"})
        void falhasDefinitivasNotificam(EventoAgendamento evento) {
            assertThat(regras.notificar(evento, "ONG", new BigDecimal("40.00"),
                    LocalDateTime.of(2026, 11, 10, 9, 0), limite)).isPresent();
        }
    }
}
