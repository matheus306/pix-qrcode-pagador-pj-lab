package br.lab.pix.pagador.dominio;

/**
 * Vínculo de uma chave Pix devolvido pelo DICT.
 *
 * @param tipoPessoa     "F" (natural) ou "J" (jurídica)
 * @param suspeitaFraude conta/usuário com marcação de fundada suspeita de fraude
 */
public record DadosDict(
        String chave,
        String tipoPessoa,
        String cpfCnpj,
        String nome,
        String nomeFantasia,
        String ispb,
        String nomePsp,
        String agencia,
        String conta,
        String tipoConta,
        boolean suspeitaFraude) {
}
