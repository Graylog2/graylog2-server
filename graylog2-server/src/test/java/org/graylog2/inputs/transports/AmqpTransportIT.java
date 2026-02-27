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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class AmqpTransportIT {
    @Container
    private static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:4.0-management-alpine")
    );

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

    private MessageInput launchTransport(String queueName, boolean passiveQueue) throws MisfireException {
        final var configuration = new Configuration(Map.of(
                AmqpTransport.CK_HOSTNAME, RABBITMQ.getHost(),
                AmqpTransport.CK_PORT, RABBITMQ.getAmqpPort(),
                AmqpTransport.CK_USERNAME, RABBITMQ.getAdminUsername(),
                AmqpTransport.CK_PASSWORD, RABBITMQ.getAdminPassword(),
                AmqpTransport.CK_VHOST, "/",
                AmqpTransport.CK_PREFETCH, 100,
                AmqpTransport.CK_PARALLEL_QUEUES, 1,
                AmqpTransport.CK_QUEUE, queueName,
                AmqpTransport.CK_QUEUE_DECLARE_PASSIVE, passiveQueue
        ));

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
