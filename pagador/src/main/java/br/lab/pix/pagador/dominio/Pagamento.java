package br.lab.pix.pagador.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Um pagamento por QR Code do começo (leitura) ao fim (liquidado, rejeitado, cancelado...).
 * Campos públicos de propósito: é um laboratório. Persistido como JSON no DynamoDB.
 */
public class Pagamento {

    public String id;
    public String idConta;
    public EstadoPagamento estado;
    public Instant criadoEm;

    // O que foi lido
    public String brCode;
    public TipoQr tipoQr;
    public TipoCobranca tipoCobranca;
    public String urlPayload;
    public String chave;
    public String txidDoQr;
    public Cobranca cobranca;
    public Cobranca.Valor valorDaCobranca;
    public DadosDict recebedor;

    // O que a tela mostra
    public DestinatarioExibido destinatario;
    public BigDecimal valorPrevisto;
    public LocalDate dataLimiteCobv;
    public String solicitacaoPagador;
    public Map<String, String> dadosDaCobranca = new LinkedHashMap<>();
    public String avisoCredito;

    // Confirmação e envio
    public ValoresInformados valoresInformados;
    public String respostaPagador;
    public ValoresDaOrdem valores;
    public String endToEndId;
    public Pacs008 pacs008;
    public Instant enviadoEm;
    public int consultasSpi;
    public BigDecimal valorDebitado;
    public Instant liquidadoEm;

    // Agendamento
    public LocalDate dataAgendada;
    public LocalDate dataDoAgendamento;
    public BigDecimal valorAgendado;
    public int tentativas;

    // Resultado
    public Recusa motivo;
    public String mensagemAoUsuario;
    public Comprovante comprovante;
    public Comprovante comprovanteAgendamento;
    public List<Notificacao> notificacoes = new ArrayList<>();
    public List<String> historico = new ArrayList<>();
}
