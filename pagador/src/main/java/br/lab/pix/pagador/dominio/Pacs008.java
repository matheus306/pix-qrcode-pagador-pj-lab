package br.lab.pix.pagador.dominio;

import java.math.BigDecimal;

/**
 * Ordem de pagamento simplificada (os campos da pacs.008 que importam para QR Code).
 * Mapeamento: Manual de Padrões, Anexo V.
 */
public record Pacs008(
        String endToEndId,
        FormaIniciacao formaIniciacao,
        BigDecimal valor,
        String purpose,
        String chave,
        String txId,
        String remittanceInformation,
        String pagadorCnpj,
        String pagadorIspb,
        String pagadorAgencia,
        String pagadorConta,
        String recebedorCpfCnpj,
        String recebedorIspb,
        String recebedorAgencia,
        String recebedorConta,
        String recebedorTipoConta,
        BigDecimal valorCompra,
        BigDecimal valorRetirada,
        String modalidadeAgente,
        String prestadorServicoSaque) {
}
