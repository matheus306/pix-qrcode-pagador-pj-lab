package br.lab.pix.simuladores;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static br.lab.pix.simuladores.DictSimulado.*;

/** GET /simulador/qrcodes: todos os QR Codes de teste, com o que deve acontecer em cada um. */
@RestController
class CatalogoDeQrCodes {

    record Qr(String cenario, String descricao, String esperado, String brCode) {
    }

    private static final String HOST = "pix.recebedor-a.local";

    @GetMapping("/simulador/qrcodes")
    List<Qr> listar() {
        List<Qr> l = new ArrayList<>();
        // QR estáticos
        l.add(new Qr("estatico-sem-valor", "Estático sem valor, chave aleatória, sem txid",
                "Pagador digita o valor (R16). Recebedor PJ sem nome fantasia (R22).",
                BrCode.estatico(CHAVE_ALEATORIA, null, null, "LOJA EXEMPLO")));
        l.add(new Qr("estatico-com-valor", "Estático com valor 150.00 e txid PEDIDO2026X1",
                "Valor travado (R16), txid vai na pacs.008 (R28).",
                BrCode.estatico(CHAVE_FORNECEDOR_A, "150.00", "PEDIDO2026X1", "FORNECEDOR A")));
        l.add(new Qr("estatico-pf", "Estático para pessoa física", "CPF mascarado na tela (R22).",
                BrCode.estatico(CHAVE_PF, "35.00", null, "MARIA SOUZA")));
        l.add(new Qr("estatico-fraude", "Chave com marcação de fundada suspeita de fraude",
                "Recusa FRAUDE com a mensagem obrigatória (R21).",
                BrCode.estatico(CHAVE_FRAUDE, "99.90", null, "PROMOCOES")));
        l.add(new Qr("estatico-chave-inexistente", "Chave que não existe no DICT", "Recusa CHAVE_INEXISTENTE (R21).",
                BrCode.estatico("nao-existe@lugar-nenhum.com.br", "10.00", null, "NINGUEM")));
        l.add(new Qr("estatico-dict-fora", "DICT fora do ar para esta chave", "Recusa DICT_INDISPONIVEL (R21).",
                BrCode.estatico(CHAVE_DICT_FORA, "10.00", null, "FORNECEDOR D")));
        String bom = BrCode.estatico(CHAVE_FORNECEDOR_A, "150.00", null, "FORNECEDOR A");
        l.add(new Qr("estatico-crc-errado", "QR adulterado (CRC não confere)", "Recusa QR_INVALIDO (R01).",
                bom.substring(0, bom.length() - 1) + (bom.endsWith("0") ? "1" : "0")));
        l.add(new Qr("estatico-txid-invalido", "Txid com hífen", "Recusa QR_INVALIDO (R04).",
                BrCode.estatico(CHAVE_FORNECEDOR_A, "10.00", "PEDIDO-123", "FORNECEDOR A")));
        l.add(new Qr("estatico-sem-saldo", "Use com a conta empresa-sem-saldo", "RECUSADO_INTERNO com mensagem de saldo (R30).",
                BrCode.estatico(CHAVE_FORNECEDOR_A, "500.00", null, "FORNECEDOR A")));

        // QR dinâmicos: cobrança imediata
        dinamico(l, "cob-ativa", "Cobrança imediata ATIVA de 250.00, pede a NF ao pagador",
                "Fluxo feliz até LIQUIDADO, com comprovante (R37) e notificação (R36).");
        dinamico(l, "cob-valor-livre", "Valor editável com sugestão 0.00", "Pagador precisa digitar um valor (R16).");
        dinamico(l, "cob-troco", "Compra de 80.00 com troco editável", "Purpose GSCB, total = compra + troco (R17).");
        dinamico(l, "cob-saque", "Saque fixo de 100.00", "Purpose OTHR (R17).");
        dinamico(l, "cob-credito", "Cobrança de 12.000,00 (saldo da empresa-a é 10.000,00)",
                "Aviso de uso do Cheque Empresarial antes de confirmar (R25).");
        dinamico(l, "cob-concluida", "Cobrança já paga", "Recusa COBRANCA_NAO_PAGAVEL (R12).");
        dinamico(l, "cob-removida", "Cobrança removida pelo recebedor", "Recusa COBRANCA_NAO_PAGAVEL (R12).");
        dinamico(l, "cob-expirada", "Criada há 2h com expiração de 1h", "Recusa QR_EXPIRADO (R13).");
        dinamico(l, "cob-410", "Recebedor responde 410 Gone", "Recusa COBRANCA_NAO_PAGAVEL (R11).");
        dinamico(l, "cob-indisponivel", "Recebedor fora do ar (503)", "Recusa PAYLOAD_INDISPONIVEL (R11).");
        dinamico(l, "cob-assinatura-adulterada", "Valor trocado depois da assinatura", "Recusa ASSINATURA_INVALIDA (R10).");
        dinamico(l, "cob-alg-hs256", "Assinado com HMAC (HS256)", "Recusa ASSINATURA_INVALIDA (R09).");
        dinamico(l, "cob-jku-outro-host", "jku aponta para outro site", "Recusa ASSINATURA_INVALIDA (R10).");
        dinamico(l, "cob-sem-kid", "Cabeçalho JWS sem kid", "Recusa ASSINATURA_INVALIDA (R10).");
        dinamico(l, "cob-txid-curto", "Txid com 8 caracteres", "Recusa QR_INVALIDO (R14).");
        dinamico(l, "cob-script", "infoAdicionais com <script>", "Texto limpo na tela (R23).");
        dinamico(l, "cob-fraude", "Payload com chave marcada por fraude", "Recusa FRAUDE (R21).");
        dinamico(l, "cob-conta-encerrada", "SPI rejeita: conta do recebedor inexistente (AC03)",
                "REJEITADO, saldo estornado, mensagem específica (R30).");
        dinamico(l, "cob-spi-lento", "SPI não responde, mas a ordem liquidou",
                "ENVIADO_SPI -> EM_VERIFICACAO -> consulta -> LIQUIDADO (R29). Nunca reenviar!");
        dinamico(l, "cob-spi-sumiu", "SPI não responde e a ordem não existe lá",
                "EM_VERIFICACAO -> consultas -> REJEITADO com estorno (R29).");

        // Segurança do site
        l.add(qrDinamico("site-homologacao", "qrcode-h.recebedor-a.local/cob-ativa", "Site de homologação (-h)",
                "Recusa QR_HOMOLOGACAO em produção (R07)."));
        l.add(qrDinamico("site-revogado", "pix.recebedor-revogado.local/cob-ativa", "Certificado revogado",
                "Recusa CERTIFICADO_INVALIDO (R08)."));
        l.add(qrDinamico("site-nao-cadastrado", "pix.nao-cadastrado.local/cob-ativa", "Certificado fora da lista CERTQRC",
                "Recusa CERTIFICADO_INVALIDO (R08)."));
        l.add(qrDinamico("site-tls-antigo", "pix.tls-antigo.local/cob-ativa", "Site com TLS 1.1",
                "Recusa CERTIFICADO_INVALIDO (R08)."));
        l.add(qrDinamico("site-wildcard", "pix.wildcard.local/cob-ativa", "Certificado com wildcard",
                "Recusa CERTIFICADO_INVALIDO (R08)."));
        l.add(qrDinamico("site-sem-ev", "pix.sem-ev.local/cob-ativa", "Certificado que não é EV",
                "Recusa CERTIFICADO_INVALIDO (R08)."));
        l.add(qrDinamico("url-com-protocolo", "https://" + HOST + "/cob-ativa", "URL com https:// dentro do QR",
                "Recusa QR_INVALIDO (R05)."));

        // QR dinâmicos: cobrança com vencimento
        dinamico(l, "cobv/cobv-a-vencer", "Boleto em Pix: 1.000,00 vence em 5 dias, desconto de 50,00 até o vencimento",
                "Pagar hoje (950,00) ou agendar (R31, R32). No dia: janela 1 (R34).");
        dinamico(l, "cobv/cobv-em-atraso", "Venceu há 3 dias, validade de 30 dias",
                "Multa e juros calculados pelo recebedor; o pagador usa valor.final (R20).");
        dinamico(l, "cobv/cobv-vencida", "Venceu há 10 dias, sem validade", "Recusa QR_VENCIDO (R19).");
        dinamico(l, "cobv/cobv-incoerente", "valor.final não bate com original - desconto", "Recusa VALOR_INVALIDO (R20).");
        dinamico(l, "cobv/cobv-paga-no-boleto", "Cobrança híbrida já paga no boleto",
                "Recusa COBRANCA_NAO_PAGAVEL (R12, status novo de 2027).");
        dinamico(l, "cobv/cobv-acima-do-limite", "30.000,00 com vencimento futuro",
                "Agendar estoura o limite de Pix Agendado de 20.000,00 (R32).");
        return l;
    }

    private static void dinamico(List<Qr> l, String token, String descricao, String esperado) {
        l.add(qrDinamico(token.replace("cobv/", ""), HOST + "/" + token, descricao, esperado));
    }

    private static Qr qrDinamico(String cenario, String url, String descricao, String esperado) {
        return new Qr(cenario, descricao, esperado, BrCode.dinamico(url, "RECEBEDOR"));
    }
}
