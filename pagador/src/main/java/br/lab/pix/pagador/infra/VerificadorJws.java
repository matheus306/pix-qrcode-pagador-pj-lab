package br.lab.pix.pagador.infra;

import br.lab.pix.pagador.dominio.CabecalhoJws;
import br.lab.pix.pagador.dominio.ChaveJwk;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyOperation;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * A parte mecânica do JWS: abrir o cabeçalho, buscar o JWK Set no "jku" e conferir a assinatura.
 * Quem DECIDE se aceita (algoritmo, thumbprint, host do jku...) são as regras R09 e R10.
 */
@Component
public class VerificadorJws {

    public record Analise(CabecalhoJws cabecalho, ChaveJwk chave, boolean assinaturaConfere,
                          String payloadJson, String jwkSetJson) {
    }

    private final ClienteRecebedor recebedor;
    private final ObjectMapper json;

    public VerificadorJws(ClienteRecebedor recebedor, ObjectMapper json) {
        this.recebedor = recebedor;
        this.json = json;
    }

    public Analise analisar(String jwsCompacto) {
        String[] partes = jwsCompacto == null ? new String[0] : jwsCompacto.trim().split("\\.");
        if (partes.length != 3) {
            return new Analise(new CabecalhoJws(null, null, null, null, null), null, false, null, null);
        }
        CabecalhoJws cabecalho = lerCabecalho(partes[0]);
        String payloadJson = decodificar(partes[1]);

        if (cabecalho.jku() == null || cabecalho.kid() == null) {
            return new Analise(cabecalho, null, false, payloadJson, null);
        }
        Http.Resposta jwks = Http.get(recebedor.paraHttp(cabecalho.jku()));
        if (jwks.status() != 200) {
            return new Analise(cabecalho, null, false, payloadJson, null);
        }
        try {
            JWK jwk = JWKSet.parse(jwks.corpo()).getKeyByKeyId(cabecalho.kid());
            if (jwk == null) {
                return new Analise(cabecalho, null, false, payloadJson, jwks.corpo());
            }
            List<String> ops = jwk.getKeyOperations() == null ? List.of()
                    : jwk.getKeyOperations().stream().map(KeyOperation::identifier).toList();
            @SuppressWarnings("deprecation")
            String x5t = jwk.getX509CertThumbprint() == null ? null : jwk.getX509CertThumbprint().toString();
            String x5tS256 = jwk.getX509CertSHA256Thumbprint() == null ? null : jwk.getX509CertSHA256Thumbprint().toString();
            ChaveJwk chave = new ChaveJwk(jwk.getKeyID(), x5t, x5tS256, ops);
            return new Analise(cabecalho, chave, conferirAssinatura(jwsCompacto, jwk), payloadJson, jwks.corpo());
        } catch (Exception e) {
            return new Analise(cabecalho, null, false, payloadJson, jwks.corpo());
        }
    }

    private boolean conferirAssinatura(String jwsCompacto, JWK jwk) {
        try {
            JWSVerifier verificador;
            if (jwk instanceof RSAKey rsa) {
                verificador = new RSASSAVerifier(rsa);
            } else if (jwk instanceof ECKey ec) {
                verificador = new ECDSAVerifier(ec);
            } else {
                return false;
            }
            return JWSObject.parse(jwsCompacto).verify(verificador);
        } catch (Exception e) {
            return false;
        }
    }

    private CabecalhoJws lerCabecalho(String parte) {
        try {
            Map<String, Object> h = json.readValue(decodificar(parte), new TypeReference<>() { });
            return new CabecalhoJws(texto(h.get("alg")), texto(h.get("kid")), texto(h.get("jku")),
                    texto(h.get("x5t")), texto(h.get("x5t#S256")));
        } catch (Exception e) {
            return new CabecalhoJws(null, null, null, null, null);
        }
    }

    private static String texto(Object o) {
        return o == null ? null : o.toString();
    }

    private static String decodificar(String base64Url) {
        try {
            return new String(Base64.getUrlDecoder().decode(base64Url), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
