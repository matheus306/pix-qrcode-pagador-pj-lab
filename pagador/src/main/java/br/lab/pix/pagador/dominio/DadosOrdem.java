package br.lab.pix.pagador.dominio;

/** Tudo o que o pagador juntou até a confirmação, usado para montar a pacs.008. */
public record DadosOrdem(
        TipoQr tipoQr,
        String txidDoQr,
        String txidDoPayload,
        String chave,
        ValoresDaOrdem valores,
        String respostaPagador,
        String endToEndId,
        ContaPagador pagador,
        DadosDict recebedor) {
}
