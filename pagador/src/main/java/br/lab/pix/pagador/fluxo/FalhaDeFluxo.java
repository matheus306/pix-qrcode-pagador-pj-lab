package br.lab.pix.pagador.fluxo;

import br.lab.pix.pagador.dominio.CodigoRecusa;
import br.lab.pix.pagador.dominio.Recusa;

import java.util.Optional;

/** Interrompe o fluxo com uma recusa (vira HTTP 422 na API). */
public class FalhaDeFluxo extends RuntimeException {

    private final Recusa recusa;

    public FalhaDeFluxo(Recusa recusa) {
        super(recusa.codigo() + ": " + recusa.mensagem());
        this.recusa = recusa;
    }

    public FalhaDeFluxo(CodigoRecusa codigo, String mensagem) {
        this(Recusa.de(codigo, mensagem));
    }

    public Recusa getRecusa() {
        return recusa;
    }

    /** Se a regra devolveu uma recusa, para o fluxo aqui. */
    public static void exigir(Optional<Recusa> resultado) {
        resultado.ifPresent(r -> {
            throw new FalhaDeFluxo(r);
        });
    }
}
