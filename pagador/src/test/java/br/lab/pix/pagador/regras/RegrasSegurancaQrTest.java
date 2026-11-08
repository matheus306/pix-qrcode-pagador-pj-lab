package br.lab.pix.pagador.regras;

import br.lab.pix.pagador.dominio.CabecalhoJws;
import br.lab.pix.pagador.dominio.CertificadoSite;
import br.lab.pix.pagador.dominio.ChaveJwk;
import br.lab.pix.pagador.dominio.CodigoRecusa;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RegrasSegurancaQrTest {

    final RegrasSegurancaQr regras = new RegrasSegurancaQr();

    @Nested
    class R07_AmbienteDoHost {
        @Test
        void producaoRecusaHostDeHomologacao() {
            assertThat(regras.validarAmbienteDoHost("qrcode-h.bancoxyz.com.br", true)).get()
                    .extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.QR_HOMOLOGACAO);
        }

        @Test
        void producaoAceitaHostNormal() {
            assertThat(regras.validarAmbienteDoHost("qrcode.bancoxyz.com.br", true)).isEmpty();
            // "-h" no meio do domínio não conta: o que vale é o nome do servidor
            assertThat(regras.validarAmbienteDoHost("qrcode.banco-h.com.br", true)).isEmpty();
        }

        @Test
        void homologacaoAceitaHostDeHomologacao() {
            assertThat(regras.validarAmbienteDoHost("qrcode-h.bancoxyz.com.br", false)).isEmpty();
        }
    }

    @Nested
    class R08_CertificadoDoSite {
        static final String HOST = "pix.recebedor-a.local";
        final CertificadoSite bom = new CertificadoSite(List.of("pix.recebedor-a.local", "qr.recebedor-a.local"), true, false);

        @Test
        void aceitaCertificadoCorreto() {
            assertThat(regras.validarCertificadoDoSite(HOST, "TLSv1.2", bom)).isEmpty();
            assertThat(regras.validarCertificadoDoSite("PIX.Recebedor-A.local", "TLSv1.3", bom)).isEmpty();
        }

        @Test
        void recusaForaDaListaDoBcb() {
            assertThat(regras.validarCertificadoDoSite(HOST, "TLSv1.2", null)).get()
                    .extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.CERTIFICADO_INVALIDO);
        }

        @Test
        void recusaTlsAntigo() {
            assertThat(regras.validarCertificadoDoSite(HOST, "TLSv1.1", bom)).isPresent();
            assertThat(regras.validarCertificadoDoSite(HOST, null, bom)).isPresent();
        }

        @Test
        void recusaNaoEv() {
            assertThat(regras.validarCertificadoDoSite(HOST, "TLSv1.2",
                    new CertificadoSite(List.of(HOST), false, false))).isPresent();
        }

        @Test
        void recusaWildcard() {
            assertThat(regras.validarCertificadoDoSite(HOST, "TLSv1.2",
                    new CertificadoSite(List.of(HOST, "*.recebedor-a.local"), true, false))).isPresent();
        }

        @Test
        void recusaHostForaDoCertificado() {
            assertThat(regras.validarCertificadoDoSite("pix.outro.local", "TLSv1.2", bom)).isPresent();
        }

        @Test
        void recusaRevogado() {
            assertThat(regras.validarCertificadoDoSite(HOST, "TLSv1.2",
                    new CertificadoSite(List.of(HOST), true, true))).isPresent();
        }
    }

    @Nested
    class R09_Algoritmo {
        @ParameterizedTest
        @ValueSource(strings = {"RS256", "RS384", "RS512", "PS256", "PS384", "PS512", "ES256", "ES384", "ES512"})
        void permitidos(String alg) {
            assertThat(regras.validarAlgoritmo(alg)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"none", "HS256", "HS512", "RS1", "EdDSA", ""})
        void proibidos(String alg) {
            assertThat(regras.validarAlgoritmo(alg)).get()
                    .extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.ASSINATURA_INVALIDA);
        }

        @Test
        void nuloEhProibido() {
            assertThat(regras.validarAlgoritmo(null)).isPresent();
        }
    }

    @Nested
    class R10_CabecalhoJws {
        static final String HOST = "pix.recebedor-a.local";
        final CabecalhoJws cab = new CabecalhoJws("PS256", "chave-1", "http://pix.recebedor-a.local/jwks", null, "abc123");
        final ChaveJwk jwk = new ChaveJwk("chave-1", null, "abc123", List.of("verify"));

        @Test
        void tudoCerto() {
            assertThat(regras.validarCabecalhoJws(cab, HOST, jwk, true)).isEmpty();
        }

        @Test
        void aceitaX5tSha1QuandoNaoHaSha256() {
            CabecalhoJws c = new CabecalhoJws("PS256", "chave-1", "http://pix.recebedor-a.local/jwks", "sha1thumb", null);
            ChaveJwk k = new ChaveJwk("chave-1", "sha1thumb", null, List.of("verify"));
            assertThat(regras.validarCabecalhoJws(c, HOST, k, true)).isEmpty();
        }

        @Test
        void assinaturaQueNaoConfereNuncaPassa() {
            assertThat(regras.validarCabecalhoJws(cab, HOST, jwk, false)).get()
                    .extracting(r -> r.codigo()).isEqualTo(CodigoRecusa.ASSINATURA_INVALIDA);
        }

        @Test
        void recusaCamposFaltando() {
            assertThat(regras.validarCabecalhoJws(new CabecalhoJws("PS256", null, cab.jku(), null, "abc123"), HOST, jwk, true)).isPresent();
            assertThat(regras.validarCabecalhoJws(new CabecalhoJws("PS256", "chave-1", cab.jku(), null, null), HOST, jwk, true)).isPresent();
            assertThat(regras.validarCabecalhoJws(new CabecalhoJws("PS256", "chave-1", null, null, "abc123"), HOST, jwk, true)).isPresent();
        }

        @Test
        void recusaJkuEmOutroSite() {
            CabecalhoJws c = new CabecalhoJws("PS256", "chave-1", "http://pix.atacante.local/jwks", null, "abc123");
            assertThat(regras.validarCabecalhoJws(c, HOST, jwk, true)).isPresent();
        }

        @Test
        void recusaChaveQueNaoBate() {
            assertThat(regras.validarCabecalhoJws(cab, HOST, null, true)).isPresent();
            assertThat(regras.validarCabecalhoJws(cab, HOST, new ChaveJwk("outra", null, "abc123", List.of("verify")), true)).isPresent();
            assertThat(regras.validarCabecalhoJws(cab, HOST, new ChaveJwk("chave-1", null, "zzz", List.of("verify")), true)).isPresent();
            assertThat(regras.validarCabecalhoJws(cab, HOST, new ChaveJwk("chave-1", null, "abc123", List.of("sign")), true)).isPresent();
        }
    }
}
