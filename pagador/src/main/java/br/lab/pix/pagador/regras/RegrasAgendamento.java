package br.lab.pix.pagador.regras;

import br.lab.pix.pagador.dominio.AcaoRetentativa;
import br.lab.pix.pagador.dominio.DecisaoRevalidacao;
import br.lab.pix.pagador.dominio.EventoAgendamento;
import br.lab.pix.pagador.dominio.MotivoFalha;
import br.lab.pix.pagador.dominio.Notificacao;
import br.lab.pix.pagador.dominio.Recusa;
import br.lab.pix.pagador.dominio.RespostaDict;
import br.lab.pix.pagador.dominio.TipoCobranca;
import br.lab.pix.pagador.dominio.TipoQr;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Bloco 6: Pix Agendado por QR Code.
 * Fonte principal: Requisitos Mínimos para a Experiência do Usuário (v7.3), cap. 12.
 */
public class RegrasAgendamento {

    /**
     * R31: este QR pode ser agendado para esta data?
     * <ul>
     *   <li>O UX regula o agendamento por QR apenas para QR DINÂMICO COM VENCIMENTO. Qualquer
     *       outro tipo: AGENDAMENTO_NAO_PERMITIDO (confirme com o seu compliance se a sua
     *       instituição oferece mais que isso).</li>
     *   <li>A data precisa ser futura (depois de hoje); senão AGENDAMENTO_NAO_PERMITIDO.</li>
     *   <li>Dias não úteis são permitidos (UX cap. 12: "inclusive para dias não úteis").</li>
     *   <li>Depois da data limite da cobrança (R19): QR_VENCIDO.</li>
     * </ul>
     */
    public Optional<Recusa> validarPedidoDeAgendamento(TipoQr tipoQr, TipoCobranca tipoCobranca,
                                                       LocalDate dataAgendada, LocalDate hoje,
                                                       LocalDate dataLimiteCobranca) {
        throw new RegraNaoImplementada("R31", "RegrasAgendamento.validarPedidoDeAgendamento");
    }

    /**
     * R32: o agendamento cabe no limite diário de Pix Agendado da data da liquidação?
     * <p>
     * UX cap. 12 item 17: verificar na hora de agendar e, se exceder, recusar imediatamente
     * com orientação (aumentar o limite, reduzir o valor ou mudar a data): LIMITE_PIX_AGENDADO.
     * Cabe se valor + jaAgendadoNaData &lt;= limiteDiario.
     */
    public Optional<Recusa> validarLimitePixAgendado(BigDecimal valor, BigDecimal limiteDiario,
                                                     BigDecimal jaAgendadoNaData) {
        throw new RegraNaoImplementada("R32", "RegrasAgendamento.validarLimitePixAgendado");
    }

    /**
     * R33: o pagador ainda pode cancelar este agendamento?
     * <p>
     * UX cap. 12: até as 23:59 do dia anterior à data agendada, no horário de Brasília
     * (America/Sao_Paulo). Atenção: {@code agora} pode chegar em qualquer fuso (a VPS costuma
     * estar em UTC).
     */
    public boolean podeCancelarAgendamento(LocalDate dataAgendada, ZonedDateTime agora) {
        throw new RegraNaoImplementada("R33", "RegrasAgendamento.podeCancelarAgendamento");
    }

    /**
     * R34: no dia da liquidação, ainda dá para pagar?
     * <p>
     * O pagador consulta de novo a cobrança (com DPP = hoje) e o DICT antes de liquidar.
     * Recomendação deste lab (consequência do art. 39 do Regulamento, cobrança híbrida):
     * não confie no payload capturado no dia do agendamento. Verifique, nesta ordem:
     * <ol>
     *   <li>DICT fora do ar (status diferente de 200 e 404): FALHA_OPERACIONAL;</li>
     *   <li>status da cobrança diferente de "ATIVA": NAO_EFETIVAR_COBRANCA_NAO_PAGAVEL;</li>
     *   <li>chave não existe mais (404) ou o CPF/CNPJ do titular mudou: NAO_EFETIVAR_CHAVE_MUDOU
     *       (UX cap. 12 item 21);</li>
     *   <li>valor final atual diferente do valor agendado: NAO_EFETIVAR_VALOR_DIVERGENTE;</li>
     *   <li>senão: SEGUIR.</li>
     * </ol>
     */
    public DecisaoRevalidacao revalidarNoDia(String statusCobrancaAtual, RespostaDict dictAtual,
                                            String cpfCnpjRecebedorNoAgendamento,
                                            BigDecimal valorFinalAtual, BigDecimal valorAgendado) {
        throw new RegraNaoImplementada("R34", "RegrasAgendamento.revalidarNoDia");
    }

    /**
     * R35: a tentativa falhou. Tenta de novo hoje?
     * <p>
     * UX cap. 12: falta de saldo exige nova tentativa obrigatória no MESMO dia. Falha operacional
     * também é retentada (sem avisar o cliente). Limite de Pix Agendado insuficiente não é
     * retentado. Com tentativaAtual &gt;= maxTentativas, NAO_EFETIVAR.
     */
    public AcaoRetentativa decidirRetentativa(MotivoFalha motivo, int tentativaAtual, int maxTentativas) {
        throw new RegraNaoImplementada("R35", "RegrasAgendamento.decidirRetentativa");
    }

    /**
     * R36: este acontecimento gera notificação ao pagador? Qual e quando?
     * <p>
     * UX cap. 12, itens 21 a 24:
     * <ul>
     *   <li>LIQUIDADO: notificar com nome do recebedor e valor. Se a liquidação ocorrer entre
     *       00:00 e 05:59, enviar preferencialmente às 06:00 do mesmo dia.</li>
     *   <li>FALHA_SALDO_PRIMEIRA_TENTATIVA: notificar que não foi efetivado por falta de saldo,
     *       que haverá nova tentativa hoje e o horário limite dela (HH:mm), para o cliente
     *       recompor o saldo.</li>
     *   <li>FALHA_SALDO_ULTIMA_TENTATIVA, FALHA_LIMITE_AGENDADO, CHAVE_MUDOU_OU_INEXISTENTE,
     *       COBRANCA_NAO_PAGAVEL e FALHA_OPERACIONAL_ULTIMA: notificar o motivo.</li>
     *   <li>FALHA_OPERACIONAL_INTERMEDIARIA: NÃO notificar.</li>
     * </ul>
     * Exceto no caso da madrugada, enviarEm = agora.
     */
    public Optional<Notificacao> notificar(EventoAgendamento evento, String nomeRecebedor, BigDecimal valor,
                                           LocalDateTime agora, LocalTime horarioLimiteRetentativa) {
        throw new RegraNaoImplementada("R36", "RegrasAgendamento.notificar");
    }
}
