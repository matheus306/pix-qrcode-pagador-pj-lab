package br.lab.pix.pagador.infra;

import br.lab.pix.pagador.dominio.ContaPagador;
import br.lab.pix.pagador.fluxo.FalhaDeFluxo;
import br.lab.pix.pagador.dominio.CodigoRecusa;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Contas PJ do laboratório, em memória. Use a API de simulação para mudar saldo e limites. */
@Component
public class Contas {

    private final Map<String, ContaPagador> contas = new ConcurrentHashMap<>();

    public Contas() {
        reiniciar();
    }

    public final void reiniciar() {
        contas.clear();
        salvar(new ContaPagador("empresa-a", "11222333000181", "EMPRESA PAGADORA A LTDA", "12345678", "PSP Lab",
                "0001", "998877", "3550308", new BigDecimal("10000.00"), new BigDecimal("5000.00"),
                "Cheque Empresarial", new BigDecimal("20000.00")));
        salvar(new ContaPagador("empresa-sem-saldo", "44555666000110", "EMPRESA B SEM SALDO LTDA", "12345678",
                "PSP Lab", "0001", "554433", "3304557", new BigDecimal("10.00"), BigDecimal.ZERO,
                "Cheque Empresarial", new BigDecimal("1000.00")));
    }

    public ContaPagador buscar(String id) {
        ContaPagador conta = contas.get(id);
        if (conta == null) {
            throw new FalhaDeFluxo(CodigoRecusa.NAO_SUPORTADO, "Conta " + id + " não existe no laboratório.");
        }
        return conta;
    }

    public Collection<ContaPagador> todas() {
        return contas.values();
    }

    public void salvar(ContaPagador conta) {
        contas.put(conta.id(), conta);
    }

    public synchronized void debitar(String id, BigDecimal valor) {
        ContaPagador c = buscar(id);
        salvar(c.comSaldo(c.saldo().subtract(valor)));
    }

    public synchronized void creditar(String id, BigDecimal valor) {
        ContaPagador c = buscar(id);
        salvar(c.comSaldo(c.saldo().add(valor)));
    }
}
