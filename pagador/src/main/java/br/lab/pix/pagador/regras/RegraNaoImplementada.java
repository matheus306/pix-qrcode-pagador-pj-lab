package br.lab.pix.pagador.regras;

/** Lançada pelas regras que você ainda não implementou. A API devolve 501 com o código da regra. */
public class RegraNaoImplementada extends RuntimeException {

    private final String regra;

    public RegraNaoImplementada(String regra, String onde) {
        super(regra + " ainda não implementada: " + onde + ". Veja docs/REGRAS.md.");
        this.regra = regra;
    }

    public String getRegra() {
        return regra;
    }
}
