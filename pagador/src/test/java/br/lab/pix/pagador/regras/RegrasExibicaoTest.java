package br.lab.pix.pagador.regras;

import br.lab.pix.pagador.dominio.CodigoRecusa;
import br.lab.pix.pagador.dominio.ContaPagador;
import br.lab.pix.pagador.dominio.DadosDict;
import br.lab.pix.pagador.dominio.DestinatarioExibido;
import br.lab.pix.pagador.dominio.RespostaDict;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RegrasExibicaoTest {

    final RegrasExibicao regras = new RegrasExibicao();

    static DadosDict pj(String fantasia, boolean fraude) {
        return new DadosDict("pagamentos@fornecedor-a.com.br", "J", "33444555000172", "FORNECEDOR A COMERCIO LTDA",
                fantasia, "11111111", "Banco Recebedor A", "0001", "123456", "CACC", fraude);
    }

    @Nested
    class R21_ConsultaDict {
        @Test
        void chaveOk() {
            assertThat(regras.interpretarConsultaDict(new RespostaDict(200, pj("Fornecedor A", false)))).isEmpty();
        }

        @Test
        void fraudeTemMensagemObrigatoria() {
            var recusa = regras.interpretarConsultaDict(new RespostaDict(200, pj("Fornecedor A", true)));
            assertThat(recusa).get().extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.FRAUDE);
            assertThat(recusa.get().mensagem()).containsIgnoringCase("fundada suspeita de fraude");
        }

        @Test
        void chaveInexistente() {
            assertThat(regras.interpretarConsultaDict(new RespostaDict(404, null))).get()
                    .extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.CHAVE_INEXISTENTE);
        }

        @Test
        void dictForaDoAr() {
            assertThat(regras.interpretarConsultaDict(new RespostaDict(503, null))).get()
                    .extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.DICT_INDISPONIVEL);
            assertThat(regras.interpretarConsultaDict(new RespostaDict(0, null))).get()
                    .extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.DICT_INDISPONIVEL);
        }
    }

    @Nested
    class R22_Destinatario {
        @Test
        void pjComNomeFantasiaECnpjSemMascara() {
            DestinatarioExibido d = regras.montarDestinatario(pj("Fornecedor A", false));
            assertThat(d.nome()).isEqualTo("Fornecedor A");
            assertThat(d.documento()).isEqualTo("33444555000172");
            assertThat(d.nomePsp()).isEqualTo("Banco Recebedor A");
        }

        @Test
        void pjSemNomeFantasiaUsaRazaoSocial() {
            assertThat(regras.montarDestinatario(pj(null, false)).nome()).isEqualTo("FORNECEDOR A COMERCIO LTDA");
            assertThat(regras.montarDestinatario(pj("  ", false)).nome()).isEqualTo("FORNECEDOR A COMERCIO LTDA");
        }

        @Test
        void pfComCpfMascarado() {
            DadosDict pf = new DadosDict("+5561999990001", "F", "12345678901", "Maria Souza", null,
                    "22222222", "Banco B", "0002", "999", "CACC", false);
            DestinatarioExibido d = regras.montarDestinatario(pf);
            assertThat(d.nome()).isEqualTo("Maria Souza");
            assertThat(d.documento()).isEqualTo("***.456.789-**");
        }
    }

    @Nested
    class R23_Sanitizar {
        @Test
        void removeScriptComConteudo() {
            String limpo = regras.sanitizarTexto("<script>alert('x')</script>Pedido 123");
            assertThat(limpo).isEqualTo("Pedido 123");
        }

        @Test
        void removeTagsEPreservaTexto() {
            assertThat(regras.sanitizarTexto("<b>Total</b> da <i>nota</i>")).isEqualTo("Total da nota");
            assertThat(regras.sanitizarTexto("<STYLE>body{}</STYLE>  NF 99 ")).isEqualTo("NF 99");
        }

        @Test
        void textoSimplesENulo() {
            assertThat(regras.sanitizarTexto("Número da sorte: 123456")).isEqualTo("Número da sorte: 123456");
            assertThat(regras.sanitizarTexto(null)).isNull();
        }
    }

    @Nested
    class R24_RespostaAoPagador {
        @Test
        void respostaVaziaSempreOk() {
            assertThat(regras.validarRespostaAoPagador(null, null)).isEmpty();
            assertThat(regras.validarRespostaAoPagador("Informe a NF", "")).isEmpty();
        }

        @Test
        void soRespondeSeORecebedorPediu() {
            assertThat(regras.validarRespostaAoPagador(null, "minha descrição")).get()
                    .extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.CAMPO_NAO_PERMITIDO);
        }

        @Test
        void ate140Caracteres() {
            assertThat(regras.validarRespostaAoPagador("Informe a NF", "x".repeat(140))).isEmpty();
            assertThat(regras.validarRespostaAoPagador("Informe a NF", "x".repeat(141))).isPresent();
        }
    }

    @Nested
    class R25_AvisoDeCredito {
        final ContaPagador conta = new ContaPagador("c", "11222333000181", "EMPRESA", "12345678", "PSP", "0001", "1",
                "3550308", new BigDecimal("200.00"), new BigDecimal("1000.00"), "Cheque Empresarial", BigDecimal.TEN);

        @Test
        void saldoCobreSemAviso() {
            assertThat(regras.avisoUsoDeCredito(conta, new BigDecimal("200.00"))).isEmpty();
        }

        @Test
        void usaCreditoMostraValorELinha() {
            var aviso = regras.avisoUsoDeCredito(conta, new BigDecimal("250.00"));
            assertThat(aviso).isPresent();
            assertThat(aviso.get()).contains("50,00").contains("Cheque Empresarial");
        }

        @Test
        void nemComCreditoCobre() {
            assertThat(regras.avisoUsoDeCredito(conta, new BigDecimal("1200.01"))).isEmpty();
        }
    }
}
