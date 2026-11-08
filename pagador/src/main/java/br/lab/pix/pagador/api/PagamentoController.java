package br.lab.pix.pagador.api;

import br.lab.pix.pagador.fluxo.Confirmacao;
import br.lab.pix.pagador.fluxo.FluxoLeitura;
import br.lab.pix.pagador.fluxo.FluxoPagamento;
import br.lab.pix.pagador.infra.RepositorioPagamentos;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/** A "tela" do internet banking PJ, em REST. */
@RestController
@RequestMapping("/api")
public class PagamentoController {

    public record Leitura(String idConta, String brCode) {
    }

    private final FluxoLeitura leitura;
    private final FluxoPagamento pagamento;
    private final RepositorioPagamentos repositorio;

    public PagamentoController(FluxoLeitura leitura, FluxoPagamento pagamento, RepositorioPagamentos repositorio) {
        this.leitura = leitura;
        this.pagamento = pagamento;
        this.repositorio = repositorio;
    }

    /** Pix Copia e Cola / leitura do QR. */
    @PostMapping("/leituras")
    @ResponseStatus(HttpStatus.CREATED)
    public PagamentoView ler(@RequestBody Leitura req) {
        return PagamentoView.de(leitura.ler(req.idConta(), req.brCode()));
    }

    @PostMapping("/pagamentos/{id}/confirmacao")
    public PagamentoView confirmar(@PathVariable String id, @RequestBody Confirmacao req) {
        return PagamentoView.de(pagamento.confirmar(id, req));
    }

    @PostMapping("/pagamentos/{id}/cancelamento")
    public PagamentoView cancelar(@PathVariable String id) {
        return PagamentoView.de(pagamento.cancelar(id));
    }

    @GetMapping("/pagamentos/{id}")
    public PagamentoView buscar(@PathVariable String id) {
        return PagamentoView.de(pagamento.buscar(id));
    }

    @GetMapping("/pagamentos")
    public List<PagamentoView> listar() {
        return repositorio.todos().stream()
                .sorted(Comparator.comparing((br.lab.pix.pagador.dominio.Pagamento p) -> p.criadoEm).reversed())
                .map(PagamentoView::de).toList();
    }
}
