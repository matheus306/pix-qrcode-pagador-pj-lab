package br.lab.pix.pagador.regras;

import br.lab.pix.pagador.dominio.CalendarioDiasUteis;
import br.lab.pix.pagador.dominio.Cobranca;
import br.lab.pix.pagador.dominio.ContaPagador;
import br.lab.pix.pagador.dominio.Recusa;
import br.lab.pix.pagador.dominio.ValoresDaOrdem;
import br.lab.pix.pagador.dominio.ValoresInformados;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * Bloco 3: o payload da cobrança (imediata e com vencimento).
 * Fonte principal: Manual de Padrões para Iniciação do Pix, seção 2.7 e Anexo V.
 */
public class RegrasCobranca {

    /**
     * R11: o que fazer com o status HTTP da URL do payload?
     * <p>
     * 200: segue. 404 ou 410: o recebedor está dizendo que a cobrança já foi concluída,
     * expirou ou foi removida (seção 2.7.3): COBRANCA_NAO_PAGAVEL. Qualquer outro
     * (5xx, timeout = 0, etc.): o PSP recebedor está indisponível: PAYLOAD_INDISPONIVEL
     * (Requisitos Mínimos de UX, cap. 07, item 04: mensagem de falha de comunicação).
     */
    public Optional<Recusa> interpretarHttpDoPayload(int httpStatus) {
        throw new RegraNaoImplementada("R11", "RegrasCobranca.interpretarHttpDoPayload");
    }

    /**
     * R12: a cobrança pode ser paga?
     * <p>
     * Só "ATIVA" é pagável. Todo o resto recusa com COBRANCA_NAO_PAGAVEL, inclusive:
     * "CONCLUIDA"/"CONCLUÍDA", "REMOVIDO_PELO_USUARIO_RECEBEDOR" (v2.10) ou
     * "REMOVIDA_PELO_USUARIO_RECEBEDOR" (grafia da v2.11-rc), os equivalentes "_PELO_PSP",
     * e os novos status da cobrança híbrida (a partir de 01/02/2027):
     * "CONCLUÍDA_POR_OUTRO_ARRANJO" e "REMOVIDA_PELO_PSP_BOLETO".
     */
    public Optional<Recusa> validarStatus(String status) {
        throw new RegraNaoImplementada("R12", "RegrasCobranca.validarStatus");
    }

    /**
     * R13: a cobrança imediata já expirou?
     * <p>
     * Seção 2.7.1.1: o prazo conta a partir de calendario.criacao. Se calendario.expiracao vier
     * nulo, vale o padrão de 86400 segundos (24h). Depois de criacao + expiracao: QR_EXPIRADO
     * (na tela, "QR Code inválido" ou "expirado", UX cap. 07 item 09). No limite exato ainda vale.
     */
    public Optional<Recusa> validarExpiracao(Instant criacao, Integer expiracaoSegundos, Instant agora) {
        throw new RegraNaoImplementada("R13", "RegrasCobranca.validarExpiracao");
    }

    /**
     * R14: o txid do payload do QR dinâmico é válido?
     * <p>
     * Seção 2.7.1.1: de 26 a 35 caracteres, só letras e dígitos. Senão: QR_INVALIDO.
     * (No QR dinâmico o txid que vale é o do payload, nunca o do campo 62-05 da imagem.)
     */
    public Optional<Recusa> validarTxidDinamico(String txid) {
        throw new RegraNaoImplementada("R14", "RegrasCobranca.validarTxidDinamico");
    }

    /**
     * R15: o texto de valor está no formato do Pix?
     * <p>
     * Todos os campos monetários seguem o padrão \d{1,10}\.\d{2}: "0.00", "1.00", "123456789.23".
     * "1.5", "1,50", "-1.00" e nulo são inválidos.
     */
    public boolean valorNoFormato(String valor) {
        throw new RegraNaoImplementada("R15", "RegrasCobranca.valorNoFormato");
    }

    /**
     * R16: os valores que o pagador digitou são aceitáveis para esta cobrança imediata?
     * <p>
     * Seção 2.7.1.1 (tabela "Em síntese"). Recuse com VALOR_INVALIDO se:
     * <ul>
     *   <li>a cobrança traz saque E troco ao mesmo tempo (são mutuamente exclusivos);</li>
     *   <li>modalidadeAlteracao do valor original é 0 (ou ausente) e o pagador digitou um valor
     *       diferente de valor.original;</li>
     *   <li>o mesmo para saque/troco: modalidade 0 (ou ausente) e valor digitado diferente;</li>
     *   <li>a soma efetiva (original + saque + troco) for zero ou negativa. "Efetivo" = o que
     *       o pagador digitou, ou o valor da cobrança se ele não digitou.</li>
     * </ul>
     */
    public Optional<Recusa> validarValoresInformados(Cobranca.Valor valor, ValoresInformados informado) {
        throw new RegraNaoImplementada("R16", "RegrasCobranca.validarValoresInformados");
    }

    /**
     * R17: quais valores vão para a pacs.008? (considere que R16 já aprovou)
     * <p>
     * Anexo V, seção 2.2:
     * <ul>
     *   <li>Sem saque/troco: purpose "IPAY", total = original efetivo.</li>
     *   <li>Saque: purpose "OTHR", total = valor do saque; valorRetirada = saque; copie
     *       modalidadeAgente e prestadorDoServicoDeSaque. (No saque, valor.original é 0.00.)</li>
     *   <li>Troco: purpose "GSCB", total = original + troco; valorCompra = original;
     *       valorRetirada = troco; copie modalidadeAgente e prestador.</li>
     * </ul>
     * Mapeie "AGPSS" (como vem da API Pix) para "AGFSS" (domínio da pacs.008), nota 111.
     */
    public ValoresDaOrdem valoresDaOrdem(Cobranca.Valor valor, ValoresInformados informado) {
        throw new RegraNaoImplementada("R17", "RegrasCobranca.valoresDaOrdem");
    }

    /**
     * R18: quais parâmetros o pagador manda ao consultar uma cobrança com vencimento?
     * <p>
     * Seção 2.7.1.2: "codMun" (código IBGE de 7 dígitos do município do CADASTRO do pagador)
     * e "DPP" (data de pagamento pretendida, yyyy-MM-dd). Se a conta não tiver um codMun
     * de 7 dígitos, mande só a DPP.
     */
    public Map<String, String> parametrosConsultaCobv(ContaPagador conta, LocalDate dataPagamentoPretendida) {
        throw new RegraNaoImplementada("R18", "RegrasCobranca.parametrosConsultaCobv");
    }

    /**
     * R19: até que dia a cobrança com vencimento aceita pagamento?
     * <p>
     * Seção 2.7.1.2 e notas 44/45: limite = dataDeVencimento + validadeAposVencimento (dias
     * corridos, contados do vencimento original). Se o limite cair em dia não útil para o
     * pagador, ele passa para o próximo dia útil. Depois desse dia: "QR Code vencido".
     */
    public LocalDate dataLimiteCobv(LocalDate dataDeVencimento, int validadeAposVencimento,
                                    CalendarioDiasUteis calendario) {
        throw new RegraNaoImplementada("R19", "RegrasCobranca.dataLimiteCobv");
    }

    /**
     * R20: o valor final da cobrança com vencimento é coerente?
     * <p>
     * Anexo V, seção 2.2: o valor liquidado é valor.final, que equivale a
     * original + multa + juros - abatimento - desconto (ausente = 0). O pagador NÃO recalcula
     * nem altera o valor, mas a v2.11-rc reforça que ele deve garantir a integridade do
     * payload antes de iniciar. Recuse com VALOR_INVALIDO se valor.final estiver fora do formato,
     * for zero, ou (quando valor.original existir) não bater com a conta acima.
     */
    public Optional<Recusa> validarCoerenciaValorFinal(Cobranca.Valor valor) {
        throw new RegraNaoImplementada("R20", "RegrasCobranca.validarCoerenciaValorFinal");
    }
}
