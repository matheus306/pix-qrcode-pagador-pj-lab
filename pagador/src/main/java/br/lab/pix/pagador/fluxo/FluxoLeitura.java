package br.lab.pix.pagador.fluxo;

import br.lab.pix.pagador.dominio.CalendarioDiasUteis;
import br.lab.pix.pagador.dominio.Cobranca;
import br.lab.pix.pagador.dominio.CodigoRecusa;
import br.lab.pix.pagador.dominio.ContaPagador;
import br.lab.pix.pagador.dominio.EstadoPagamento;
import br.lab.pix.pagador.dominio.Pagamento;
import br.lab.pix.pagador.dominio.RespostaDict;
import br.lab.pix.pagador.dominio.TipoCobranca;
import br.lab.pix.pagador.dominio.TipoQr;
import br.lab.pix.pagador.emv.EmvDados;
import br.lab.pix.pagador.emv.EmvParser;
import br.lab.pix.pagador.infra.ClienteDict;
import br.lab.pix.pagador.infra.Contas;
import br.lab.pix.pagador.infra.Relogio;
import br.lab.pix.pagador.infra.RepositorioPagamentos;
import br.lab.pix.pagador.regras.RegrasCobranca;
import br.lab.pix.pagador.regras.RegrasExibicao;
import br.lab.pix.pagador.regras.RegrasLeituraQr;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static br.lab.pix.pagador.fluxo.FalhaDeFluxo.exigir;

/**
 * Máquina M1: da leitura do QR até PRONTO_PARA_CONFIRMACAO (ou recusa).
 * Os números Rxx nos comentários apontam a regra que você implementa em cada passo.
 */
@Service
public class FluxoLeitura {

    private static final String QR_INVALIDO = "Transação não concluída. QR Code inválido.";

    private final RegrasLeituraQr leitura;
    private final RegrasCobranca cobranca;
    private final RegrasExibicao exibicao;
    private final LeitorDePayload leitorDePayload;
    private final ClienteDict dict;
    private final Contas contas;
    private final Relogio relogio;
    private final RepositorioPagamentos repositorio;
    private final CalendarioDiasUteis calendario;

    public FluxoLeitura(RegrasLeituraQr leitura, RegrasCobranca cobranca, RegrasExibicao exibicao,
                        LeitorDePayload leitorDePayload, ClienteDict dict, Contas contas, Relogio relogio,
                        RepositorioPagamentos repositorio, CalendarioDiasUteis calendario) {
        this.leitura = leitura;
        this.cobranca = cobranca;
        this.exibicao = exibicao;
        this.leitorDePayload = leitorDePayload;
        this.dict = dict;
        this.contas = contas;
        this.relogio = relogio;
        this.repositorio = repositorio;
        this.calendario = calendario;
    }

    public Pagamento ler(String idConta, String brCode) {
        ContaPagador conta = contas.buscar(idConta);
        Pagamento p = new Pagamento();
        p.id = UUID.randomUUID().toString();
        p.idConta = idConta;
        p.brCode = brCode;
        p.criadoEm = relogio.agora();

        EmvDados emv;
        try {
            emv = EmvParser.parse(brCode);
        } catch (IllegalArgumentException e) {
            throw new FalhaDeFluxo(CodigoRecusa.QR_INVALIDO, QR_INVALIDO);
        }
        if (!leitura.crcValido(brCode)) {                                                      // R01
            throw new FalhaDeFluxo(CodigoRecusa.QR_INVALIDO, QR_INVALIDO);
        }
        String idTemplate = leitura.idTemplatePix(emv)                                          // R02
                .orElseThrow(() -> new FalhaDeFluxo(CodigoRecusa.QR_INVALIDO, QR_INVALIDO));
        p.tipoQr = leitura.classificar(emv, idTemplate);                                        // R03

        switch (p.tipoQr) {
            case ESTATICO -> lerEstatico(p, emv, idTemplate);
            case DINAMICO -> lerDinamico(p, emv, idTemplate, conta);
            case COMPOSTO -> throw new FalhaDeFluxo(CodigoRecusa.NAO_SUPORTADO,
                    "QR Code composto (Pix Automático) está fora do escopo deste laboratório.");
            default -> throw new FalhaDeFluxo(CodigoRecusa.QR_INVALIDO, QR_INVALIDO);
        }

        RespostaDict respostaDict = dict.consultar(p.chave);
        exigir(exibicao.interpretarConsultaDict(respostaDict));                                 // R21
        p.recebedor = respostaDict.dados();
        p.destinatario = exibicao.montarDestinatario(p.recebedor);                              // R22
        if (p.valorPrevisto != null) {
            p.avisoCredito = exibicao.avisoUsoDeCredito(conta, p.valorPrevisto).orElse(null);  // R25
        }
        p.estado = EstadoPagamento.PRONTO_PARA_CONFIRMACAO;
        p.historico.add(relogio.agoraLocalBrasilia() + "  (leitura) -> PRONTO_PARA_CONFIRMACAO");
        repositorio.salvar(p);
        return p;
    }

    private void lerEstatico(Pagamento p, EmvDados emv, String idTemplate) {
        Map<String, String> template = emv.template(idTemplate);
        p.chave = template.get("01");
        p.txidDoQr = emv.template("62").get("05");
        exigir(leitura.validarTxidEstatico(p.txidDoQr));                                        // R04

        String valor = emv.valor("54");
        if (valor != null && !cobranca.valorNoFormato(valor)) {                                 // R15
            throw new FalhaDeFluxo(CodigoRecusa.QR_INVALIDO, QR_INVALIDO);
        }
        // Estático sem valor = o pagador digita; com valor = o valor não pode ser alterado (UX cap. 06).
        p.valorDaCobranca = valor == null
                ? new Cobranca.Valor("0.00", 1, null, null, null, null, null, null)
                : new Cobranca.Valor(valor, 0, null, null, null, null, null, null);
        p.valorPrevisto = valor == null ? null : new BigDecimal(valor);
        if (p.txidDoQr != null && !"***".equals(p.txidDoQr)) {
            p.dadosDaCobranca.put("Identificador", p.txidDoQr);
        }
    }

    private void lerDinamico(Pagamento p, EmvDados emv, String idTemplate, ContaPagador conta) {
        p.urlPayload = emv.template(idTemplate).get("25");
        exigir(leitura.validarUrlLocation(p.urlPayload));                                       // R05
        p.tipoCobranca = leitura.tipoCobranca(p.urlPayload);                                    // R06
        if (p.tipoCobranca == TipoCobranca.RECORRENCIA) {
            throw new FalhaDeFluxo(CodigoRecusa.NAO_SUPORTADO, "Recorrência fica fora deste laboratório.");
        }
        boolean comVencimento = p.tipoCobranca == TipoCobranca.COM_VENCIMENTO;
        Map<String, String> parametros = comVencimento
                ? cobranca.parametrosConsultaCobv(conta, relogio.hoje())                       // R18
                : Map.of();

        Cobranca cob = leitorDePayload.buscar(p.id, "leitura", p.urlPayload, parametros);      // R07 a R11
        p.cobranca = cob;
        p.chave = cob.chave();
        exigir(cobranca.validarStatus(cob.status()));                                           // R12
        exigir(cobranca.validarTxidDinamico(cob.txid()));                                       // R14

        if (comVencimento) {
            exigir(cobranca.validarCoerenciaValorFinal(cob.valor()));                           // R20
            if (cob.calendario() == null || cob.calendario().dataDeVencimento() == null
                    || cob.calendario().validadeAposVencimento() == null) {
                throw new FalhaDeFluxo(CodigoRecusa.QR_INVALIDO, QR_INVALIDO);
            }
            p.dataLimiteCobv = cobranca.dataLimiteCobv(cob.calendario().dataDeVencimento(),   // R19
                    cob.calendario().validadeAposVencimento(), calendario);
            if (relogio.hoje().isAfter(p.dataLimiteCobv)) {
                throw new FalhaDeFluxo(CodigoRecusa.QR_VENCIDO,
                        "Transação não concluída. QR Code vencido. Esse QR Code não aceita pagamento após o vencimento.");
            }
            p.valorDaCobranca = new Cobranca.Valor(cob.valor().valorFinal(), 0, null, null, null, null, null, null);
            p.valorPrevisto = new BigDecimal(cob.valor().valorFinal());
            preencherDadosCobv(p, cob);
        } else {
            exigir(cobranca.validarExpiracao(cob.calendario().criacao(),                       // R13
                    cob.calendario().expiracao(), relogio.agora()));
            if (!cobranca.valorNoFormato(cob.valor().original())) {                             // R15
                throw new FalhaDeFluxo(CodigoRecusa.QR_INVALIDO, QR_INVALIDO);
            }
            p.valorDaCobranca = cob.valor();
            boolean valorFixo = cob.valor().modalidadeAlteracao() == null || cob.valor().modalidadeAlteracao() == 0;
            if (valorFixo && cob.valor().retirada() == null) {
                p.valorPrevisto = new BigDecimal(cob.valor().original());
            }
        }

        p.solicitacaoPagador = exibicao.sanitizarTexto(cob.solicitacaoPagador());              // R23
        p.dadosDaCobranca.put("Identificador", cob.txid());
        if (cob.devedor() != null && cob.devedor().nome() != null) {
            p.dadosDaCobranca.put("Devedor", cob.devedor().nome() + " "
                    + (cob.devedor().cnpj() != null ? cob.devedor().cnpj() : cob.devedor().cpf()));
        }
        if (cob.infoAdicionais() != null) {
            cob.infoAdicionais().forEach(i ->
                    p.dadosDaCobranca.put(exibicao.sanitizarTexto(i.nome()), exibicao.sanitizarTexto(i.valor())));
        }
    }

    private static void preencherDadosCobv(Pagamento p, Cobranca cob) {
        Cobranca.Valor v = cob.valor();
        p.dadosDaCobranca.put("Vencimento", cob.calendario().dataDeVencimento().toString());
        p.dadosDaCobranca.put("Pagável até", p.dataLimiteCobv.toString());
        if (v.original() != null) p.dadosDaCobranca.put("Valor original", v.original());
        if (v.abatimento() != null) p.dadosDaCobranca.put("Abatimento", v.abatimento());
        if (v.desconto() != null) p.dadosDaCobranca.put("Desconto", v.desconto());
        if (v.juros() != null) p.dadosDaCobranca.put("Juros", v.juros());
        if (v.multa() != null) p.dadosDaCobranca.put("Multa", v.multa());
        p.dadosDaCobranca.put("Valor final", v.valorFinal());
        if (cob.recebedor() != null) {
            var r = cob.recebedor();
            p.dadosDaCobranca.put("Endereço do recebedor",
                    r.logradouro() + " - " + r.cidade() + "/" + r.uf() + " - CEP " + r.cep());
        }
    }

    /** Usado no agendamento: a cobrança precisa ser consultada de novo com a nova DPP. */
    public Cobranca reconsultarCobv(Pagamento p, ContaPagador conta, java.time.LocalDate dpp, String evidencia) {
        Cobranca cob = leitorDePayload.buscar(p.id, evidencia, p.urlPayload,
                cobranca.parametrosConsultaCobv(conta, dpp));                                  // R18
        return cob;
    }

    public boolean ehCobv(Pagamento p) {
        return p.tipoQr == TipoQr.DINAMICO && p.tipoCobranca == TipoCobranca.COM_VENCIMENTO;
    }
}
