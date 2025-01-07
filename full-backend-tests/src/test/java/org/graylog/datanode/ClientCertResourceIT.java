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
package org.graylog.datanode;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.restassured.response.ValidatableResponse;
import jakarta.annotation.Nonnull;
import jakarta.ws.rs.core.Response;
import org.assertj.core.api.Assertions;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.graylog.security.certutil.CertConstants;
import org.graylog.security.certutil.csr.InMemoryKeystoreInformation;
import org.graylog.security.certutil.csr.KeystoreInformation;
import org.graylog.testing.completebackend.apis.GraylogApiResponse;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog2.security.TruststoreCreator;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;

@ContainerMatrixTestsConfiguration(searchVersions = SearchServer.DATANODE_DEV,
                                   additionalConfigurationParameters = {
                                           @ContainerMatrixTestsConfiguration.ConfigurationParameter(key = "GRAYLOG_DATANODE_INSECURE_STARTUP", value = "false"),
                                           @ContainerMatrixTestsConfiguration.ConfigurationParameter(key = "GRAYLOG_SELFSIGNED_STARTUP", value = "true"),
                                           @ContainerMatrixTestsConfiguration.ConfigurationParameter(key = "GRAYLOG_ELASTICSEARCH_HOSTS", value = ""),
                                   })
public class ClientCertResourceIT {

    private final GraylogApis api;

    public ClientCertResourceIT(GraylogApis api) {
        this.api = api;
    }

    @ContainerMatrixTest
    void generateClientCert() throws Exception {
        // these roles are supported: all_access,security_rest_api_access,readall
        final ValidatableResponse clientCertResponse = api.post("/ca/clientcert", """
                {
                    "principal": "admin",
                    "role": "all_access",
                    "password": "asdfgh",
                    "certificate_lifetime": "P6M"
                }
                """, Response.Status.OK.getStatusCode());

        final GraylogApiResponse parsedResponse = new GraylogApiResponse(clientCertResponse);
        final X509Certificate caCertificate = decodeCert(parsedResponse.properJSONPath().read("ca_certificate"));
        final PrivateKey privateKey = decodePrivateKey(parsedResponse.properJSONPath().read("private_key"));
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
                createKeystore(privateKey, certificate, caCertificate),
                createTruststore(caCertificate));

        final URL url = new URL("https://" + this.api.backend().searchServerInstance().getHttpHostAddress());

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

    private static KeystoreInformation createKeystore(PrivateKey privateKey, X509Certificate certificate, X509Certificate caCertificate) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
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

    private static PrivateKey decodePrivateKey(String pemEncodedCert) {
        final PEMParser pemParser = new PEMParser(new StringReader(pemEncodedCert));
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        try {
            Object parsed = pemParser.readObject();
            if (parsed instanceof PEMKeyPair keyPair) {
                return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
            } else {
                throw new IllegalArgumentException("Couldn't parse private key from provided string, unknown type");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
