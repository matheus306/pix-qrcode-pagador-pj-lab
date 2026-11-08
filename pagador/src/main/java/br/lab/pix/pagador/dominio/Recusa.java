package br.lab.pix.pagador.dominio;

/** Recusa com o código interno e a mensagem que o usuário pagador vai ler. */
public record Recusa(CodigoRecusa codigo, String mensagem) {

    public static Recusa de(CodigoRecusa codigo, String mensagem) {
        return new Recusa(codigo, mensagem);
    }
}
