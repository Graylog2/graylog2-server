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
import org.graylog.testing.completebackend.apis.GraylogApiResponse;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.annotations.FullBackendTest;
import org.graylog.testing.containermatrix.annotations.GraylogBackendConfiguration;
import org.graylog2.security.TruststoreCreator;
import org.junit.jupiter.api.BeforeAll;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
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

@GraylogBackendConfiguration(onlyOnDataNode = true,
                             additionalConfigurationParameters = {
                                           @GraylogBackendConfiguration.ConfigurationParameter(key = "GRAYLOG_DATANODE_INSECURE_STARTUP", value = "false"),
                                           @GraylogBackendConfiguration.ConfigurationParameter(key = "GRAYLOG_SELFSIGNED_STARTUP", value = "true"),
                                           @GraylogBackendConfiguration.ConfigurationParameter(key = "GRAYLOG_ELASTICSEARCH_HOSTS", value = ""),
                                   })
public class ClientCertResourceIT {

    private static GraylogApis api;

    @BeforeAll
    static void beforeAll(GraylogApis graylogApis) {
        api = graylogApis;
        Security.addProvider(new BouncyCastleProvider());
    }

    @FullBackendTest
    void generateClientCert() throws Exception {

        final String privateKeyPassword = RandomStringUtils.secure().nextAlphabetic(10);

        // these roles are supported: all_access,security_rest_api_access,readall
        final ValidatableResponse clientCertResponse = api.post("/ca/clientcert", """
                {
                    "principal": "admin",
                    "roles": ["all_access"],
                    "password": "%s",
                    "certificate_lifetime": "P6M"
                }
                """.formatted(privateKeyPassword), Response.Status.OK.getStatusCode());

        final GraylogApiResponse parsedResponse = new GraylogApiResponse(clientCertResponse);
        final X509Certificate caCertificate = decodeCert(parsedResponse.properJSONPath().read("ca_certificate"));
        final PrivateKey privateKey = decodePrivateKey(parsedResponse.properJSONPath().read("private_key"), privateKeyPassword);
        final X509Certificate certificate = decodeCert(parsedResponse.properJSONPath().read("certificate"));

        Assertions.assertThat(certificate.getIssuerX500Principal().getName()).isEqualTo("CN=Graylog CA");
        Assertions.assertThat(certificate.getSubjectX500Principal().getName()).isEqualTo("CN=admin");
        LocalDate expires = certificate.getNotAfter()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        LocalDate shouldExpire = Instant.now().plus(Duration.ofDays(180)).atZone(ZoneId.systemDefault()).toLocalDate();
        Assertions.assertThat(expires).isBetween(shouldExpire.minusDays(2), shouldExpire.plusDays(2));

        final SSLContext sslContext = createSslContext(
                createKeystore(privateKey, certificate),
                createTruststore(caCertificate));

        final URL url = new URI("https://" + api.backend().searchServerInstance().getHttpHostAddress()).toURL();

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());

            Assertions.assertThat(connection.getResponseCode()).isEqualTo(200);

            final DocumentContext parsedOpensearchResponse = JsonPath.parse(connection.getInputStream());
            final String clusterName = parsedOpensearchResponse.read("cluster_name");
            Assertions.assertThat(clusterName).isEqualTo("datanode-cluster");
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
