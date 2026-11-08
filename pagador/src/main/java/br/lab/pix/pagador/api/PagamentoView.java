package br.lab.pix.pagador.api;

import br.lab.pix.pagador.dominio.Comprovante;
import br.lab.pix.pagador.dominio.DestinatarioExibido;
import br.lab.pix.pagador.dominio.EstadoPagamento;
import br.lab.pix.pagador.dominio.Notificacao;
import br.lab.pix.pagador.dominio.Pacs008;
import br.lab.pix.pagador.dominio.Pagamento;
import br.lab.pix.pagador.dominio.TipoCobranca;
import br.lab.pix.pagador.dominio.TipoQr;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * O que a "tela" recebe. Repare que os dados do DICT (agência, conta) não estão aqui:
 * só o destinatário montado pela regra R22. A pacs.008 aparece para você estudar o mapeamento.
 */
public record PagamentoView(
        String id,
        EstadoPagamento estado,
        TipoQr tipoQr,
        TipoCobranca tipoCobranca,
        DestinatarioExibido destinatario,
        BigDecimal valorPrevisto,
        boolean valorEditavel,
        String solicitacaoPagador,
        Map<String, String> dadosDaCobranca,
        String avisoCredito,
        LocalDate dataLimiteCobv,
        LocalDate dataAgendada,
        BigDecimal valorAgendado,
        String mensagemAoUsuario,
        Comprovante comprovante,
        Comprovante comprovanteAgendamento,
        List<Notificacao> notificacoes,
        Pacs008 pacs008,
        List<String> historico) {

    static PagamentoView de(Pagamento p) {
        boolean editavel = p.valorDaCobranca != null && p.valorDaCobranca.modalidadeAlteracao() != null
                && p.valorDaCobranca.modalidadeAlteracao() == 1;
        return new PagamentoView(p.id, p.estado, p.tipoQr, p.tipoCobranca, p.destinatario, p.valorPrevisto, editavel,
                p.solicitacaoPagador, p.dadosDaCobranca, p.avisoCredito, p.dataLimiteCobv, p.dataAgendada,
                p.valorAgendado, p.mensagemAoUsuario, p.comprovante, p.comprovanteAgendamento, p.notificacoes,
                p.pacs008, p.historico);
    }
}
