package br.lab.pix.pagador.fluxo;

import br.lab.pix.pagador.dominio.Cobranca;
import br.lab.pix.pagador.dominio.CodigoRecusa;
import br.lab.pix.pagador.infra.ArquivoNaoRepudio;
import br.lab.pix.pagador.infra.ClienteArq;
import br.lab.pix.pagador.infra.ClienteRecebedor;
import br.lab.pix.pagador.infra.Http;
import br.lab.pix.pagador.infra.VerificadorJws;
import br.lab.pix.pagador.regras.RegrasCobranca;
import br.lab.pix.pagador.regras.RegrasSegurancaQr;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

import static br.lab.pix.pagador.fluxo.FalhaDeFluxo.exigir;

/**
 * Busca e valida o payload de um QR dinâmico (máquina M1, ramo "dinâmico").
 * Usado na leitura e de novo no dia de um agendamento.
 */
@Component
public class LeitorDePayload {

    private final ClienteRecebedor recebedor;
    private final ClienteArq arq;
    private final VerificadorJws verificador;
    private final ArquivoNaoRepudio naoRepudio;
    private final RegrasSegurancaQr seguranca;
    private final RegrasCobranca cobranca;
    private final ObjectMapper json;
    private final boolean producao;

    public LeitorDePayload(ClienteRecebedor recebedor, ClienteArq arq, VerificadorJws verificador,
                           ArquivoNaoRepudio naoRepudio, RegrasSegurancaQr seguranca, RegrasCobranca cobranca,
                           ObjectMapper json, @Value("${pix.ambiente-producao}") boolean producao) {
        this.recebedor = recebedor;
        this.arq = arq;
        this.verificador = verificador;
        this.naoRepudio = naoRepudio;
        this.seguranca = seguranca;
        this.cobranca = cobranca;
        this.json = json;
        this.producao = producao;
    }

    public Cobranca buscar(String idPagamento, String evidencia, String url, Map<String, String> parametros) {
        String host = ClienteRecebedor.host(url);
        exigir(seguranca.validarAmbienteDoHost(host, producao));                              // R07

        Http.Resposta resposta = recebedor.buscar(url, parametros);
        exigir(cobranca.interpretarHttpDoPayload(resposta.status()));                          // R11

        String versaoTls = resposta.cabecalho("X-Simulacao-Tls");
        var certificado = arq.certificado(resposta.cabecalho("X-Simulacao-Certificado"));
        exigir(seguranca.validarCertificadoDoSite(host, versaoTls, certificado));              // R08

        VerificadorJws.Analise analise = verificador.analisar(resposta.corpo());
        exigir(seguranca.validarAlgoritmo(analise.cabecalho().alg()));                         // R09
        exigir(seguranca.validarCabecalhoJws(analise.cabecalho(), host, analise.chave(),
                analise.assinaturaConfere()));                                                  // R10

        naoRepudio.guardar(idPagamento, evidencia, resposta.corpo(), analise.jwkSetJson());
        try {
            return json.readValue(analise.payloadJson(), Cobranca.class);
        } catch (Exception e) {
            throw new FalhaDeFluxo(CodigoRecusa.QR_INVALIDO, "Transação não concluída. QR Code inválido.");
        }
    }
}
