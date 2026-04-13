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
package org.graylog.integrations.aws.transports;

import org.graylog.integrations.aws.AWSMessageType;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KinesisShardProcessorFactoryTest {

    @Mock
    private KinesisTransport transport;

    @Mock
    private ProcessRecordsInput processRecordsInput;

    private final List<RawMessage> capturedMessages = new ArrayList<>();

    @BeforeEach
    void setUp() {
        capturedMessages.clear();
    }

    @Test
    void cloudWatchRecordDistributesDecompressedSizeAcrossMessages() throws IOException {
        final String json = """
                {
                  "messageType": "DATA_MESSAGE",
                  "owner": "123456789",
                  "logGroup": "test-group",
                  "logStream": "test-stream",
                  "subscriptionFilters": ["filter"],
                  "logEvents": [
                    {"id": "1", "timestamp": 1700000000000, "message": "log message one"},
                    {"id": "2", "timestamp": 1700000000000, "message": "log message two"}
                  ]
                }""";
        final byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        final byte[] compressed = gzipCompress(jsonBytes);

        final var factory = createFactory(AWSMessageType.KINESIS_CLOUDWATCH_FLOW_LOGS);
        processRecord(factory, compressed);

        assertThat(capturedMessages).hasSize(2);

        // Equal message lengths → each gets half the decompressed size
        final int expectedPerMessage = jsonBytes.length / 2;
        assertThat(capturedMessages.get(0).getInputMessageSize()).isEqualTo(expectedPerMessage);
        // Last message gets the remainder
        assertThat(capturedMessages.get(1).getInputMessageSize()).isEqualTo(jsonBytes.length - expectedPerMessage);
    }

    @Test
    void rawKinesisRecordSetsInputMessageSizeToPayloadLength() {
        final String message = "a raw log message";
        final byte[] payload = message.getBytes(StandardCharsets.UTF_8);

        final var factory = createFactory(AWSMessageType.KINESIS_RAW);
        processRecord(factory, payload);

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.getFirst().getInputMessageSize()).isEqualTo(payload.length);
    }

    private KinesisShardProcessorFactory createFactory(AWSMessageType messageType) {
        return new KinesisShardProcessorFactory(
                new ObjectMapperProvider().get(),
                transport,
                capturedMessages::add,
                "test-stream",
                messageType);
    }

    private void processRecord(KinesisShardProcessorFactory factory, byte[] payload) {
        final var kinesisRecord = KinesisClientRecord.builder()
                .data(ByteBuffer.wrap(payload))
                .approximateArrivalTimestamp(Instant.now())
                .build();
        when(processRecordsInput.records()).thenReturn(List.of(kinesisRecord));
        factory.shardRecordProcessor().processRecords(processRecordsInput);
    }

    private static byte[] gzipCompress(byte[] data) throws IOException {
        final var bos = new ByteArrayOutputStream(data.length);
        try (final var gzip = new GZIPOutputStream(bos)) {
            gzip.write(data);
        }
        return bos.toByteArray();
    }
}
