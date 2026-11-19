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

import com.google.common.eventbus.EventBus;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.awaitility.Awaitility;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class AmqpTransportIT {
    // Self-signed broker certificate, as commonly used with the "tls" option before "tls_verify" existed.
    private static final KeyPair BROKER_CERTIFICATE = selfSignedBrokerCertificate();

    // RabbitMQContainer#withSSL relies on RABBITMQ_SSL_* environment variables, which RabbitMQ no longer supports.
    @Container
    private static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:4.0-management-alpine")
    )
            .withCopyToContainer(Transferable.of(toPem(BROKER_CERTIFICATE.privateKey())), "/etc/rabbitmq/tls/key.pem")
            .withCopyToContainer(Transferable.of(toPem(BROKER_CERTIFICATE.certificate())), "/etc/rabbitmq/tls/cert.pem")
            .withCopyToContainer(Transferable.of("""
                    listeners.ssl.default = 5671
                    ssl_options.cacertfile = /etc/rabbitmq/tls/cert.pem
                    ssl_options.certfile = /etc/rabbitmq/tls/cert.pem
                    ssl_options.keyfile = /etc/rabbitmq/tls/key.pem
                    ssl_options.verify = verify_none
                    ssl_options.fail_if_no_peer_cert = false
                    """), "/etc/rabbitmq/conf.d/20-tls.conf");

    protected static final String QUEUE_NAME = "test-queue";
    protected static final String QUEUE_NAME_PASSIVE = "test-queue-passive";

    private static final byte[] MESSAGE_BODY = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);

    @Captor
    ArgumentCaptor<RawMessage> messageCaptor;

    @Test
    void consumerWithActiveQueueDeclaration() throws Exception {
        final var input = launchTransport(QUEUE_NAME, false);

        try (Connection connection = rabbitMQConnectionFactory().newConnection(); Channel channel = connection.createChannel()) {
            // wait for our active queue declaration to happen
            Awaitility.await()
                    .atMost(Duration.ofMillis(5_000))
                    .untilAsserted(() -> assertThat(queueExists(channel, QUEUE_NAME)).isTrue());

            channel.basicPublish("", QUEUE_NAME, null, MESSAGE_BODY);
        }

        verify(input, timeout(5_000).times(1)).processRawMessage(messageCaptor.capture());

        assertThat(messageCaptor.getValue()).isNotNull().satisfies(rawMessage -> {
            assertThat(rawMessage.getId()).isNotNull();
            assertThat(rawMessage.getPayload()).isEqualTo(MESSAGE_BODY);
        });
    }

    @Test
    void consumerWithPassiveQueueDeclaration() throws Exception {
        try (Connection connection = rabbitMQConnectionFactory().newConnection(); Channel channel = connection.createChannel()) {
            // simulate the case of an existing queue with custom settings, which would cause a conflict when using
            // active queue declaration
            channel.queueDeclare(QUEUE_NAME_PASSIVE, false, false, false, null);

            assertThat(queueExists(channel, QUEUE_NAME_PASSIVE)).isTrue();

            channel.basicPublish("", QUEUE_NAME_PASSIVE, null, MESSAGE_BODY);
        }

        final var input = launchTransport(QUEUE_NAME_PASSIVE, true);

        verify(input, timeout(5_000).times(1)).processRawMessage(messageCaptor.capture());

        assertThat(messageCaptor.getValue()).isNotNull().satisfies(rawMessage -> {
            assertThat(rawMessage.getId()).isNotNull();
            assertThat(rawMessage.getPayload()).isEqualTo(MESSAGE_BODY);
        });
    }

    @Test
    void tlsWithoutVerificationAcceptsSelfSignedBrokerCertificate() throws Exception {
        final String queueName = "test-queue-tls";
        try (Connection connection = rabbitMQConnectionFactory().newConnection(); Channel channel = connection.createChannel()) {
            channel.queueDeclare(queueName, false, false, false, null);
            channel.basicPublish("", queueName, null, MESSAGE_BODY);
        }

        // No "tls_verify" key, as in the configuration of inputs created before the option existed.
        final var input = mock(MessageInput.class);
        when(input.getId()).thenReturn("TEST-ID");
        final var inputFailureRecorder = mock(InputFailureRecorder.class);
        final var transport = launchTransport(tlsConfiguration(queueName), input, inputFailureRecorder);
        try {
            verify(input, timeout(10_000).times(1)).processRawMessage(messageCaptor.capture());
            assertThat(messageCaptor.getValue().getPayload()).isEqualTo(MESSAGE_BODY);
        } finally {
            transport.stop();
        }
    }

    @Test
    void tlsWithVerificationRejectsUntrustedBrokerCertificate() throws Exception {
        final String queueName = "test-queue-tls-verify";
        try (Connection connection = rabbitMQConnectionFactory().newConnection(); Channel channel = connection.createChannel()) {
            channel.queueDeclare(queueName, false, false, false, null);
            channel.basicPublish("", queueName, null, MESSAGE_BODY);
        }

        final var configuration = tlsConfiguration(queueName);
        configuration.put(AmqpTransport.CK_TLS_VERIFY, true);
        final var input = mock(MessageInput.class);
        when(input.getId()).thenReturn("TEST-ID");
        final var inputFailureRecorder = mock(InputFailureRecorder.class);
        // InputFailureRecorder#setFailing is synchronized, so a verify() with timeout would hold the mock's monitor
        // while the consumer thread tries to report the failure. Capture the reported error instead.
        final CompletableFuture<Throwable> reportedError = new CompletableFuture<>();
        doAnswer(invocation -> reportedError.complete(invocation.getArgument(2)))
                .when(inputFailureRecorder)
                .setFailing(eq(AmqpTransport.class), eq("Error while opening new AMQP connection"), any());
        final var transport = launchTransport(configuration, input, inputFailureRecorder);
        try {
            assertThat(reportedError.get(30, TimeUnit.SECONDS)).hasMessageContaining("certification path");
            verify(input, after(1_000).never()).processRawMessage(any());
        } finally {
            transport.stop();
        }
    }

    private static Map<String, Object> tlsConfiguration(String queueName) throws UnknownHostException {
        final Map<String, Object> configuration = new HashMap<>(baseConfiguration(queueName, true));
        // A single address, so the reported error is the TLS one and not a refused connection on another address
        // of the same name (e.g. "localhost" resolving to both 127.0.0.1 and ::1).
        configuration.put(AmqpTransport.CK_HOSTNAME, InetAddress.getByName(RABBITMQ.getHost()).getHostAddress());
        configuration.put(AmqpTransport.CK_PORT, RABBITMQ.getAmqpsPort());
        configuration.put(AmqpTransport.CK_TLS, true);
        return configuration;
    }

    private static Map<String, Object> baseConfiguration(String queueName, boolean passiveQueue) {
        return Map.of(
                AmqpTransport.CK_HOSTNAME, RABBITMQ.getHost(),
                AmqpTransport.CK_PORT, RABBITMQ.getAmqpPort(),
                AmqpTransport.CK_USERNAME, RABBITMQ.getAdminUsername(),
                AmqpTransport.CK_PASSWORD, RABBITMQ.getAdminPassword(),
                AmqpTransport.CK_VHOST, "/",
                AmqpTransport.CK_PREFETCH, 100,
                AmqpTransport.CK_PARALLEL_QUEUES, 1,
                AmqpTransport.CK_QUEUE, queueName,
                AmqpTransport.CK_QUEUE_DECLARE_PASSIVE, passiveQueue
        );
    }

    private AmqpTransport launchTransport(Map<String, Object> configuration, MessageInput input,
                                          InputFailureRecorder inputFailureRecorder) throws MisfireException {
        final var transport = new AmqpTransport(
                new Configuration(configuration),
                new EventBus(),
                new LocalMetricRegistry(),
                new SimpleNodeId("node-1"),
                new EncryptedValueService(UUID.randomUUID().toString())
        );
        transport.launch(input, inputFailureRecorder);
        return transport;
    }

    private static KeyPair selfSignedBrokerCertificate() {
        try {
            return CertificateGenerator.generate(CertRequest.selfSigned("localhost")
                    .withSubjectAlternativeName("localhost")
                    .validity(Duration.ofDays(1)));
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't create broker certificate", e);
        }
    }

    private static String toPem(Object object) {
        final StringWriter writer = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(object);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return writer.toString();
    }

    private MessageInput launchTransport(String queueName, boolean passiveQueue) throws MisfireException {
        final var configuration = new Configuration(baseConfiguration(queueName, passiveQueue));

        final var transport = new AmqpTransport(
                configuration,
                new EventBus(),
                new LocalMetricRegistry(),
                new SimpleNodeId("node-1"),
                new EncryptedValueService(UUID.randomUUID().toString())
        );

        final var input = mock(MessageInput.class);
        when(input.getId()).thenReturn("TEST-ID");
        final var inputFailureRecorder = mock(InputFailureRecorder.class);
        transport.launch(input, inputFailureRecorder);
        return input;
    }

    private static ConnectionFactory rabbitMQConnectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ.getHost());
        factory.setPort(RABBITMQ.getAmqpPort());
        return factory;
    }

    private static boolean queueExists(Channel channel, String queueName) {
        try {
            channel.queueDeclarePassive(queueName);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
