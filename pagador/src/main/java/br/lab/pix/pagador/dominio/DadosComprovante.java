package br.lab.pix.pagador.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * Insumos do comprovante. Inclui de propósito dados que NÃO podem aparecer
 * (chave, agência e conta do recebedor estão dentro de {@code recebedor}).
 */
public record DadosComprovante(
        boolean agendamento,
        String pagadorNome,
        String pagadorCnpj,
        String pagadorPsp,
        DadosDict recebedor,
        String txid,
        BigDecimal valor,
        String endToEndId,
        Instant dataHoraLiquidacao,
        LocalDate dataAgendada,
        LocalDate dataDoAgendamento,
        Map<String, String> dadosDoPayload) {
}
