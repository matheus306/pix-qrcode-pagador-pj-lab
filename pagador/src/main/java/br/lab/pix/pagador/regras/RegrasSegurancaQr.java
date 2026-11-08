package br.lab.pix.pagador.regras;

import br.lab.pix.pagador.dominio.CabecalhoJws;
import br.lab.pix.pagador.dominio.CertificadoSite;
import br.lab.pix.pagador.dominio.ChaveJwk;
import br.lab.pix.pagador.dominio.Recusa;

import java.util.Optional;

/**
 * Bloco 2: segurança do QR dinâmico.
 * Fonte principal: Manual de Segurança do SFN v6.00, Volume II, seções 4 e 5.
 */
public class RegrasSegurancaQr {

    /**
     * R07: site de homologação em produção?
     * <p>
     * Seção 4.3: se o nome do servidor (o primeiro rótulo do host, ex.: "qrcode-h" em
     * "qrcode-h.banco.com.br") terminar com "-h", um aplicativo de PRODUÇÃO não pode seguir.
     * Recuse com QR_HOMOLOGACAO. Em homologação, o host "-h" é o esperado.
     */
    public Optional<Recusa> validarAmbienteDoHost(String host, boolean ambienteProducao) {
        throw new RegraNaoImplementada("R07", "RegrasSegurancaQr.validarAmbienteDoHost");
    }

    /**
     * R08: o certificado do site do PSP recebedor é aceitável?
     * <p>
     * Seções 4.1, 4.3, 5.1 e 5.2. Recuse com CERTIFICADO_INVALIDO se:
     * <ul>
     *   <li>o certificado não está na lista CERTQRC do BCB ({@code cert == null});</li>
     *   <li>a conexão não foi TLS 1.2 ou superior ("TLSv1.2" ou "TLSv1.3");</li>
     *   <li>não é EV;</li>
     *   <li>tem algum nome com wildcard ("*.");</li>
     *   <li>o host não aparece no CN/SAN (compare sem diferenciar maiúsculas);</li>
     *   <li>está revogado (verificar revogação é obrigação do PAGADOR).</li>
     * </ul>
     */
    public Optional<Recusa> validarCertificadoDoSite(String host, String versaoTls, CertificadoSite cert) {
        throw new RegraNaoImplementada("R08", "RegrasSegurancaQr.validarCertificadoDoSite");
    }

    /**
     * R09: o algoritmo do JWS é permitido?
     * <p>
     * Seção 4.2: proibidos "none" e qualquer "HS*" (HMAC). Permitidos: RS256 ou superior
     * (RS256, RS384, RS512, PS256, PS384, PS512) e ES256 ou superior (ES256, ES384, ES512).
     * Qualquer outro valor, inclusive nulo: ASSINATURA_INVALIDA.
     */
    public Optional<Recusa> validarAlgoritmo(String alg) {
        throw new RegraNaoImplementada("R09", "RegrasSegurancaQr.validarAlgoritmo");
    }

    /**
     * R10: o cabeçalho JWS e a chave do JWK Set batem, e a assinatura confere?
     * <p>
     * Seções 4.2 e 4.3. Recuse com ASSINATURA_INVALIDA se:
     * <ul>
     *   <li>falta "kid", ou faltam ao mesmo tempo "x5t" e "x5t#S256", ou falta "jku";</li>
     *   <li>o host do "jku" não é o mesmo host da URL do QR (o JWK Set fica no mesmo site);</li>
     *   <li>não há chave no JWK Set ({@code jwk == null}) ou o "kid" é diferente;</li>
     *   <li>o thumbprint do cabeçalho não bate com o do JWK (compare o x5t#S256 se o
     *       cabeçalho trouxer; senão o x5t);</li>
     *   <li>"key_ops" do JWK não contém "verify";</li>
     *   <li>a assinatura não confere. Só com assinatura válida o payload pode ser processado.</li>
     * </ul>
     */
    public Optional<Recusa> validarCabecalhoJws(CabecalhoJws cabecalho, String hostDaUrl, ChaveJwk jwk,
                                               boolean assinaturaConfere) {
        throw new RegraNaoImplementada("R10", "RegrasSegurancaQr.validarCabecalhoJws");
    }
}
