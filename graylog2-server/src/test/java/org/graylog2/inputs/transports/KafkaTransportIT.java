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
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.graylog.testing.kafka.KafkaContainer;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.shared.SuppressForbidden;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.shared.utilities.StringUtils.f;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class KafkaTransportIT {
    @Container
    private static final KafkaContainer KAFKA = KafkaContainer.create();

    @Captor
    ArgumentCaptor<RawMessage> messageCaptor;

    @Test
    @SuppressForbidden("Executors.newSingleThreadScheduledExecutor is okay in tests")
    void basicConsumer() throws Exception {
        KAFKA.createTopic("test");

        final var messageValue = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);

        final ProducerRecord<String, byte[]> record = new ProducerRecord<>("test", messageValue);
        try (KafkaProducer<String, byte[]> producer = KAFKA.createByteArrayProducer()) {
            producer.send(record).get(30, TimeUnit.SECONDS);
        }

        final var serverStatus = mock(ServerStatus.class);
        final var config = new Configuration(Map.of(
                KafkaTransport.CK_LEGACY, false,
                KafkaTransport.CK_THREADS, 1,
                KafkaTransport.CK_BOOTSTRAP, f("localhost:%d", KAFKA.getKafkaPort()),
                KafkaTransport.CK_FETCH_MIN_BYTES, 1,
                KafkaTransport.CK_FETCH_WAIT_MAX, 100,
                KafkaTransport.CK_TOPIC_FILTER, "test",
                KafkaTransport.CK_OFFSET_RESET, "smallest"
        ));
        final var transport = new KafkaTransport(
                config,
                new LocalMetricRegistry(),
                new SimpleNodeId("node-1"),
                new EventBus(),
                serverStatus,
                Executors.newSingleThreadScheduledExecutor()
        );
        final var input = mock(MessageInput.class);
        when(input.getId()).thenReturn("TEST");

        transport.lifecycleStateChange(Lifecycle.RUNNING); // Required to set paused=false
        transport.launch(input);

        verify(input, timeout(5_000).times(1)).processRawMessage(messageCaptor.capture());

        assertThat(messageCaptor.getValue()).isNotNull().satisfies(rawMessage -> {
            assertThat(rawMessage.getId()).isNotNull();
            assertThat(rawMessage.getPayload()).isEqualTo(messageValue);
        });
    }
}
