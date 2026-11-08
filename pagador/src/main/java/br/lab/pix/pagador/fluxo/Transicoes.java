package br.lab.pix.pagador.fluxo;

import br.lab.pix.pagador.dominio.CodigoRecusa;
import br.lab.pix.pagador.dominio.EstadoPagamento;
import br.lab.pix.pagador.dominio.Pagamento;
import br.lab.pix.pagador.infra.Relogio;
import br.lab.pix.pagador.regras.RegrasPagamento;
import org.springframework.stereotype.Component;

/** Muda o estado do pagamento SÓ se a regra R26 permitir, e registra no histórico. */
@Component
public class Transicoes {

    private final RegrasPagamento regras;
    private final Relogio relogio;

    public Transicoes(RegrasPagamento regras, Relogio relogio) {
        this.regras = regras;
        this.relogio = relogio;
    }

    public void mover(Pagamento p, EstadoPagamento para) {
        if (!regras.transicaoPermitida(p.estado, para)) {
            throw new FalhaDeFluxo(CodigoRecusa.ESTADO_INVALIDO,
                    "Transição " + p.estado + " -> " + para + " não é permitida (R26).");
        }
        p.historico.add(relogio.agoraBrasilia().toLocalDateTime() + "  " + p.estado + " -> " + para);
        p.estado = para;
    }
}
