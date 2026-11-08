package br.lab.pix.pagador.fluxo;

import br.lab.pix.pagador.dominio.AcaoRetentativa;
import br.lab.pix.pagador.dominio.Cobranca;
import br.lab.pix.pagador.dominio.CodigoRecusa;
import br.lab.pix.pagador.dominio.ContaPagador;
import br.lab.pix.pagador.dominio.DecisaoRevalidacao;
import br.lab.pix.pagador.dominio.EstadoPagamento;
import br.lab.pix.pagador.dominio.EventoAgendamento;
import br.lab.pix.pagador.dominio.MotivoFalha;
import br.lab.pix.pagador.dominio.Pagamento;
import br.lab.pix.pagador.dominio.RespostaDict;
import br.lab.pix.pagador.dominio.ValoresDaOrdem;
import br.lab.pix.pagador.infra.ClienteDict;
import br.lab.pix.pagador.infra.Contas;
import br.lab.pix.pagador.infra.Relogio;
import br.lab.pix.pagador.infra.RepositorioPagamentos;
import br.lab.pix.pagador.regras.RegrasAgendamento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Máquina M3 no dia da liquidação. No mundo real isto roda em janelas de processamento;
 * aqui você dispara cada janela pela API de simulação (janela 1 = primeira tentativa,
 * janela 2 = retentativa do mesmo dia).
 */
@Service
public class FluxoAgendamento {

    private final RegrasAgendamento regras;
    private final FluxoLeitura fluxoLeitura;
    private final FluxoPagamento fluxoPagamento;
    private final Transicoes transicoes;
    private final RepositorioPagamentos repositorio;
    private final ClienteDict dict;
    private final Contas contas;
    private final Relogio relogio;
    private final int maxTentativas;
    private final LocalTime horarioLimiteRetentativa;

    public FluxoAgendamento(RegrasAgendamento regras, FluxoLeitura fluxoLeitura, FluxoPagamento fluxoPagamento,
                            Transicoes transicoes, RepositorioPagamentos repositorio, ClienteDict dict,
                            Contas contas, Relogio relogio,
                            @Value("${pix.agendamento.max-tentativas}") int maxTentativas,
                            @Value("${pix.agendamento.horario-limite-retentativa}") String horarioLimiteRetentativa) {
        this.regras = regras;
        this.fluxoLeitura = fluxoLeitura;
        this.fluxoPagamento = fluxoPagamento;
        this.transicoes = transicoes;
        this.repositorio = repositorio;
        this.dict = dict;
        this.contas = contas;
        this.relogio = relogio;
        this.maxTentativas = maxTentativas;
        this.horarioLimiteRetentativa = LocalTime.parse(horarioLimiteRetentativa);
    }

    public List<Pagamento> processarJanela(LocalDate data, int janela) {
        EstadoPagamento alvo = janela == 1 ? EstadoPagamento.AGENDADO : EstadoPagamento.AGUARDANDO_RETENTATIVA;
        List<Pagamento> processados = new ArrayList<>();
        for (Pagamento p : repositorio.filtrar(o -> o.estado == alvo && data.equals(o.dataAgendada))) {
            processar(p, data);
            processados.add(repositorio.buscar(p.id).orElse(p));
        }
        return processados;
    }

    private void processar(Pagamento p, LocalDate data) {
        ContaPagador conta = contas.buscar(p.idConta);
        transicoes.mover(p, EstadoPagamento.REVALIDANDO);                                         // R26
        p.tentativas++;

        // Revalidação: cobrança de novo (DPP = hoje) e DICT de novo.
        String statusAtual;
        BigDecimal valorAtual = null;
        try {
            Cobranca cob = fluxoLeitura.reconsultarCobv(p, conta, data, "liquidacao-" + p.tentativas);
            statusAtual = cob.status();
            valorAtual = new BigDecimal(cob.valor().valorFinal());
        } catch (FalhaDeFluxo f) {
            if (f.getRecusa().codigo() != CodigoRecusa.COBRANCA_NAO_PAGAVEL) {
                falhar(p, MotivoFalha.FALHA_OPERACIONAL);
                return;
            }
            statusAtual = "INDISPONIVEL_NO_RECEBEDOR";
        }
        RespostaDict dictAtual = dict.consultar(p.chave);

        DecisaoRevalidacao decisao = regras.revalidarNoDia(statusAtual, dictAtual,                // R34
                p.recebedor.cpfCnpj(), valorAtual, p.valorAgendado);
        switch (decisao) {
            case FALHA_OPERACIONAL -> falhar(p, MotivoFalha.FALHA_OPERACIONAL);
            case NAO_EFETIVAR_COBRANCA_NAO_PAGAVEL -> naoEfetivar(p, EventoAgendamento.COBRANCA_NAO_PAGAVEL);
            case NAO_EFETIVAR_CHAVE_MUDOU -> naoEfetivar(p, EventoAgendamento.CHAVE_MUDOU_OU_INEXISTENTE);
            case NAO_EFETIVAR_VALOR_DIVERGENTE -> naoEfetivar(p, EventoAgendamento.COBRANCA_NAO_PAGAVEL);
            case SEGUIR -> liquidarSePuder(p, conta, data);
        }
    }

    private void liquidarSePuder(Pagamento p, ContaPagador conta, LocalDate data) {
        BigDecimal jaLiquidadoNoDia = repositorio.filtrar(o -> o.idConta.equals(p.idConta) && !o.id.equals(p.id)
                        && data.equals(o.dataAgendada) && o.valorDebitado != null
                        && o.estado != EstadoPagamento.REJEITADO)
                .stream().map(o -> o.valorDebitado).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (regras.validarLimitePixAgendado(p.valorAgendado, conta.limitePixAgendadoDiario(), jaLiquidadoNoDia) // R32
                .isPresent()) {
            falhar(p, MotivoFalha.LIMITE_AGENDADO_INSUFICIENTE);
            return;
        }
        if (p.valorAgendado.compareTo(conta.saldo().add(conta.limiteCredito())) > 0) {
            falhar(p, MotivoFalha.SALDO_INSUFICIENTE);
            return;
        }
        p.valores = new ValoresDaOrdem(p.valorAgendado, "IPAY", null, null, null, null);
        transicoes.mover(p, EstadoPagamento.AUTORIZADO);                                          // R26
        fluxoPagamento.enviarAoSpi(p, conta);
    }

    private void falhar(Pagamento p, MotivoFalha motivo) {
        AcaoRetentativa acao = regras.decidirRetentativa(motivo, p.tentativas, maxTentativas);     // R35
        EventoAgendamento evento;
        if (acao == AcaoRetentativa.RETENTAR_MESMO_DIA) {
            transicoes.mover(p, EstadoPagamento.AGUARDANDO_RETENTATIVA);                           // R26
            evento = motivo == MotivoFalha.SALDO_INSUFICIENTE
                    ? EventoAgendamento.FALHA_SALDO_PRIMEIRA_TENTATIVA
                    : EventoAgendamento.FALHA_OPERACIONAL_INTERMEDIARIA;
        } else {
            transicoes.mover(p, EstadoPagamento.NAO_EFETIVADO);                                    // R26
            evento = switch (motivo) {
                case SALDO_INSUFICIENTE -> EventoAgendamento.FALHA_SALDO_ULTIMA_TENTATIVA;
                case LIMITE_AGENDADO_INSUFICIENTE -> EventoAgendamento.FALHA_LIMITE_AGENDADO;
                case FALHA_OPERACIONAL -> EventoAgendamento.FALHA_OPERACIONAL_ULTIMA;
            };
        }
        notificar(p, evento);
        repositorio.salvar(p);
    }

    private void naoEfetivar(Pagamento p, EventoAgendamento evento) {
        transicoes.mover(p, EstadoPagamento.NAO_EFETIVADO);                                        // R26
        notificar(p, evento);
        repositorio.salvar(p);
    }

    private void notificar(Pagamento p, EventoAgendamento evento) {
        regras.notificar(evento, p.destinatario.nome(), p.valorAgendado,                          // R36
                relogio.agoraLocalBrasilia(), horarioLimiteRetentativa).ifPresent(n -> {
            p.notificacoes.add(n);
            p.mensagemAoUsuario = n.texto();
        });
    }
}
