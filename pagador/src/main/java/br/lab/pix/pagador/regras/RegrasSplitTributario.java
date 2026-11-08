package br.lab.pix.pagador.regras;

import br.lab.pix.pagador.dominio.FormaIniciacao;

/**
 * Bloco 8: o que vem por aí. Split tributário (IBS/CBS, LC 214/2025).
 * Fonte: Manual de Padrões v2.11.0-rc1 (29/10/2026), Anexo VII. Prazo: a definir.
 */
public class RegrasSplitTributario {

    /**
     * R38: a pacs.008 pode levar o bloco de tributos?
     * <p>
     * Só quando pagador E recebedor são pessoa jurídica (CPF/CNPJ com 14 caracteres, o que
     * inclui o CNPJ alfanumérico) e a forma de iniciação é QRES, APES, MANU ou DICT (com dados
     * informados pelo pagador). Em QRDN, APDN, AUTO e INIC o bloco não é preenchido.
     */
    public boolean podeInformarTributo(String cpfCnpjPagador, String cpfCnpjRecebedor, FormaIniciacao forma) {
        throw new RegraNaoImplementada("R38", "RegrasSplitTributario.podeInformarTributo");
    }
}
