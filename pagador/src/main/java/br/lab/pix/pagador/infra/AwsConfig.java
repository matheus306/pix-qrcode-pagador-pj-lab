package br.lab.pix.pagador.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

/** Clientes AWS apontando para a LocalStack. */
@Configuration
public class AwsConfig {

    private final URI endpoint;
    private final Region regiao;
    private final StaticCredentialsProvider credenciais =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));

    public AwsConfig(@Value("${aws.endpoint}") String endpoint, @Value("${aws.region}") String regiao) {
        this.endpoint = URI.create(endpoint);
        this.regiao = Region.of(regiao);
    }

    @Bean
    SqsClient sqs() {
        return SqsClient.builder().endpointOverride(endpoint).region(regiao).credentialsProvider(credenciais).build();
    }

    @Bean
    DynamoDbClient dynamo() {
        return DynamoDbClient.builder().endpointOverride(endpoint).region(regiao).credentialsProvider(credenciais).build();
    }

    @Bean
    S3Client s3() {
        return S3Client.builder().endpointOverride(endpoint).region(regiao).credentialsProvider(credenciais)
                .forcePathStyle(true).build();
    }
}
