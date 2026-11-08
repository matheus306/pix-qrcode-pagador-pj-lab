package br.lab.pix.pagador.regras;

import br.lab.pix.pagador.dominio.ContaPagador;
import br.lab.pix.pagador.dominio.DadosDict;
import br.lab.pix.pagador.dominio.DestinatarioExibido;
import br.lab.pix.pagador.dominio.Recusa;
import br.lab.pix.pagador.dominio.RespostaDict;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Bloco 4: DICT e tela de confirmação.
 * Fonte principal: Requisitos Mínimos para a Experiência do Usuário (v7.3), caps. 06, 07 e 19.
 */
public class RegrasExibicao {

    /**
     * R21: o que a consulta ao DICT diz?
     * <ul>
     *   <li>200 sem marcação: segue.</li>
     *   <li>200 com marcação de fraude: FRAUDE. A mensagem é obrigatória e precisa deixar claro
     *       que o recebedor esteve envolvido em transação com "fundada suspeita de fraude"
     *       e que o Pix não pode ser feito (UX cap. 06 item 08).</li>
     *   <li>404: CHAVE_INEXISTENTE (chave inexistente ou indisponível).</li>
     *   <li>Qualquer outro: DICT_INDISPONIVEL (falha de comunicação, tente novamente).</li>
     * </ul>
     */
    public Optional<Recusa> interpretarConsultaDict(RespostaDict resposta) {
        throw new RegraNaoImplementada("R21", "RegrasExibicao.interpretarConsultaDict");
    }

    /**
     * R22: como o recebedor aparece na tela?
     * <p>
     * Sempre com os dados do DICT, nunca com o "Merchant Name" (campo 59) do QR.
     * <ul>
     *   <li>PJ ("J"): nome = nome fantasia se existir, senão a razão social; documento = CNPJ
     *       SEM máscara (os 14 caracteres como vieram).</li>
     *   <li>PF ("F"): documento = CPF COM máscara no formato "***.456.789-**".</li>
     *   <li>Agência e conta do recebedor nunca aparecem (repare que o record nem tem esses campos).</li>
     * </ul>
     */
    public DestinatarioExibido montarDestinatario(DadosDict dict) {
        throw new RegraNaoImplementada("R22", "RegrasExibicao.montarDestinatario");
    }

    /**
     * R23: limpar textos que vêm do recebedor (infoAdicionais, solicitacaoPagador).
     * <p>
     * UX cap. 07: só caracteres ou HTML sem conteúdo dinâmico. Neste lab: remova blocos
     * {@code <script>...</script>} e {@code <style>...</style>} inteiros (com o conteúdo) e depois
     * qualquer outra tag, preservando o texto. Faça trim. Nulo devolve nulo.
     */
    public String sanitizarTexto(String texto) {
        throw new RegraNaoImplementada("R23", "RegrasExibicao.sanitizarTexto");
    }

    /**
     * R24: a resposta do pagador à "solicitacaoPagador" é aceitável?
     * <p>
     * Em pagamento por QR não existe campo "Descrição" livre. O pagador só escreve algo se o
     * recebedor pediu (solicitacaoPagador preenchida), e essa resposta vai no RmtInf da pacs.008,
     * que tem no máximo 140 caracteres. Resposta vazia/nula é sempre aceita.
     * Caso contrário: CAMPO_NAO_PERMITIDO.
     */
    public Optional<Recusa> validarRespostaAoPagador(String solicitacaoPagador, String resposta) {
        throw new RegraNaoImplementada("R24", "RegrasExibicao.validarRespostaAoPagador");
    }

    /**
     * R25: o pagamento vai usar crédito? Então avise ANTES de confirmar.
     * <p>
     * UX caps. 06 e 07 (item 05): se o saldo não cobre o valor e a diferença sai de uma linha de
     * crédito (ex.: cheque empresarial), a mensagem obrigatória mostra o valor que será usado e
     * o nome da linha. Ex.: "Atenção: para concluir a transação, será utilizado R$ 50,00 do
     * Cheque Empresarial". Se o saldo cobre, ou se nem saldo + limite cobrem (aí é saldo
     * insuficiente, outra regra), devolva vazio.
     */
    public Optional<String> avisoUsoDeCredito(ContaPagador conta, BigDecimal valor) {
        throw new RegraNaoImplementada("R25", "RegrasExibicao.avisoUsoDeCredito");
    }
}
