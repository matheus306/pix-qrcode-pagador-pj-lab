package br.lab.pix.pagador.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Guarda o JWS e o JWK Set usados em cada pagamento por QR dinâmico (S3 na LocalStack).
 * Manual de Segurança do SFN, Vol. II, seção 4.3: registros históricos para não-repúdio
 * (estrutura JWS, certificados e chaves públicas; nunca chaves privadas).
 */
@Component
public class ArquivoNaoRepudio {

    private final S3Client s3;
    private final String bucket;

    public ArquivoNaoRepudio(S3Client s3, @Value("${aws.bucket-nao-repudio}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    public void guardar(String idPagamento, String sufixo, String jws, String jwkSet) {
        s3.putObject(b -> b.bucket(bucket).key(idPagamento + "/" + sufixo + ".jws"), RequestBody.fromString(jws));
        if (jwkSet != null) {
            s3.putObject(b -> b.bucket(bucket).key(idPagamento + "/" + sufixo + ".jwks.json"), RequestBody.fromString(jwkSet));
        }
    }
}
