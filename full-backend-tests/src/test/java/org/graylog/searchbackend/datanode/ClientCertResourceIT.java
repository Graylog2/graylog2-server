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
package org.graylog.searchbackend.datanode;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.restassured.response.ValidatableResponse;
import jakarta.annotation.Nonnull;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.graylog.security.certutil.CertConstants;
import org.graylog.security.certutil.csr.InMemoryKeystoreInformation;
import org.graylog.security.certutil.csr.KeystoreInformation;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApiResponse;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.conditions.EnabledIfSearchServer;
import org.graylog.testing.completebackend.FullBackendTest;
import org.graylog.testing.completebackend.GraylogBackendConfiguration;
import org.graylog2.security.TruststoreCreator;
import org.graylog2.storage.SearchVersion;
import org.junit.jupiter.api.BeforeAll;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;

@GraylogBackendConfiguration(serverLifecycle = Lifecycle.CLASS,
                             env = {
                                     @GraylogBackendConfiguration.Env(key = "GRAYLOG_DATANODE_INSECURE_STARTUP", value = "false"),
                                     @GraylogBackendConfiguration.Env(key = "GRAYLOG_SELFSIGNED_STARTUP", value = "true"),
                                     @GraylogBackendConfiguration.Env(key = "GRAYLOG_ELASTICSEARCH_HOSTS", value = ""),
                             })
@EnabledIfSearchServer(distribution = SearchVersion.Distribution.DATANODE)
public class ClientCertResourceIT {

    private static GraylogApis api;

    @BeforeAll
    static void beforeAll(GraylogApis graylogApis) {
        api = graylogApis;
        Security.addProvider(new BouncyCastleProvider());
    }

    @FullBackendTest
    void generateClientCert() throws Exception {
        final ClientCert clientCert = createClientCert("admin", "all_access");
        final X509Certificate certificate = clientCert.certificate();

        Assertions.assertThat(certificate.getIssuerX500Principal().getName()).isEqualTo("CN=Graylog CA");
        Assertions.assertThat(certificate.getSubjectX500Principal().getName()).isEqualTo("CN=admin");
        LocalDate expires = certificate.getNotAfter()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        LocalDate shouldExpire = Instant.now().plus(Duration.ofDays(180)).atZone(ZoneId.systemDefault()).toLocalDate();
        Assertions.assertThat(expires).isBetween(shouldExpire.minusDays(2), shouldExpire.plusDays(2));

        final HttpResult response = request(clientCert.sslContext(), "GET", "/", null);
        Assertions.assertThat(response.status()).isEqualTo(200);

        final DocumentContext parsedOpensearchResponse = JsonPath.parse(response.body());
        final String clusterName = parsedOpensearchResponse.read("cluster_name");
        Assertions.assertThat(clusterName).isEqualTo("datanode-cluster");
    }

    @FullBackendTest
    void readallClientCertCannotModifySecurityConfiguration() throws Exception {
        final SSLContext sslContext = createClientCert("readall-user", "readall").sslContext();

        // read access to the cluster works as expected
        Assertions.assertThat(request(sslContext, "GET", "/", null).status()).isEqualTo(200);

        // but the security REST API must not be accessible, otherwise the user can map itself to any role
        Assertions.assertThat(request(sslContext, "GET", "/_plugins/_security/api/rolesmapping", null).status()).isEqualTo(403);
        Assertions.assertThat(request(sslContext, "PUT", "/_plugins/_security/api/rolesmapping/all_access", """
                {"backend_roles": ["admin"], "hosts": [], "users": ["readall-user"]}
                """).status()).isEqualTo(403);
    }

    private record ClientCert(X509Certificate certificate, SSLContext sslContext) {}

    private static ClientCert createClientCert(String principal, String role) throws Exception {
        final String privateKeyPassword = RandomStringUtils.secure().nextAlphabetic(10);
        final ValidatableResponse clientCertResponse = api.post("/ca/clientcert", """
                {
                    "principal": "%s",
                    "roles": ["%s"],
                    "password": "%s",
                    "certificate_lifetime": "P6M"
                }
                """.formatted(principal, role, privateKeyPassword), Response.Status.OK.getStatusCode());

        final GraylogApiResponse parsedResponse = new GraylogApiResponse(clientCertResponse);
        final X509Certificate caCertificate = decodeCert(parsedResponse.properJSONPath().read("ca_certificate"));
        final PrivateKey privateKey = decodePrivateKey(parsedResponse.properJSONPath().read("private_key"), privateKeyPassword);
        final X509Certificate certificate = decodeCert(parsedResponse.properJSONPath().read("certificate"));

        return new ClientCert(certificate, createSslContext(createKeystore(privateKey, certificate), createTruststore(caCertificate)));
    }

    private record HttpResult(int status, String body) {}

    private static HttpResult request(SSLContext sslContext, String method, String path, String body) throws Exception {
        final URL url = new URI("https://" + api.backend().searchServerInstance().getHttpHostAddress() + path).toURL();
        final HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setSSLSocketFactory(sslContext.getSocketFactory());
        connection.setRequestMethod(method);
        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            try (OutputStream os = connection.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }
        final int status = connection.getResponseCode();
        try (InputStream is = status < 400 ? connection.getInputStream() : connection.getErrorStream()) {
            return new HttpResult(status, is == null ? "" : new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Nonnull
    private static SSLContext createSslContext(KeystoreInformation keystore, KeyStore truststore) throws Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keystore.loadKeystore(), keystore.password());
        SSLContext sc = SSLContext.getInstance("TLS");

        final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(truststore);

        sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        return sc;
    }

    private static KeyStore createTruststore(X509Certificate caCertificate) {
        return TruststoreCreator.newEmpty().addCertificates(Collections.singletonList(caCertificate)).getTruststore();
    }

    private static KeystoreInformation createKeystore(PrivateKey privateKey, X509Certificate certificate) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore keystore = KeyStore.getInstance(CertConstants.PKCS12);
        keystore.load(null, null);
        final char[] password = "keystorepassword".toCharArray();
        keystore.setKeyEntry("client", privateKey, password, new Certificate[]{certificate});

        return new InMemoryKeystoreInformation(keystore, password);
    }

    private static X509Certificate decodeCert(String pemEncodedCert) {
        final PEMParser pemParser = new PEMParser(new StringReader(pemEncodedCert));
        try {
            Object parsed = pemParser.readObject();
            if (parsed instanceof X509CertificateHolder certificate) {
                return new JcaX509CertificateConverter().getCertificate(certificate);
            } else {
                throw new IllegalArgumentException("Couldn't parse x509 certificate from provided string, unknown type");
            }
        } catch (IOException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    private static PrivateKey decodePrivateKey(String pemEncodedCert, String privateKeyPassword) {
        final PEMParser pemParser = new PEMParser(new StringReader(pemEncodedCert));
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        try {
            Object parsed = pemParser.readObject();
            if (parsed instanceof PEMKeyPair keyPair) {
                return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
            } else if (parsed instanceof PKCS8EncryptedPrivateKeyInfo keyPair) {
                return decryptPrivateKey(privateKeyPassword, keyPair, converter);
            } else {
                throw new IllegalArgumentException("Couldn't parse private key from provided string, unknown type");
            }
        } catch (IOException | OperatorCreationException | PKCSException e) {
            throw new RuntimeException(e);
        }
    }

    private static PrivateKey decryptPrivateKey(String privateKeyPassword, PKCS8EncryptedPrivateKeyInfo keyPair, JcaPEMKeyConverter converter) throws OperatorCreationException, PKCSException, PEMException {
        InputDecryptorProvider decryptorProviderBuilder = new JceOpenSSLPKCS8DecryptorProviderBuilder().setProvider("BC").build(privateKeyPassword.toCharArray());
        final PrivateKeyInfo privateKeyInfo = keyPair.decryptPrivateKeyInfo(decryptorProviderBuilder);
        return converter.getPrivateKey(privateKeyInfo);
    }
}
