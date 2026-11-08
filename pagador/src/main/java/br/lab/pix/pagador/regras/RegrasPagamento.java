package br.lab.pix.pagador.regras;

import br.lab.pix.pagador.dominio.AcaoPosTimeout;
import br.lab.pix.pagador.dominio.DadosOrdem;
import br.lab.pix.pagador.dominio.EstadoPagamento;
import br.lab.pix.pagador.dominio.Pacs008;
import br.lab.pix.pagador.dominio.StatusConsultaSpi;

import java.time.Instant;

/**
 * Bloco 5: confirmar, enviar ao SPI e tratar o retorno.
 * Fontes: Manual de Padrões (Anexo V), UX caps. 06/07 e a máquina de estados de docs/MAQUINA_DE_ESTADOS.md.
 */
public class RegrasPagamento {

    /**
     * R26: esta transição de estado é permitida?
     * <p>
     * Implemente exatamente a tabela de docs/MAQUINA_DE_ESTADOS.md:
     * <pre>
     * PRONTO_PARA_CONFIRMACAO -> AUTORIZADO | RECUSADO_INTERNO | CANCELADO_PELO_USUARIO | AGENDADO
     * AUTORIZADO              -> ENVIADO_SPI
     * ENVIADO_SPI             -> LIQUIDADO | REJEITADO | EM_VERIFICACAO
     * EM_VERIFICACAO          -> LIQUIDADO | REJEITADO
     * AGENDADO                -> CANCELADO_PELO_USUARIO | REVALIDANDO
     * REVALIDANDO             -> AUTORIZADO | AGUARDANDO_RETENTATIVA | NAO_EFETIVADO
     * AGUARDANDO_RETENTATIVA  -> REVALIDANDO | NAO_EFETIVADO
     * </pre>
     * Qualquer outra (inclusive de um estado para ele mesmo) é proibida. Estados finais:
     * LIQUIDADO, REJEITADO, CANCELADO_PELO_USUARIO, RECUSADO_INTERNO, NAO_EFETIVADO.
     */
    public boolean transicaoPermitida(EstadoPagamento de, EstadoPagamento para) {
        throw new RegraNaoImplementada("R26", "RegrasPagamento.transicaoPermitida");
    }

    /**
     * R27: gerar o EndToEndId da ordem.
     * <p>
     * Formato do Pix (32 caracteres): "E" + ISPB do pagador (8 dígitos) + data/hora em UTC no
     * formato yyyyMMddHHmm + 11 caracteres alfanuméricos (sequencial do participante).
     * Lance IllegalArgumentException se o ISPB não tiver 8 dígitos ou o sufixo não tiver
     * 11 caracteres alfanuméricos.
     */
    public String gerarEndToEndId(String ispbPagador, Instant agora, String sufixo11) {
        throw new RegraNaoImplementada("R27", "RegrasPagamento.gerarEndToEndId");
    }

    /**
     * R28: montar a pacs.008 a partir do QR Code.
     * <p>
     * Manual de Padrões, Anexo V, seções 2.1 e 2.2:
     * <ul>
     *   <li>formaIniciacao: QRES para QR estático; QRDN para dinâmico (e composto com pagamento).</li>
     *   <li>chave (CdtrAcct.Prxy.Id): sempre a chave Pix do recebedor.</li>
     *   <li>txId: no estático, o txid do campo 62-05 se for diferente de "***" (senão nulo);
     *       no dinâmico, SEMPRE o txid do payload (o do QR é ignorado).</li>
     *   <li>remittanceInformation (RmtInf): a resposta do pagador à solicitacaoPagador
     *       (nula se em branco). Nunca o texto da pergunta.</li>
     *   <li>valor, purpose, valorCompra, valorRetirada, modalidadeAgente e prestador: de
     *       {@code dados.valores()} (R17).</li>
     *   <li>pagador e recebedor: ISPB, agência, conta (e tipo de conta do recebedor) e CPF/CNPJ.
     *       Agência e conta vão na mensagem; só não podem aparecer na TELA.</li>
     * </ul>
     */
    public Pacs008 montarPacs008(DadosOrdem dados) {
        throw new RegraNaoImplementada("R28", "RegrasPagamento.montarPacs008");
    }

    /**
     * R29: a ordem ficou sem resposta do SPI. E agora?
     * <p>
     * Regra de engenharia (não é texto do BCB, mas todo PSP precisa dela): NUNCA reenvie a
     * ordem sem saber o que aconteceu com a primeira, ou o cliente paga duas vezes.
     * <ul>
     *   <li>LIQUIDADO: MARCAR_LIQUIDADO. REJEITADO: MARCAR_REJEITADO.</li>
     *   <li>EM_PROCESSAMENTO: CONSULTAR_NOVAMENTE.</li>
     *   <li>NAO_ENCONTRADO: CONSULTAR_NOVAMENTE enquanto consultasFeitas &lt; maxConsultas;
     *       depois disso, ESTORNAR_SEM_LIQUIDACAO (a ordem nunca entrou no SPI).</li>
     * </ul>
     */
    public AcaoPosTimeout decidirAposTimeout(StatusConsultaSpi status, int consultasFeitas, int maxConsultas) {
        throw new RegraNaoImplementada("R29", "RegrasPagamento.decidirAposTimeout");
    }

    /**
     * R30: mensagem ao pagador quando o Pix não foi concluído.
     * <p>
     * UX cap. 06 item 10 e cap. 07 item 12: a mensagem deve ser específica e deixar claro o motivo.
     * Neste lab, os códigos são:
     * <ul>
     *   <li>"SALDO": saldo insuficiente (a mensagem deve conter "saldo");</li>
     *   <li>"AC03": conta do recebedor inexistente (deve conter "inexistente");</li>
     *   <li>"AC06": conta do recebedor indisponível (deve conter "indisponível");</li>
     *   <li>"AB03": tempo de processamento extrapolado; deixe claro que o erro é do próprio PSP
     *       (deve conter "tempo");</li>
     *   <li>qualquer outro: erro de processamento (deve conter "processamento").</li>
     * </ul>
     * Os códigos reais de rejeição estão no Catálogo de Mensagens do Pix.
     */
    public String mensagemDeRejeicao(String codigoMotivo) {
        throw new RegraNaoImplementada("R30", "RegrasPagamento.mensagemDeRejeicao");
    }
}
