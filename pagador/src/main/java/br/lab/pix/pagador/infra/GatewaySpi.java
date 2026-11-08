package br.lab.pix.pagador.infra;

import br.lab.pix.pagador.dominio.Pacs008;
import br.lab.pix.pagador.dominio.StatusConsultaSpi;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;

/**
 * Conversa com o SPI simulado: a pacs.008 vai por uma fila SQS e o retorno (pacs.002 simplificada)
 * volta por outra. A consulta de uma ordem sem resposta é feita por HTTP.
 */
@Component
public class GatewaySpi {

    public record Pacs002(String endToEndId, String status, String motivo) {
    }

    private final SqsClient sqs;
    private final ObjectMapper json;
    private final String filaEnvio;
    private final String filaRetorno;
    private final String spiUrl;

    public GatewaySpi(SqsClient sqs, ObjectMapper json,
                      @Value("${aws.fila-envio}") String filaEnvio,
                      @Value("${aws.fila-retorno}") String filaRetorno,
                      @Value("${pix.spi-url}") String spiUrl) {
        this.sqs = sqs;
        this.json = json;
        this.filaEnvio = filaEnvio;
        this.filaRetorno = filaRetorno;
        this.spiUrl = spiUrl;
    }

    public void enviar(Pacs008 pacs) {
        try {
            String corpo = json.writeValueAsString(pacs);
            sqs.sendMessage(b -> b.queueUrl(url(filaEnvio)).messageBody(corpo));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<Pacs002> receberRetornos() {
        String url = url(filaRetorno);
        List<Message> msgs = sqs.receiveMessage(b -> b.queueUrl(url).maxNumberOfMessages(10).waitTimeSeconds(1)).messages();
        return msgs.stream().map(m -> {
            sqs.deleteMessage(b -> b.queueUrl(url).receiptHandle(m.receiptHandle()));
            try {
                return json.readValue(m.body(), Pacs002.class);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }).toList();
    }

    public StatusConsultaSpi consultar(String endToEndId) {
        Http.Resposta r = Http.get(spiUrl + "/spi/ordens/" + endToEndId);
        if (r.status() != 200) {
            return StatusConsultaSpi.EM_PROCESSAMENTO;
        }
        return StatusConsultaSpi.valueOf(r.corpo().replaceAll("[^A-Z_]", ""));
    }

    private String url(String fila) {
        return sqs.getQueueUrl(b -> b.queueName(fila)).queueUrl();
    }
}
