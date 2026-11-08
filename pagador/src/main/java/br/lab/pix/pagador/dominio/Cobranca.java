package br.lab.pix.pagador.dominio;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Payload JSON de uma cobrança, devolvido (assinado em JWS) pela URL do QR dinâmico.
 * Manual de Padrões, seções 2.7.1.1 (imediata) e 2.7.1.2 (com vencimento).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Cobranca(
        Integer revisao,
        Calendario calendario,
        Pessoa devedor,
        Pessoa recebedor,
        Valor valor,
        String chave,
        String txid,
        String solicitacaoPagador,
        List<InfoAdicional> infoAdicionais,
        String status) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Calendario(
            Instant criacao,
            Instant apresentacao,
            Integer expiracao,
            LocalDate dataDeVencimento,
            Integer validadeAposVencimento) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pessoa(
            String cpf,
            String cnpj,
            String nome,
            String nomeFantasia,
            String logradouro,
            String cidade,
            String uf,
            String cep) {
    }

    /** Todos os valores monetários são texto no formato \d{1,10}\.\d{2}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Valor(
            String original,
            Integer modalidadeAlteracao,
            Retirada retirada,
            String abatimento,
            String desconto,
            String juros,
            String multa,
            @JsonProperty("final") String valorFinal) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Retirada(DetalheRetirada saque, DetalheRetirada troco) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DetalheRetirada(
            String valor,
            Integer modalidadeAlteracao,
            String modalidadeAgente,
            String prestadorDoServicoDeSaque) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InfoAdicional(String nome, String valor) {
    }
}
