package br.lab.pix.pagador.fluxo;

import br.lab.pix.pagador.dominio.AcaoPosTimeout;
import br.lab.pix.pagador.dominio.Cobranca;
import br.lab.pix.pagador.dominio.CodigoRecusa;
import br.lab.pix.pagador.dominio.ContaPagador;
import br.lab.pix.pagador.dominio.DadosComprovante;
import br.lab.pix.pagador.dominio.DadosOrdem;
import br.lab.pix.pagador.dominio.EstadoPagamento;
import br.lab.pix.pagador.dominio.EventoAgendamento;
import br.lab.pix.pagador.dominio.Pagamento;
import br.lab.pix.pagador.dominio.Recusa;
import br.lab.pix.pagador.dominio.StatusConsultaSpi;
import br.lab.pix.pagador.dominio.ValoresDaOrdem;
import br.lab.pix.pagador.dominio.ValoresInformados;
import br.lab.pix.pagador.infra.Contas;
import br.lab.pix.pagador.infra.GatewaySpi;
import br.lab.pix.pagador.infra.Relogio;
import br.lab.pix.pagador.infra.RepositorioPagamentos;
import br.lab.pix.pagador.regras.RegraNaoImplementada;
import br.lab.pix.pagador.regras.RegrasAgendamento;
import br.lab.pix.pagador.regras.RegrasComprovante;
import br.lab.pix.pagador.regras.RegrasCobranca;
import br.lab.pix.pagador.regras.RegrasExibicao;
import br.lab.pix.pagador.regras.RegrasPagamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import static br.lab.pix.pagador.fluxo.FalhaDeFluxo.exigir;

/** Máquinas M2 (pagar agora) e o começo da M3 (agendar), mais o retorno do SPI. */
@Service
public class FluxoPagamento {

    private static final Logger log = LoggerFactory.getLogger(FluxoPagamento.class);
    private static final String ALFANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final RegrasCobranca cobranca;
    private final RegrasExibicao exibicao;
    private final RegrasPagamento pagamento;
    private final RegrasAgendamento agendamento;
    private final RegrasComprovante comprovante;
    private final FluxoLeitura fluxoLeitura;
    private final Transicoes transicoes;
    private final RepositorioPagamentos repositorio;
    private final Contas contas;
    private final GatewaySpi spi;
    private final Relogio relogio;
    private final int timeoutSpiSegundos;
    private final int maxConsultasSpi;
    private final LocalTime horarioLimiteRetentativa;
    private final SecureRandom aleatorio = new SecureRandom();

    public FluxoPagamento(RegrasCobranca cobranca, RegrasExibicao exibicao, RegrasPagamento pagamento,
                          RegrasAgendamento agendamento, RegrasComprovante comprovante, FluxoLeitura fluxoLeitura,
                          Transicoes transicoes, RepositorioPagamentos repositorio, Contas contas, GatewaySpi spi,
                          Relogio relogio,
                          @Value("${pix.spi.timeout-segundos}") int timeoutSpiSegundos,
                          @Value("${pix.spi.max-consultas}") int maxConsultasSpi,
                          @Value("${pix.agendamento.horario-limite-retentativa}") String horarioLimiteRetentativa) {
        this.cobranca = cobranca;
        this.exibicao = exibicao;
        this.pagamento = pagamento;
        this.agendamento = agendamento;
        this.comprovante = comprovante;
        this.fluxoLeitura = fluxoLeitura;
        this.transicoes = transicoes;
        this.repositorio = repositorio;
        this.contas = contas;
        this.spi = spi;
        this.relogio = relogio;
        this.timeoutSpiSegundos = timeoutSpiSegundos;
        this.maxConsultasSpi = maxConsultasSpi;
        this.horarioLimiteRetentativa = LocalTime.parse(horarioLimiteRetentativa);
    }

    public Pagamento buscar(String id) {
        return repositorio.buscar(id).orElseThrow(() ->
                new FalhaDeFluxo(CodigoRecusa.NAO_SUPORTADO, "Pagamento " + id + " não encontrado."));
    }

    public Pagamento confirmar(String id, Confirmacao c) {
        Pagamento p = buscar(id);
        if (p.estado != EstadoPagamento.PRONTO_PARA_CONFIRMACAO) {
            throw new FalhaDeFluxo(CodigoRecusa.ESTADO_INVALIDO, "Pagamento está em " + p.estado + ".");
        }
        exigir(exibicao.validarRespostaAoPagador(p.solicitacaoPagador, c.respostaPagador()));    // R24
        p.respostaPagador = c.respostaPagador();
        if (c.dataPagamento() != null && c.dataPagamento().isAfter(relogio.hoje())) {
            return agendar(p, c.dataPagamento());
        }
        return pagarAgora(p, c);
    }

    // ---------------------------------------------------------------- M2: pagar agora

    private Pagamento pagarAgora(Pagamento p, Confirmacao c) {
        ContaPagador conta = contas.buscar(p.idConta);
        ValoresInformados informado = new ValoresInformados(c.valor(), c.valorSaque(), c.valorTroco());
        p.valoresInformados = informado;
        ValoresDaOrdem valores;
        if (fluxoLeitura.ehCobv(p)) {
            // Com vencimento: o valor é sempre o valor.final devolvido pelo recebedor.
            valores = new ValoresDaOrdem(new BigDecimal(p.cobranca.valor().valorFinal()), "IPAY", null, null, null, null);
        } else {
            exigir(cobranca.validarValoresInformados(p.valorDaCobranca, informado));             // R16
            valores = cobranca.valoresDaOrdem(p.valorDaCobranca, informado);                     // R17
        }
        p.valores = valores;

        if (valores.total().compareTo(conta.saldo().add(conta.limiteCredito())) > 0) {
            transicoes.mover(p, EstadoPagamento.RECUSADO_INTERNO);                                // R26
            p.motivo = Recusa.de(CodigoRecusa.SALDO_INSUFICIENTE, pagamento.mensagemDeRejeicao("SALDO")); // R30
            p.mensagemAoUsuario = p.motivo.mensagem();
            repositorio.salvar(p);
            return p;
        }
        p.avisoCredito = exibicao.avisoUsoDeCredito(conta, valores.total()).orElse(null);        // R25
        transicoes.mover(p, EstadoPagamento.AUTORIZADO);                                          // R26
        enviarAoSpi(p, conta);
        return p;
    }

    /** AUTORIZADO -> débito -> pacs.008 -> ENVIADO_SPI. Também usado pelo agendamento no dia D. */
    void enviarAoSpi(Pagamento p, ContaPagador conta) {
        contas.debitar(conta.id(), p.valores.total());
        p.valorDebitado = p.valores.total();
        p.endToEndId = pagamento.gerarEndToEndId(conta.ispb(), relogio.agora(), sufixo());         // R27
        String txidPayload = p.cobranca == null ? null : p.cobranca.txid();
        p.pacs008 = pagamento.montarPacs008(new DadosOrdem(p.tipoQr, p.txidDoQr, txidPayload,      // R28
                p.chave, p.valores, p.respostaPagador, p.endToEndId, conta, p.recebedor));
        spi.enviar(p.pacs008);
        p.enviadoEm = relogio.agora();
        transicoes.mover(p, EstadoPagamento.ENVIADO_SPI);                                         // R26
        repositorio.salvar(p);
    }

    // ---------------------------------------------------------------- M3: agendar

    private Pagamento agendar(Pagamento p, LocalDate data) {
        ContaPagador conta = contas.buscar(p.idConta);
        exigir(agendamento.validarPedidoDeAgendamento(p.tipoQr, p.tipoCobranca, data,             // R31
                relogio.hoje(), p.dataLimiteCobv));

        // A DPP mudou: o recebedor recalcula juros/multa/desconto para a data agendada.
        Cobranca cob = fluxoLeitura.reconsultarCobv(p, conta, data, "agendamento");
        exigir(cobranca.validarStatus(cob.status()));                                             // R12
        exigir(cobranca.validarCoerenciaValorFinal(cob.valor()));                                 // R20
        BigDecimal valor = new BigDecimal(cob.valor().valorFinal());

        BigDecimal jaAgendado = repositorio.filtrar(o -> o.idConta.equals(p.idConta)
                        && o.estado == EstadoPagamento.AGENDADO && data.equals(o.dataAgendada))
                .stream().map(o -> o.valorAgendado).reduce(BigDecimal.ZERO, BigDecimal::add);
        exigir(agendamento.validarLimitePixAgendado(valor, conta.limitePixAgendadoDiario(), jaAgendado)); // R32

        p.cobranca = cob;
        p.dataAgendada = data;
        p.dataDoAgendamento = relogio.hoje();
        p.valorAgendado = valor;
        transicoes.mover(p, EstadoPagamento.AGENDADO);                                            // R26
        p.comprovanteAgendamento = comprovante.montarComprovante(dadosComprovante(p, conta, true)); // R37
        repositorio.salvar(p);
        return p;
    }

    public Pagamento cancelar(String id) {
        Pagamento p = buscar(id);
        if (p.estado == EstadoPagamento.AGENDADO
                && !agendamento.podeCancelarAgendamento(p.dataAgendada, relogio.agoraBrasilia())) { // R33
            throw new FalhaDeFluxo(CodigoRecusa.CANCELAMENTO_FORA_DO_PRAZO,
                    "O agendamento só pode ser cancelado até as 23:59 do dia anterior.");
        }
        transicoes.mover(p, EstadoPagamento.CANCELADO_PELO_USUARIO);                               // R26
        repositorio.salvar(p);
        return p;
    }

    // ---------------------------------------------------------------- retorno do SPI

    @Scheduled(fixedDelay = 500)
    public void processarRetornosDoSpi() {
        try {
            for (GatewaySpi.Pacs002 r : spi.receberRetornos()) {
                repositorio.porEndToEndId(r.endToEndId()).ifPresent(p -> {
                    if ("ACSC".equals(r.status())) {
                        liquidar(p);
                    } else {
                        rejeitar(p, r.motivo());
                    }
                });
            }
        } catch (RegraNaoImplementada e) {
            avisarUmaVez(e);
        } catch (Exception e) {
            log.debug("Retorno do SPI ainda indisponível: {}", e.getMessage());
        }
    }

    /** Ordens sem resposta: ENVIADO_SPI vira EM_VERIFICACAO e o pagador CONSULTA (máquina M2). */
    @Scheduled(fixedDelay = 2000)
    public void verificarOrdensSemResposta() {
        try {
            for (Pagamento p : repositorio.noEstado(EstadoPagamento.ENVIADO_SPI)) {
                if (Duration.between(p.enviadoEm, relogio.agora()).toSeconds() >= timeoutSpiSegundos) {
                    transicoes.mover(p, EstadoPagamento.EM_VERIFICACAO);                           // R26
                    repositorio.salvar(p);
                }
            }
            for (Pagamento p : repositorio.noEstado(EstadoPagamento.EM_VERIFICACAO)) {
                StatusConsultaSpi status = spi.consultar(p.endToEndId);
                p.consultasSpi++;
                AcaoPosTimeout acao = pagamento.decidirAposTimeout(status, p.consultasSpi, maxConsultasSpi); // R29
                switch (acao) {
                    case MARCAR_LIQUIDADO -> liquidar(p);
                    case MARCAR_REJEITADO -> rejeitar(p, "AB03");
                    case ESTORNAR_SEM_LIQUIDACAO -> rejeitar(p, "AB03");
                    case CONSULTAR_NOVAMENTE -> repositorio.salvar(p);
                    case REENVIAR_ORDEM -> throw new IllegalStateException(
                            "R29 mandou reenviar uma ordem sem resposta: isso pode pagar em dobro!");
                }
            }
        } catch (RegraNaoImplementada e) {
            avisarUmaVez(e);
        } catch (Exception e) {
            log.debug("Verificação de ordens ainda indisponível: {}", e.getMessage());
        }
    }

    private void liquidar(Pagamento p) {
        ContaPagador conta = contas.buscar(p.idConta);
        transicoes.mover(p, EstadoPagamento.LIQUIDADO);                                          // R26
        p.liquidadoEm = relogio.agora();
        p.comprovante = comprovante.montarComprovante(dadosComprovante(p, conta, false));          // R37
        BigDecimal valor = p.valores.total();
        agendamento.notificar(EventoAgendamento.LIQUIDADO, p.destinatario.nome(), valor,          // R36
                relogio.agoraLocalBrasilia(), horarioLimiteRetentativa).ifPresent(p.notificacoes::add);
        p.mensagemAoUsuario = "Pix confirmado.";
        repositorio.salvar(p);
    }

    private void rejeitar(Pagamento p, String motivo) {
        transicoes.mover(p, EstadoPagamento.REJEITADO);                                          // R26
        if (p.valorDebitado != null) {
            contas.creditar(p.idConta, p.valorDebitado);
        }
        p.mensagemAoUsuario = pagamento.mensagemDeRejeicao(motivo);                                // R30
        repositorio.salvar(p);
    }

    DadosComprovante dadosComprovante(Pagamento p, ContaPagador conta, boolean doAgendamento) {
        BigDecimal valor = doAgendamento ? p.valorAgendado : p.valores.total();
        String txid = p.cobranca != null ? p.cobranca.txid()
                : ("***".equals(p.txidDoQr) ? null : p.txidDoQr);
        return new DadosComprovante(doAgendamento, conta.razaoSocial(), conta.cnpj(), conta.nomePsp(), p.recebedor,
                txid, valor, p.endToEndId, p.liquidadoEm, p.dataAgendada, p.dataDoAgendamento, p.dadosDaCobranca);
    }

    private String sufixo() {
        StringBuilder sb = new StringBuilder(11);
        for (int i = 0; i < 11; i++) {
            sb.append(ALFANUM.charAt(aleatorio.nextInt(ALFANUM.length())));
        }
        return sb.toString();
    }

    private final java.util.Set<String> regrasJaAvisadas = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private void avisarUmaVez(RegraNaoImplementada e) {
        if (regrasJaAvisadas.add(e.getRegra())) {
            log.warn("Processamento em segundo plano parado: {}", e.getMessage());
        }
    }
}
