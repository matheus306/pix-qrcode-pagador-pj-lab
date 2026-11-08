package br.lab.pix.pagador.regras;

import br.lab.pix.pagador.dominio.Comprovante;
import br.lab.pix.pagador.dominio.DadosComprovante;

/**
 * Bloco 7: comprovantes.
 * Fonte: UX cap. 06 (item 13), cap. 07 (item 14) e cap. 12 (agendamento).
 */
public class RegrasComprovante {

    /**
     * R37: montar o comprovante (de pagamento ou de agendamento).
     * <p>
     * Pagamento (título "Pix confirmado"), no mínimo:
     * <ul>
     *   <li>pagador: nome, CNPJ e PSP;</li>
     *   <li>recebedor: nome (nome fantasia se PJ e existir), CPF/CNPJ e PSP;</li>
     *   <li>"Identificador": o TxId, quando houver;</li>
     *   <li>valor e "ID/Transação" (o EndToEndId);</li>
     *   <li>data e hora da liquidação em horário de Brasília, com segundos (dd/MM/yyyy HH:mm:ss);</li>
     *   <li>os demais dados do payload (ex.: juros, multa, valor original), se houver.</li>
     * </ul>
     * É PROIBIDO exibir a chave Pix, a agência e a conta do recebedor.
     * <p>
     * Agendamento: o título deve conter "Agendamento Pix" (para não confundir com comprovante
     * de pagamento), mais a data agendada, a data em que foi feito e o aviso destacado de que
     * pode ser cancelado até as 23:59 do dia anterior.
     */
    public Comprovante montarComprovante(DadosComprovante dados) {
        throw new RegraNaoImplementada("R37", "RegrasComprovante.montarComprovante");
    }
}
