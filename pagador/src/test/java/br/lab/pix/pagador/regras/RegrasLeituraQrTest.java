package br.lab.pix.pagador.regras;

import br.lab.pix.pagador.dominio.CodigoRecusa;
import br.lab.pix.pagador.dominio.TipoCobranca;
import br.lab.pix.pagador.dominio.TipoQr;
import br.lab.pix.pagador.emv.EmvDados;
import br.lab.pix.pagador.emv.EmvParser;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegrasLeituraQrTest {

    // Exemplos do próprio Manual de Padrões para Iniciação do Pix (seções 2.6.3, 2.7.2 e 2.8.5)
    static final String ESTATICO_MANUAL =
            "00020126580014br.gov.bcb.pix0136123e4567-e12b-12d1-a456-426655440000"
                    + "5204000053039865802BR5913Fulano de Tal6008BRASILIA62070503***63041D3D";
    static final String DINAMICO_MANUAL =
            "00020101021226700014br.gov.bcb.pix2548pix.example.com/8b3da2f39a4140d1a91abd93113bd441"
                    + "5204000053039865802BR5913Fulano de Tal6008BRASILIA62070503***630464E4";
    static final String COMPOSTO_SO_RECORRENCIA_MANUAL =
            "00020126180014br.gov.bcb.pix5204000053039865802BR5913Fulano de Tal6008BRASILIA62070503***"
                    + "80740014br.gov.bcb.pix2552pix.example.com/rec/2353c790eefb11eaadc10242ac1200026304F2DA";
    static final String COMPOSTO_ESTATICO_MANUAL =
            "00020126580014br.gov.bcb.pix0136123e4567-e12b-12d1-a456-4266554400005204000053039865406100.50"
                    + "5802BR5913Fulano de Tal6008BRASILIA62070503***"
                    + "80740014br.gov.bcb.pix2552pix.example.com/rec/2353c790eefb11eaadc10242ac12000263042875";

    final RegrasLeituraQr regras = new RegrasLeituraQr();

    @Nested
    class R01_Crc {
        @Test
        void aceitaOsExemplosDoManual() {
            assertThat(regras.crcValido(ESTATICO_MANUAL)).isTrue();
            assertThat(regras.crcValido(DINAMICO_MANUAL)).isTrue();
            assertThat(regras.crcValido(COMPOSTO_SO_RECORRENCIA_MANUAL)).isTrue();
            assertThat(regras.crcValido(COMPOSTO_ESTATICO_MANUAL)).isTrue();
        }

        @Test
        void aceitaCrcEmMinusculas() {
            assertThat(regras.crcValido(DINAMICO_MANUAL.replace("630464E4", "630464e4"))).isTrue();
        }

        @Test
        void recusaQrAlterado() {
            assertThat(regras.crcValido(ESTATICO_MANUAL.replace("Fulano", "Golpista"))).isFalse();
            assertThat(regras.crcValido(ESTATICO_MANUAL.replace("63041D3D", "63041D3E"))).isFalse();
        }

        @Test
        void recusaTextoSemCrc() {
            assertThat(regras.crcValido(null)).isFalse();
            assertThat(regras.crcValido("000201")).isFalse();
            assertThat(regras.crcValido(ESTATICO_MANUAL.substring(0, ESTATICO_MANUAL.length() - 8))).isFalse();
        }
    }

    @Nested
    class R02_TemplatePix {
        @Test
        void achaOTemplateNo26() {
            assertThat(regras.idTemplatePix(EmvParser.parse(ESTATICO_MANUAL))).contains("26");
        }

        @Test
        void achaOTemplateEmOutroIdEIgnoraMaiusculas() {
            // ID 27 com outro arranjo antes e GUI em maiúsculas
            EmvDados emv = EmvParser.parse("000201" + "2616" + "0012br.com.outro" + "2726" + "0014BR.GOV.BCB.PIX" + "0104abcd");
            assertThat(regras.idTemplatePix(emv)).contains("27");
        }

        @Test
        void vazioSemPix() {
            assertThat(regras.idTemplatePix(EmvParser.parse("000201" + "2616" + "0012br.com.outro"))).isEmpty();
        }
    }

    @Nested
    class R03_Classificar {
        @Test
        void estatico() {
            assertThat(regras.classificar(EmvParser.parse(ESTATICO_MANUAL), "26")).isEqualTo(TipoQr.ESTATICO);
        }

        @Test
        void dinamico() {
            assertThat(regras.classificar(EmvParser.parse(DINAMICO_MANUAL), "26")).isEqualTo(TipoQr.DINAMICO);
        }

        @Test
        void compostoComOuSemPagamento() {
            assertThat(regras.classificar(EmvParser.parse(COMPOSTO_SO_RECORRENCIA_MANUAL), "26")).isEqualTo(TipoQr.COMPOSTO);
            assertThat(regras.classificar(EmvParser.parse(COMPOSTO_ESTATICO_MANUAL), "26")).isEqualTo(TipoQr.COMPOSTO);
        }

        @Test
        void invalidoSemChaveNemUrl() {
            EmvDados emv = EmvParser.parse("000201" + "2618" + "0014br.gov.bcb.pix");
            assertThat(regras.classificar(emv, "26")).isEqualTo(TipoQr.INVALIDO);
        }
    }

    @Nested
    class R04_TxidEstatico {
        @Test
        void semTxidEhValido() {
            assertThat(regras.validarTxidEstatico(null)).isEmpty();
            assertThat(regras.validarTxidEstatico("***")).isEmpty();
        }

        @Test
        void alfanumericoAte25() {
            assertThat(regras.validarTxidEstatico("PEDIDO2026X1")).isEmpty();
            assertThat(regras.validarTxidEstatico("a".repeat(25))).isEmpty();
        }

        @Test
        void recusaGrandeOuComSimbolos() {
            assertThat(regras.validarTxidEstatico("a".repeat(26))).get()
                    .extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.QR_INVALIDO);
            assertThat(regras.validarTxidEstatico("PEDIDO-123")).isPresent();
            assertThat(regras.validarTxidEstatico("")).isPresent();
        }
    }

    @Nested
    class R05_UrlLocation {
        @Test
        void aceitaUrlDoManual() {
            assertThat(regras.validarUrlLocation("pix.example.com/8b3da2f39a4140d1a91abd93113bd441")).isEmpty();
            assertThat(regras.validarUrlLocation("pix.example.com/pix/123/cobv/8b3da2f39a4140d1")).isEmpty();
        }

        @Test
        void recusaComProtocolo() {
            assertThat(regras.validarUrlLocation("https://pix.example.com/8b3da2f3")).get()
                    .extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.QR_INVALIDO);
        }

        @Test
        void recusaMaisDe77Caracteres() {
            String url = "pix.example.com/" + "a".repeat(62); // 16 + 62 = 78
            assertThat(regras.validarUrlLocation(url)).isPresent();
            assertThat(regras.validarUrlLocation(url.substring(1))).isEmpty();
        }

        @Test
        void recusaCaracteresInvalidosOuSemCaminho() {
            assertThat(regras.validarUrlLocation("pix.example.com/abc def")).isPresent();
            assertThat(regras.validarUrlLocation("pix.example.com/<script>")).isPresent();
            assertThat(regras.validarUrlLocation("pix.example.com/abc?x=1")).isPresent();
            assertThat(regras.validarUrlLocation("pix.example.com")).isPresent();
            assertThat(regras.validarUrlLocation(null)).isPresent();
        }
    }

    @Nested
    class R06_TipoCobranca {
        @Test
        void identificaPeloFragmento() {
            assertThat(regras.tipoCobranca("pix.example.com/8b3da2f39a4140d1")).isEqualTo(TipoCobranca.IMEDIATA);
            assertThat(regras.tipoCobranca("pix.example.com/cob/8b3da2f39a4140d1")).isEqualTo(TipoCobranca.IMEDIATA);
            assertThat(regras.tipoCobranca("pix.example.com/pix/123/cobv/8b3da2")).isEqualTo(TipoCobranca.COM_VENCIMENTO);
            assertThat(regras.tipoCobranca("pix.example.com/rec/2353c790eefb11ea")).isEqualTo(TipoCobranca.RECORRENCIA);
        }

        @Test
        void soOlhaOSegmentoAntesDoToken() {
            assertThat(regras.tipoCobranca("pix.example.com/cobv/x/8b3da2")).isEqualTo(TipoCobranca.IMEDIATA);
        }
    }
}
