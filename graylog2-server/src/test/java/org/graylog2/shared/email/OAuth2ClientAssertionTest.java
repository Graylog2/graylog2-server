/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.shared.email;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.graylog2.plugin.inputs.transports.util.KeyUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.mail.MessagingException;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.Security;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuth2ClientAssertionTest {
    private static final String TOKEN_URL = "https://login.microsoftonline.com/tenant/oauth2/v2.0/token";
    // Test certificates are valid from their creation on, so assertions are created a bit later.
    private static final Instant NOW = Instant.now().plusSeconds(60).truncatedTo(ChronoUnit.SECONDS);

    @TempDir
    private Path tempDir;

    private final OAuth2ClientAssertion clientAssertion = new OAuth2ClientAssertion(Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeAll
    static void registerBouncyCastle() {
        // Registered at server startup, see CmdLineTool
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Test
    void createsAssertionInMicrosoftEntraFormat() throws Exception {
        final ClientCertificate client = ClientCertificate.write(tempDir);

        final String assertion = clientAssertion.create("the-client-id", TOKEN_URL, client.certificateFile(), client.keyFile(), null);

        final Jws<Claims> jws = parser(client).parseSignedClaims(assertion);
        assertThat(jws.getHeader().getAlgorithm()).isEqualTo("PS256");
        assertThat(jws.getHeader().getType()).isEqualTo("JWT");
        assertThat(jws.getHeader().getX509Sha256Thumbprint())
                .isEqualTo(MessageDigest.getInstance("SHA-256").digest(client.keyPair().certificate().getEncoded()));
        assertThat(decodedPart(assertion, 0)).contains("\"x5t#S256\":");

        final Claims claims = jws.getPayload();
        assertThat(claims.getIssuer()).isEqualTo("the-client-id");
        assertThat(claims.getSubject()).isEqualTo("the-client-id");
        assertThat(claims.getAudience()).containsExactly(TOKEN_URL);
        assertThat(decodedPart(assertion, 1)).contains("\"aud\":\"" + TOKEN_URL + "\"");
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getIssuedAt()).isEqualTo(Date.from(NOW));
        assertThat(claims.getNotBefore()).isEqualTo(Date.from(NOW));
        assertThat(claims.getExpiration()).isEqualTo(Date.from(NOW.plus(OAuth2ClientAssertion.LIFETIME)));
    }

    @Test
    void usesNewIdForEveryAssertion() throws Exception {
        final ClientCertificate client = ClientCertificate.write(tempDir);

        final String first = clientAssertion.create("the-client-id", TOKEN_URL, client.certificateFile(), client.keyFile(), null);
        final String second = clientAssertion.create("the-client-id", TOKEN_URL, client.certificateFile(), client.keyFile(), null);

        assertThat(claims(first, client).getId()).isNotEqualTo(claims(second, client).getId());
    }

    @Test
    void readsEncryptedPrivateKey() throws Exception {
        final ClientCertificate client = ClientCertificate.write(tempDir);
        final Path encryptedKey = KeyUtil.generatePKCS8FromPrivateKey(tempDir, "secret".toCharArray(), client.keyPair().privateKey()).toPath();

        final String assertion = clientAssertion.create("the-client-id", TOKEN_URL, client.certificateFile(), encryptedKey, "secret");

        assertThat(claims(assertion, client).getIssuer()).isEqualTo("the-client-id");
    }

    @Test
    void readsPkcs1PrivateKey() throws Exception {
        final ClientCertificate client = ClientCertificate.write(tempDir);
        final Path pkcs1Key = tempDir.resolve("pkcs1.key");
        Files.writeString(pkcs1Key, pem(client.keyPair().privateKey()));
        assertThat(Files.readString(pkcs1Key)).startsWith("-----BEGIN RSA PRIVATE KEY-----");

        final String assertion = clientAssertion.create("the-client-id", TOKEN_URL, client.certificateFile(), pkcs1Key, null);

        assertThat(claims(assertion, client).getIssuer()).isEqualTo("the-client-id");
    }

    @Test
    void failsForWrongPrivateKeyPassword() throws Exception {
        final ClientCertificate client = ClientCertificate.write(tempDir);
        final Path encryptedKey = KeyUtil.generatePKCS8FromPrivateKey(tempDir, "secret".toCharArray(), client.keyPair().privateKey()).toPath();

        assertThatThrownBy(() -> clientAssertion.create("the-client-id", TOKEN_URL, client.certificateFile(), encryptedKey, "wrong"))
                .isInstanceOf(MessagingException.class)
                .hasMessageStartingWith("Failed to read OAuth 2.0 client private key <" + encryptedKey + ">");
    }

    @Test
    void failsIfPrivateKeyFileContainsNoKey() throws Exception {
        final ClientCertificate client = ClientCertificate.write(tempDir);
        final Path notAKey = Files.writeString(tempDir.resolve("not-a.key"), "not a key");

        assertThatThrownBy(() -> clientAssertion.create("the-client-id", TOKEN_URL, client.certificateFile(), notAKey, null))
                .isInstanceOf(MessagingException.class)
                .hasMessageStartingWith("Failed to read OAuth 2.0 client private key <" + notAKey + ">");
    }

    @Test
    void failsIfCertificateFileContainsNoCertificate() throws Exception {
        final ClientCertificate client = ClientCertificate.write(tempDir);
        final Path notACertificate = Files.writeString(tempDir.resolve("not-a.crt"), "not a certificate");

        assertThatThrownBy(() -> clientAssertion.create("the-client-id", TOKEN_URL, notACertificate, client.keyFile(), null))
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("<" + notACertificate + ">");
    }

    @Test
    void failsIfPrivateKeyDoesNotBelongToCertificate() throws Exception {
        final ClientCertificate client = ClientCertificate.write(tempDir);
        final ClientCertificate other = ClientCertificate.write(Files.createDirectory(tempDir.resolve("other")));

        assertThatThrownBy(() -> clientAssertion.create("the-client-id", TOKEN_URL, client.certificateFile(), other.keyFile(), null))
                .isInstanceOf(MessagingException.class)
                .hasMessage("OAuth 2.0 client private key <" + other.keyFile() + "> does not belong to the certificate");
    }

    @Test
    void failsForExpiredCertificate() throws Exception {
        final ClientCertificate client = ClientCertificate.write(tempDir);
        final Instant later = NOW.plus(Duration.ofDays(2));
        final OAuth2ClientAssertion laterAssertion = new OAuth2ClientAssertion(Clock.fixed(later, ZoneOffset.UTC));

        assertThatThrownBy(() -> laterAssertion.create("the-client-id", TOKEN_URL, client.certificateFile(), client.keyFile(), null))
                .isInstanceOf(MessagingException.class)
                .hasMessage("OAuth 2.0 client certificate <" + client.certificateFile() + "> expired on "
                        + client.keyPair().certificate().getNotAfter().toInstant());
    }

    @Test
    void failsForCertificateThatIsNotValidYet() throws Exception {
        final ClientCertificate client = ClientCertificate.write(tempDir);
        final OAuth2ClientAssertion earlierAssertion = new OAuth2ClientAssertion(Clock.fixed(NOW.minus(Duration.ofDays(1)), ZoneOffset.UTC));

        assertThatThrownBy(() -> earlierAssertion.create("the-client-id", TOKEN_URL, client.certificateFile(), client.keyFile(), null))
                .isInstanceOf(MessagingException.class)
                .hasMessage("OAuth 2.0 client certificate <" + client.certificateFile() + "> is not valid before "
                        + client.keyPair().certificate().getNotBefore().toInstant());
    }

    private static Claims claims(String assertion, ClientCertificate client) {
        return parser(client).parseSignedClaims(assertion).getPayload();
    }

    private static JwtParser parser(ClientCertificate client) {
        return Jwts.parser().verifyWith(client.keyPair().publicKey()).clock(() -> Date.from(NOW)).build();
    }

    private static String decodedPart(String jwt, int index) {
        return new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[index]), StandardCharsets.UTF_8);
    }

    private static String pem(Object object) throws IOException {
        final StringWriter writer = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(object);
        }
        return writer.toString();
    }

    /**
     * A self-signed client certificate valid for one day and its unencrypted PKCS#8 private key, written to PEM files.
     */
    record ClientCertificate(KeyPair keyPair, Path certificateFile, Path keyFile) {
        static ClientCertificate write(Path dir) throws Exception {
            final KeyPair keyPair = new CertificateGenerator(2048)
                    .generateKeyPair(CertRequest.selfSigned("graylog-smtp").validity(Duration.ofDays(1)));
            final Path certificateFile = Files.writeString(dir.resolve("client.crt"), pem(keyPair.certificate()));
            final Path keyFile = Files.writeString(dir.resolve("client.key"),
                    pem(new JcaPKCS8Generator(keyPair.privateKey(), null).generate()));
            return new ClientCertificate(keyPair, certificateFile, keyFile);
        }
    }
}
