package br.lab.pix.simuladores;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyOperation;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.Base64URL;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;

/** Assina payloads como um PSP recebedor (PS256) e publica o JWK Set com x5c e thumbprints. */
@Component
class Assinador {

    enum Modo { NORMAL, ADULTERADO, HS256, JKU_OUTRO_HOST, SEM_KID }

    static final String KID = "recebedor-a-2026";

    private final RSAKey chave;
    private final Base64URL x5tS256;

    @SuppressWarnings("deprecation")
    Assinador() throws Exception {
        KeyPairGenerator gerador = KeyPairGenerator.getInstance("RSA");
        gerador.initialize(2048);
        KeyPair par = gerador.generateKeyPair();

        X500Name nome = new X500Name("CN=Assinatura QR Codes Recebedor A (laboratorio)");
        Instant agora = Instant.now();
        var builder = new JcaX509v3CertificateBuilder(nome, BigInteger.valueOf(agora.toEpochMilli()),
                Date.from(agora.minus(1, ChronoUnit.DAYS)), Date.from(agora.plus(365, ChronoUnit.DAYS)),
                nome, par.getPublic());
        X509Certificate cert = new JcaX509CertificateConverter()
                .getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(par.getPrivate())));

        byte[] der = cert.getEncoded();
        x5tS256 = Base64URL.encode(MessageDigest.getInstance("SHA-256").digest(der));
        Base64URL x5t = Base64URL.encode(MessageDigest.getInstance("SHA-1").digest(der));

        chave = new RSAKey.Builder((RSAPublicKey) par.getPublic())
                .privateKey(par.getPrivate())
                .keyID(KID)
                .keyOperations(Set.of(KeyOperation.VERIFY))
                .x509CertChain(List.of(Base64.encode(der)))
                .x509CertSHA256Thumbprint(x5tS256)
                .x509CertThumbprint(x5t)
                .build();
    }

    String jwkSetPublico() {
        return new JWKSet(chave.toPublicJWK()).toString();
    }

    String assinar(String payloadJson, String host, Modo modo) {
        try {
            String jku = (modo == Modo.JKU_OUTRO_HOST ? "http://pix.atacante.local" : "http://" + host) + "/jwks";
            if (modo == Modo.HS256) {
                JWSObject jws = new JWSObject(new JWSHeader.Builder(JWSAlgorithm.HS256).keyID(KID)
                        .jwkURL(URI.create(jku)).build(), new Payload(payloadJson));
                jws.sign(new MACSigner("segredo-compartilhado-de-32-bytes!!".getBytes(StandardCharsets.UTF_8)));
                return jws.serialize();
            }
            JWSHeader.Builder cab = new JWSHeader.Builder(JWSAlgorithm.PS256)
                    .type(JOSEObjectType.JOSE)
                    .jwkURL(URI.create(jku))
                    .x509CertSHA256Thumbprint(x5tS256);
            if (modo != Modo.SEM_KID) {
                cab.keyID(KID);
            }
            JWSObject jws = new JWSObject(cab.build(), new Payload(payloadJson));
            jws.sign(new RSASSASigner(chave));
            String compacto = jws.serialize();
            if (modo == Modo.ADULTERADO) {
                // Alguém trocou o valor no meio do caminho: a assinatura não confere mais.
                String[] p = compacto.split("\\.");
                String adulterado = payloadJson.replace("\"250.00\"", "\"2.50\"");
                compacto = p[0] + "." + Base64URL.encode(adulterado) + "." + p[2];
            }
            return compacto;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
