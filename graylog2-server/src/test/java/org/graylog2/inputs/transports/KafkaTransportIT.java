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
import org.graylog.testing.SlowParameterizedTest;
import org.graylog.testing.SlowTest;
import org.graylog.testing.kafka.KafkaContainer;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.shared.SuppressForbidden;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.shared.utilities.StringUtils.f;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Runs the Kafka transport tests against a specific Kafka version. Concrete subclasses (see below) each provide
 * their own {@link KafkaContainer} of a fixed version, so the same set of tests runs against every supported
 * Kafka broker version.
 */
@ExtendWith(MockitoExtension.class)
abstract class KafkaTransportIT {
    @Captor
    ArgumentCaptor<RawMessage> messageCaptor;

    private final List<KafkaTransport> launchedTransports = new ArrayList<>();

    /**
     * Returns the Kafka container to run the tests against. Each subclass provides a container for a specific
     * Kafka version.
     *
     * @return the Kafka container
     */
    protected abstract KafkaContainer kafka();

    @AfterEach
    void stopTransports() {
        // Stop the transports so their consumer threads shut down. Otherwise leaked consumers keep retrying
        // against the (torn down) broker and flood the log with connection warnings.
        launchedTransports.forEach(KafkaTransport::stop);
        launchedTransports.clear();
    }

    @SlowTest
    void basicConsumer() throws Exception {
        final var topic = "test";
        kafka().createTopic(topic);

        final var messageValue = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);

        final ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, messageValue);
        try (KafkaProducer<String, byte[]> producer = kafka().createByteArrayProducer()) {
            producer.send(record).get(30, TimeUnit.SECONDS);
        }

        final var input = launchTransport(topic, "basic-consumer");

        verify(input, timeout(5_000).times(1)).processRawMessage(messageCaptor.capture());

        assertThat(messageCaptor.getValue()).isNotNull().satisfies(rawMessage -> {
            assertThat(rawMessage.getId()).isNotNull();
            assertThat(rawMessage.getPayload()).isEqualTo(messageValue);
        });
    }

    /**
     * Verifies that the transport can consume record batches that were compressed by the producer. Consuming
     * compressed batches requires the matching compression codec (and its native library) to be present on the
     * classpath, so this test guards against a codec library going missing at runtime.
     *
     * @see <a href="https://github.com/Graylog2/graylog2-server/pull/26674">PR #26674</a>
     */
    @SlowParameterizedTest
    @ValueSource(strings = {"gzip", "snappy", "lz4", "zstd"})
    void compressedConsumer(String compressionType) throws Exception {
        final var topic = f("test-%s", compressionType);
        kafka().createTopic(topic);

        // Produce a batch of records with compressible (repetitive) payloads so the codec actually kicks in.
        final List<String> messageValues = IntStream.range(0, 10)
                .mapToObj(i -> f("%s-compressed-message-%d-%s", compressionType, i, "x".repeat(256)))
                .toList();

        try (KafkaProducer<String, byte[]> producer = kafka().createByteArrayProducer(compressionType)) {
            for (final String messageValue : messageValues) {
                producer.send(new ProducerRecord<>(topic, messageValue.getBytes(StandardCharsets.UTF_8)));
            }
            producer.flush();
        }

        final var input = launchTransport(topic, f("compressed-consumer-%s", compressionType));

        verify(input, timeout(10_000).times(messageValues.size())).processRawMessage(messageCaptor.capture());

        // Compare the payloads as strings because AssertJ compares byte[] elements by reference, not by content.
        final List<String> receivedPayloads = messageCaptor.getAllValues().stream()
                .map(rawMessage -> new String(rawMessage.getPayload(), StandardCharsets.UTF_8))
                .toList();

        assertThat(receivedPayloads).containsExactlyInAnyOrderElementsOf(messageValues);
    }

    @SuppressForbidden("Executors.newSingleThreadScheduledExecutor is okay in tests")
    private MessageInput launchTransport(String topicFilter, String groupId) throws Exception {
        final var serverStatus = mock(ServerStatus.class);
        final var config = new Configuration(Map.of(
                KafkaTransport.CK_LEGACY, false,
                KafkaTransport.CK_THREADS, 1,
                KafkaTransport.CK_BOOTSTRAP, f("localhost:%d", kafka().getKafkaPort()),
                KafkaTransport.CK_FETCH_MIN_BYTES, 1,
                KafkaTransport.CK_FETCH_WAIT_MAX, 100,
                KafkaTransport.CK_TOPIC_FILTER, topicFilter,
                KafkaTransport.CK_OFFSET_RESET, "smallest",
                KafkaTransport.CK_GROUP_ID, groupId
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
        launchedTransports.add(transport);

        return input;
    }
}

@Testcontainers
class KafkaTransport37IT extends KafkaTransportIT {
    @Container
    private static final KafkaContainer KAFKA = KafkaContainer.create(KafkaContainer.Version.V37);

    @Override
    protected KafkaContainer kafka() {
        return KAFKA;
    }
}

@Testcontainers
class KafkaTransport38IT extends KafkaTransportIT {
    @Container
    private static final KafkaContainer KAFKA = KafkaContainer.create(KafkaContainer.Version.V38);

    @Override
    protected KafkaContainer kafka() {
        return KAFKA;
    }
}

@Testcontainers
class KafkaTransport39IT extends KafkaTransportIT {
    @Container
    private static final KafkaContainer KAFKA = KafkaContainer.create(KafkaContainer.Version.V39);

    @Override
    protected KafkaContainer kafka() {
        return KAFKA;
    }
}

@Testcontainers
class KafkaTransport40IT extends KafkaTransportIT {
    @Container
    private static final KafkaContainer KAFKA = KafkaContainer.create(KafkaContainer.Version.V40);

    @Override
    protected KafkaContainer kafka() {
        return KAFKA;
    }
}

@Testcontainers
class KafkaTransport41IT extends KafkaTransportIT {
    @Container
    private static final KafkaContainer KAFKA = KafkaContainer.create(KafkaContainer.Version.V41);

    @Override
    protected KafkaContainer kafka() {
        return KAFKA;
    }
}

@Testcontainers
class KafkaTransport42IT extends KafkaTransportIT {
    @Container
    private static final KafkaContainer KAFKA = KafkaContainer.create(KafkaContainer.Version.V42);

    @Override
    protected KafkaContainer kafka() {
        return KAFKA;
    }
}

@Testcontainers
class KafkaTransport43IT extends KafkaTransportIT {
    @Container
    private static final KafkaContainer KAFKA = KafkaContainer.create(KafkaContainer.Version.V43);

    @Override
    protected KafkaContainer kafka() {
        return KAFKA;
    }
}
