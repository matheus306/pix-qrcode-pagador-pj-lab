package br.lab.pix.simuladores;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static br.lab.pix.simuladores.DictSimulado.*;

/**
 * SPI simulado: lê a pacs.008 da fila, decide e devolve uma pacs.002 simplificada
 * (ACSC = liquidada, RJCT = rejeitada) na fila de retorno.
 */
@RestController
class SpiSimulado {

    private static final Logger log = LoggerFactory.getLogger(SpiSimulado.class);

    private final SqsClient sqs;
    private final ObjectMapper json;
    private final String filaEntrada;
    private final String filaRetorno;
    private final Map<String, String> ordens = new ConcurrentHashMap<>();

    SpiSimulado(ObjectMapper json,
                @Value("${aws.endpoint}") String endpoint,
                @Value("${aws.region}") String regiao,
                @Value("${aws.fila-envio}") String filaEntrada,
                @Value("${aws.fila-retorno}") String filaRetorno) {
        this.json = json;
        this.filaEntrada = filaEntrada;
        this.filaRetorno = filaRetorno;
        this.sqs = SqsClient.builder().endpointOverride(URI.create(endpoint)).region(Region.of(regiao))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build();
    }

    @Scheduled(fixedDelay = 300)
    void processar() {
        try {
            String entrada = sqs.getQueueUrl(b -> b.queueName(filaEntrada)).queueUrl();
            String retorno = sqs.getQueueUrl(b -> b.queueName(filaRetorno)).queueUrl();
            for (var msg : sqs.receiveMessage(b -> b.queueUrl(entrada).maxNumberOfMessages(10).waitTimeSeconds(1)).messages()) {
                sqs.deleteMessage(b -> b.queueUrl(entrada).receiptHandle(msg.receiptHandle()));
                JsonNode pacs = json.readTree(msg.body());
                String e2e = pacs.path("endToEndId").asText();
                String chave = pacs.path("chave").asText();
                log.info("pacs.008 recebida: {} chave={} valor={}", e2e, chave, pacs.path("valor").asText());
                switch (chave) {
                    case CHAVE_SPI_LENTO -> ordens.put(e2e, "LIQUIDADO");      // liquidou, mas o retorno se perdeu
                    case CHAVE_SPI_SUMIU -> { }                                // a ordem nunca chegou
                    case CHAVE_CONTA_ENCERRADA -> {
                        ordens.put(e2e, "REJEITADO");
                        enviar(retorno, e2e, "RJCT", "AC03");
                    }
                    default -> {
                        ordens.put(e2e, "LIQUIDADO");
                        enviar(retorno, e2e, "ACSC", null);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("SPI aguardando LocalStack: {}", e.getMessage());
        }
    }

    @GetMapping("/spi/ordens/{e2e}")
    Map<String, String> consultar(@PathVariable String e2e) {
        return Map.of("status", ordens.getOrDefault(e2e, "NAO_ENCONTRADO"));
    }

    private void enviar(String fila, String e2e, String status, String motivo) throws Exception {
        Map<String, String> pacs002 = new java.util.HashMap<>();
        pacs002.put("endToEndId", e2e);
        pacs002.put("status", status);
        if (motivo != null) {
            pacs002.put("motivo", motivo);
        }
        String corpo = json.writeValueAsString(pacs002);
        sqs.sendMessage(b -> b.queueUrl(fila).messageBody(corpo));
    }
}
