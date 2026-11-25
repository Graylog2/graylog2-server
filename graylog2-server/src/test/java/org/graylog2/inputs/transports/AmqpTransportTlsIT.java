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
package org.graylog2.inputs.transports;

import com.google.common.base.Throwables;
import com.google.common.net.InetAddresses;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.graylog2.inputs.transports.AmqpTransport.CK_TLS_VERIFICATION_ENABLED;

/**
 * Verifies the TLS behavior of AMQP inputs against brokers with certificates from a test CA.
 * <p>
 * Inputs created before the {@link AmqpTransport#CK_TLS_VERIFICATION_ENABLED} setting existed must keep working
 * without TLS certificate verification, while new inputs verify certificates.
 */
class AmqpTransportTlsIT {
    private static final DockerImageName IMAGE = DockerImageName.parse("rabbitmq:4.0-management-alpine");
    private static final String TLS_DIR = "/etc/rabbitmq/tls";
    private static final String TLS_CONFIG = """
            listeners.ssl.default = 5671
            ssl_options.cacertfile = /etc/rabbitmq/tls/ca.pem
            ssl_options.certfile = /etc/rabbitmq/tls/server.pem
            ssl_options.keyfile = /etc/rabbitmq/tls/server-key.pem
            ssl_options.verify = verify_none
            ssl_options.fail_if_no_peer_cert = false
            """;

    private static KeyPair ca;
    private static String dockerHost;
    // Broker certificate is issued for the Docker host name.
    private static RabbitMQContainer matchingBroker;
    // Broker certificate is issued for a different host name.
    private static RabbitMQContainer mismatchingBroker;

    @BeforeAll
    static void setUp() throws Exception {
        dockerHost = DockerClientFactory.instance().dockerHostIpAddress();

        final CertificateGenerator generator = new CertificateGenerator(2048);
        ca = generator.generateKeyPair(CertRequest.selfSigned("AMQP test CA")
                .isCA(true)
                .validity(Duration.ofDays(1)));
        final KeyPair matchingCert = generator.generateKeyPair(CertRequest.signed(dockerHost, ca)
                .withSubjectAlternativeName(dockerHost)
                .validity(Duration.ofDays(1)));
        final KeyPair mismatchingCert = generator.generateKeyPair(CertRequest.signed("does-not-match.invalid", ca)
                .withSubjectAlternativeName("does-not-match.invalid")
                .validity(Duration.ofDays(1)));

        matchingBroker = brokerWithCertificate(matchingCert);
        mismatchingBroker = brokerWithCertificate(mismatchingCert);
        matchingBroker.start();
        mismatchingBroker.start();
    }

    @AfterAll
    static void tearDown() {
        if (matchingBroker != null) {
            matchingBroker.stop();
        }
        if (mismatchingBroker != null) {
            mismatchingBroker.stop();
        }
    }

    @Test
    void inputWithoutSettingConnectsToBrokerWithUntrustedCertificateAndMismatchingHostname() throws Exception {
        assertConnects(tlsConfig(mismatchingBroker, Map.of()));
    }

    @Test
    void inputWithDisabledVerificationConnectsToBrokerWithUntrustedCertificateAndMismatchingHostname() throws Exception {
        assertConnects(tlsConfig(mismatchingBroker, Map.of(CK_TLS_VERIFICATION_ENABLED, false)));
    }

    @Test
    void inputWithVerificationRejectsUntrustedCertificate() {
        assertRejected(tlsConfig(matchingBroker, Map.of(CK_TLS_VERIFICATION_ENABLED, true)));
    }

    @Test
    void inputWithVerificationConnectsToBrokerWithTrustedCertificate() throws Exception {
        assumeThat(InetAddresses.isInetAddress(dockerHost))
                .as("Test certificates only support DNS names, but the Docker host is an IP address: %s", dockerHost)
                .isFalse();

        withCaTrustedByDefault(() -> assertConnects(tlsConfig(matchingBroker, Map.of(CK_TLS_VERIFICATION_ENABLED, true))));
    }

    @Test
    void inputWithVerificationRejectsTrustedCertificateWithMismatchingHostname() throws Exception {
        withCaTrustedByDefault(() -> assertRejected(tlsConfig(mismatchingBroker, Map.of(CK_TLS_VERIFICATION_ENABLED, true))));
    }

    @Test
    void inputWithoutTlsIgnoresVerificationSetting() throws Exception {
        final Map<String, Object> config = baseConfig(matchingBroker);
        config.put(AmqpTransport.CK_PORT, matchingBroker.getAmqpPort());
        config.put(AmqpTransport.CK_TLS, false);
        config.put(CK_TLS_VERIFICATION_ENABLED, true);

        assertConnects(config);
    }

    private static void assertConnects(Map<String, Object> config) throws Exception {
        final AmqpConsumer consumer = AmqpConsumerTlsProbe.consumer(config, "amqp-tls-it");
        try {
            consumer.run();
            assertThat(consumer.isConnected()).isTrue();
        } finally {
            consumer.stop();
        }
    }

    private static void assertRejected(Map<String, Object> config) {
        final AmqpConsumer consumer = AmqpConsumerTlsProbe.consumer(config, "amqp-tls-it");
        try {
            assertThatThrownBy(consumer::run)
                    .isInstanceOf(IOException.class)
                    .satisfies(e -> assertThat(Throwables.getCausalChain(e)).hasAtLeastOneElementOfType(SSLHandshakeException.class));
            assertThat(consumer.isConnected()).isFalse();
        } finally {
            try {
                consumer.stop();
            } catch (IOException ignored) {
            }
        }
    }

    private static Map<String, Object> tlsConfig(RabbitMQContainer broker, Map<String, Object> verificationSetting) {
        final Map<String, Object> config = baseConfig(broker);
        config.put(AmqpTransport.CK_PORT, broker.getAmqpsPort());
        config.put(AmqpTransport.CK_TLS, true);
        config.putAll(verificationSetting);
        return config;
    }

    private static Map<String, Object> baseConfig(RabbitMQContainer broker) {
        final Map<String, Object> config = new HashMap<>();
        config.put(AmqpTransport.CK_HOSTNAME, broker.getHost());
        config.put(AmqpTransport.CK_VHOST, "/");
        config.put(AmqpTransport.CK_PREFETCH, 100);
        config.put(AmqpTransport.CK_PARALLEL_QUEUES, 1);
        config.put(AmqpTransport.CK_QUEUE, "amqp-tls-it-" + UUID.randomUUID());
        config.put(AmqpTransport.CK_QUEUE_DECLARE_PASSIVE, false);
        return config;
    }

    /**
     * {@link com.rabbitmq.client.ConnectionFactory#useSslProtocol()} uses the JVM default {@link SSLContext}.
     */
    private static void withCaTrustedByDefault(ThrowingRunnable runnable) throws Exception {
        final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca", ca.certificate());
        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

        final SSLContext originalDefault = SSLContext.getDefault();
        SSLContext.setDefault(sslContext);
        try {
            runnable.run();
        } finally {
            SSLContext.setDefault(originalDefault);
        }
    }

    private static RabbitMQContainer brokerWithCertificate(KeyPair serverCert) throws IOException {
        return new RabbitMQContainer(IMAGE)
                .withCopyToContainer(Transferable.of(toPem(ca.certificate()), 0644), TLS_DIR + "/ca.pem")
                .withCopyToContainer(Transferable.of(toPem(serverCert.certificate()), 0644), TLS_DIR + "/server.pem")
                .withCopyToContainer(Transferable.of(toPem(serverCert.privateKey()), 0644), TLS_DIR + "/server-key.pem")
                .withCopyToContainer(Transferable.of(TLS_CONFIG.getBytes(StandardCharsets.UTF_8), 0644), "/etc/rabbitmq/conf.d/90-tls.conf");
    }

    private static byte[] toPem(Object object) throws IOException {
        final StringWriter writer = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(object);
        }
        return writer.toString().getBytes(StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
