package br.lab.pix.pagador.infra;

import br.lab.pix.pagador.dominio.EstadoPagamento;
import br.lab.pix.pagador.dominio.Pagamento;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/** Pagamentos no DynamoDB (LocalStack): chave "id" e o pagamento inteiro em JSON. */
@Component
public class RepositorioPagamentos {

    private final DynamoDbClient dynamo;
    private final ObjectMapper json;
    private final String tabela;

    public RepositorioPagamentos(DynamoDbClient dynamo, ObjectMapper json, @Value("${aws.tabela-pagamentos}") String tabela) {
        this.dynamo = dynamo;
        this.json = json;
        this.tabela = tabela;
    }

    public void salvar(Pagamento p) {
        try {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", AttributeValue.fromS(p.id));
            item.put("estado", AttributeValue.fromS(p.estado.name()));
            item.put("json", AttributeValue.fromS(json.writeValueAsString(p)));
            dynamo.putItem(b -> b.tableName(tabela).item(item));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public Optional<Pagamento> buscar(String id) {
        var resp = dynamo.getItem(b -> b.tableName(tabela).key(Map.of("id", AttributeValue.fromS(id))));
        return resp.hasItem() ? Optional.of(ler(resp.item())) : Optional.empty();
    }

    public List<Pagamento> todos() {
        return dynamo.scanPaginator(b -> b.tableName(tabela)).items().stream().map(this::ler).toList();
    }

    public List<Pagamento> filtrar(Predicate<Pagamento> filtro) {
        return todos().stream().filter(filtro).toList();
    }

    public List<Pagamento> noEstado(EstadoPagamento estado) {
        return filtrar(p -> p.estado == estado);
    }

    public Optional<Pagamento> porEndToEndId(String e2e) {
        return filtrar(p -> e2e.equals(p.endToEndId)).stream().findFirst();
    }

    private Pagamento ler(Map<String, AttributeValue> item) {
        try {
            return json.readValue(item.get("json").s(), Pagamento.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
