package br.lab.pix.pagador.api;

import br.lab.pix.pagador.dominio.ContaPagador;
import br.lab.pix.pagador.fluxo.FluxoAgendamento;
import br.lab.pix.pagador.infra.Contas;
import br.lab.pix.pagador.infra.Relogio;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Controles do laboratório: relógio, saldo, limites e janelas de agendamento. */
@RestController
@RequestMapping("/api/simulacao")
public class SimulacaoController {

    public record Janela(LocalDate data, int janela) {
    }

    private final Contas contas;
    private final Relogio relogio;
    private final FluxoAgendamento agendamento;

    public SimulacaoController(Contas contas, Relogio relogio, FluxoAgendamento agendamento) {
        this.contas = contas;
        this.relogio = relogio;
        this.agendamento = agendamento;
    }

    @GetMapping("/contas")
    public Collection<ContaPagador> contas() {
        return contas.todas();
    }

    @PutMapping("/contas/{id}/saldo")
    public ContaPagador saldo(@PathVariable String id, @RequestBody Map<String, BigDecimal> body) {
        contas.salvar(contas.buscar(id).comSaldo(body.get("saldo")));
        return contas.buscar(id);
    }

    @PutMapping("/contas/{id}/limite-pix-agendado")
    public ContaPagador limite(@PathVariable String id, @RequestBody Map<String, BigDecimal> body) {
        contas.salvar(contas.buscar(id).comLimitePixAgendado(body.get("limite")));
        return contas.buscar(id);
    }

    @PostMapping("/contas/reiniciar")
    public Collection<ContaPagador> reiniciar() {
        contas.reiniciar();
        return contas.todas();
    }

    @GetMapping("/relogio")
    public Map<String, Object> relogio() {
        return Map.of("agoraUtc", relogio.agora().toString(), "agoraBrasilia", relogio.agoraBrasilia().toString());
    }

    /** Ex.: {"agora": "2026-11-13T12:00:00Z"} */
    @PostMapping("/relogio")
    public Map<String, Object> definir(@RequestBody Map<String, String> body) {
        relogio.definir(Instant.parse(body.get("agora")));
        return relogio();
    }

    @DeleteMapping("/relogio")
    public Map<String, Object> voltar() {
        relogio.voltarAoNormal();
        return relogio();
    }

    /** Roda a janela 1 (primeira tentativa) ou 2 (retentativa) dos agendamentos de uma data. */
    @PostMapping("/agendamentos/processar")
    public List<PagamentoView> processar(@RequestBody Janela janela) {
        return agendamento.processarJanela(janela.data(), janela.janela()).stream().map(PagamentoView::de).toList();
    }
}
