package br.lab.pix.pagador.dominio;

import java.math.BigDecimal;

/**
 * Valores que vão para a pacs.008 (Manual de Padrões, Anexo V, seção 2.2).
 *
 * @param total             InterbankSettlementAmount (IntrBkSttlmAmt)
 * @param purpose           Purp.Cd: IPAY (compra/transferência), GSCB (troco) ou OTHR (saque)
 * @param valorCompra       valor da compra detalhado (só no troco, Rsn = VLCP)
 * @param valorRetirada     valor do saque ou do troco (Rsn = VLDN)
 * @param modalidadeAgente  AGTEC, AGTOT ou AGFSS (só saque/troco)
 * @param prestadorServicoSaque ISPB do facilitador de serviço de saque (só saque/troco)
 */
public record ValoresDaOrdem(
        BigDecimal total,
        String purpose,
        BigDecimal valorCompra,
        BigDecimal valorRetirada,
        String modalidadeAgente,
        String prestadorServicoSaque) {
}
