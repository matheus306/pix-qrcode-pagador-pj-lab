package br.lab.pix.simuladores;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static br.lab.pix.simuladores.DictSimulado.*;

/**
 * Sites de QR Code dos PSPs recebedores. O mesmo container responde por vários nomes
 * (aliases no docker compose); o cabeçalho Host diz qual "site" foi acessado.
 * Cada token da URL é um cenário (ver /simulador/qrcodes).
 */
@RestController
class RecebedorSimulado {

    static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
    /** Os vencimentos são relativos ao dia em que o simulador subiu. */
    static final LocalDate HOJE_NA_SUBIDA = LocalDate.now(BRASILIA);

    private final Assinador assinador;
    private final ObjectMapper json;
    private final Map<String, String> statusAlterado = new ConcurrentHashMap<>();
    private final Map<String, String> originalAlterado = new ConcurrentHashMap<>();

    RecebedorSimulado(Assinador assinador, ObjectMapper json) {
        this.assinador = assinador;
        this.json = json;
    }

    @GetMapping(value = "/jwks", produces = "application/json")
    String jwks() {
        return assinador.jwkSetPublico();
    }

    @GetMapping({"/{token}", "/cob/{token}"})
    ResponseEntity<String> cobrancaImediata(@PathVariable String token, @RequestHeader("Host") String hostHeader) {
        return responder(hostHeader, token, () -> cob(token));
    }

    @GetMapping("/cobv/{token}")
    ResponseEntity<String> cobrancaComVencimento(@PathVariable String token, @RequestHeader("Host") String hostHeader,
                                                 @RequestParam(name = "DPP", required = false) LocalDate dpp,
                                                 @RequestParam(name = "codMun", required = false) String codMun) {
        return responder(hostHeader, token, () -> cobv(token, dpp));
    }

    /** Cobrança híbrida: o boleto foi pago, então o Pix não pode mais ser pago. */
    @PostMapping("/simulador/cobrancas/{token}/pagar-no-boleto")
    Map<String, String> pagarNoBoleto(@PathVariable String token) {
        statusAlterado.put(token, "CONCLUÍDA_POR_OUTRO_ARRANJO");
        return Map.of("token", token, "status", "CONCLUÍDA_POR_OUTRO_ARRANJO");
    }

    /** O recebedor revisou a cobrança e mudou o valor original. */
    @PostMapping("/simulador/cobrancas/{token}/alterar-valor")
    Map<String, String> alterarValor(@PathVariable String token) {
        originalAlterado.put(token, "1100.00");
        return Map.of("token", token, "original", "1100.00");
    }

    @PostMapping("/simulador/cobrancas/reiniciar")
    void reiniciar() {
        statusAlterado.clear();
        originalAlterado.clear();
    }

    // ------------------------------------------------------------------ montagem da resposta

    private record Cenario(int status, Map<String, Object> payload, Assinador.Modo modo) {
    }

    private ResponseEntity<String> responder(String hostHeader, String token, java.util.function.Supplier<Cenario> c) {
        String host = hostHeader.replaceFirst(":\\d+$", "");
        BcbArqSimulado.Site site = BcbArqSimulado.SITES.getOrDefault(host, new BcbArqSimulado.Site("cert-desconhecido", "TLSv1.2"));
        Cenario cenario = c.get();
        var resposta = ResponseEntity.status(cenario.status())
                .header("X-Simulacao-Tls", site.tls())
                .header("X-Simulacao-Certificado", site.certificado());
        if (cenario.payload() == null) {
            return resposta.build();
        }
        try {
            String jws = assinador.assinar(json.writeValueAsString(cenario.payload()), host, cenario.modo());
            return resposta.header("Content-Type", "application/jose").body(jws);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Cenario cob(String token) {
        Instant agora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("revisao", 0);
        p.put("calendario", Map.of("criacao", agora.minus(10, ChronoUnit.MINUTES).toString(),
                "apresentacao", agora.toString(), "expiracao", 3600));
        p.put("devedor", Map.of("cnpj", "11222333000181", "nome", "EMPRESA PAGADORA A LTDA"));
        p.put("valor", Map.of("original", "250.00"));
        p.put("chave", CHAVE_FORNECEDOR_A);
        p.put("txid", txid(token));
        p.put("solicitacaoPagador", "Informe o número da nota fiscal");
        p.put("infoAdicionais", List.of(Map.of("nome", "Pedido", "valor", "4521")));
        p.put("status", statusAlterado.getOrDefault(token, "ATIVA"));
        Assinador.Modo modo = Assinador.Modo.NORMAL;

        switch (token) {
            case "cob-ativa" -> { }
            case "cob-valor-livre" -> {
                p.put("valor", Map.of("original", "0.00", "modalidadeAlteracao", 1));
                p.remove("solicitacaoPagador");
            }
            case "cob-troco" -> p.put("valor", Map.of("original", "80.00", "retirada", Map.of("troco",
                    Map.of("valor", "0.00", "modalidadeAlteracao", 1, "modalidadeAgente", "AGTEC",
                            "prestadorDoServicoDeSaque", "11111111"))));
            case "cob-saque" -> p.put("valor", Map.of("original", "0.00", "retirada", Map.of("saque",
                    Map.of("valor", "100.00", "modalidadeAlteracao", 0, "modalidadeAgente", "AGTEC",
                            "prestadorDoServicoDeSaque", "11111111"))));
            case "cob-credito" -> p.put("valor", Map.of("original", "12000.00"));
            case "cob-concluida" -> p.put("status", "CONCLUIDA");
            case "cob-removida" -> p.put("status", "REMOVIDA_PELO_USUARIO_RECEBEDOR");
            case "cob-expirada" -> p.put("calendario", Map.of("criacao", agora.minus(2, ChronoUnit.HOURS).toString(),
                    "apresentacao", agora.toString(), "expiracao", 3600));
            case "cob-410" -> { return new Cenario(410, null, modo); }
            case "cob-indisponivel" -> { return new Cenario(503, null, modo); }
            case "cob-assinatura-adulterada" -> modo = Assinador.Modo.ADULTERADO;
            case "cob-alg-hs256" -> modo = Assinador.Modo.HS256;
            case "cob-jku-outro-host" -> modo = Assinador.Modo.JKU_OUTRO_HOST;
            case "cob-sem-kid" -> modo = Assinador.Modo.SEM_KID;
            case "cob-txid-curto" -> p.put("txid", "CURTO123");
            case "cob-script" -> p.put("infoAdicionais", List.of(
                    Map.of("nome", "Pedido", "valor", "<script>alert('golpe')</script>4521"),
                    Map.of("nome", "<b>Loja</b>", "valor", "Matriz")));
            case "cob-fraude" -> p.put("chave", CHAVE_FRAUDE);
            case "cob-conta-encerrada" -> {
                p.put("chave", CHAVE_CONTA_ENCERRADA);
                p.put("valor", Map.of("original", "50.00"));
            }
            case "cob-spi-lento" -> {
                p.put("chave", CHAVE_SPI_LENTO);
                p.put("valor", Map.of("original", "75.00"));
            }
            case "cob-spi-sumiu" -> {
                p.put("chave", CHAVE_SPI_SUMIU);
                p.put("valor", Map.of("original", "60.00"));
            }
            default -> { return new Cenario(404, null, modo); }
        }
        return new Cenario(200, p, modo);
    }

    private Cenario cobv(String token, LocalDate dppInformada) {
        LocalDate vencimento = HOJE_NA_SUBIDA.plusDays(5);
        int validade = 0;
        String original = originalAlterado.getOrDefault(token, "1000.00");
        boolean incoerente = false;
        switch (token) {
            case "cobv-a-vencer", "cobv-paga-no-boleto" -> { }
            case "cobv-vencida" -> vencimento = HOJE_NA_SUBIDA.minusDays(10);
            case "cobv-em-atraso" -> {
                vencimento = HOJE_NA_SUBIDA.minusDays(3);
                validade = 30;
            }
            case "cobv-incoerente" -> incoerente = true;
            case "cobv-acima-do-limite" -> original = "30000.00";
            default -> { return new Cenario(404, null, Assinador.Modo.NORMAL); }
        }
        LocalDate hoje = LocalDate.now(BRASILIA);
        // Sem DPP: até o vencimento, assume o vencimento; depois, a data da consulta (Manual, seção 2.7.1.2).
        LocalDate dpp = dppInformada != null ? dppInformada : (hoje.isAfter(vencimento) ? hoje : vencimento);

        BigDecimal valorOriginal = new BigDecimal(original);
        Map<String, Object> valor = new LinkedHashMap<>();
        valor.put("original", original);
        BigDecimal fim;
        if (!dpp.isAfter(vencimento)) {
            BigDecimal desconto = new BigDecimal("50.00");
            valor.put("desconto", desconto.toPlainString());
            fim = valorOriginal.subtract(desconto);
        } else {
            long dias = ChronoUnit.DAYS.between(vencimento, dpp);
            BigDecimal multa = valorOriginal.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal juros = valorOriginal.multiply(new BigDecimal("0.01")).multiply(BigDecimal.valueOf(dias))
                    .divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_EVEN);
            valor.put("multa", multa.toPlainString());
            valor.put("juros", juros.toPlainString());
            fim = valorOriginal.add(multa).add(juros);
        }
        valor.put("final", incoerente ? "999.99" : fim.setScale(2, RoundingMode.HALF_EVEN).toPlainString());

        Instant agora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("revisao", originalAlterado.containsKey(token) ? 1 : 0);
        p.put("calendario", Map.of("criacao", agora.minus(1, ChronoUnit.DAYS).toString(), "apresentacao", agora.toString(),
                "dataDeVencimento", vencimento.toString(), "validadeAposVencimento", validade));
        p.put("devedor", Map.of("cnpj", "11222333000181", "nome", "EMPRESA PAGADORA A LTDA"));
        p.put("recebedor", Map.of("cnpj", "33444555000172", "nome", "FORNECEDOR A COMERCIO LTDA",
                "nomeFantasia", "Fornecedor A", "logradouro", "Rua das Flores, 100", "cidade", "São Paulo",
                "uf", "SP", "cep", "01001000"));
        p.put("valor", valor);
        p.put("chave", CHAVE_FORNECEDOR_A);
        p.put("txid", txid(token));
        p.put("infoAdicionais", List.of(Map.of("nome", "Regra de cálculo",
                "valor", "Desconto de R$ 50,00 até o vencimento. Após: multa de 2% e juros de 1% ao mês.")));
        String status = "cobv-paga-no-boleto".equals(token) ? "CONCLUÍDA_POR_OUTRO_ARRANJO" : "ATIVA";
        p.put("status", statusAlterado.getOrDefault(token, status));
        return new Cenario(200, p, Assinador.Modo.NORMAL);
    }

    static String txid(String token) {
        String base = "LAB" + token.replaceAll("[^a-zA-Z0-9]", "").toUpperCase() + "0".repeat(35);
        return base.substring(0, 30);
    }
}
