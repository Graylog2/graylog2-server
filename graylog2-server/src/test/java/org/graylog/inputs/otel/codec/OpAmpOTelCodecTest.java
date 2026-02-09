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
package org.graylog.inputs.otel.codec;

import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import org.graylog.inputs.otel.OTelJournal;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.failure.InputProcessingException;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.Inet4Address;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpAmpOTelCodecTest {

    private final MessageFactory messageFactory = new TestMessageFactory();
    private OpAmpOTelCodec codec;

    @BeforeEach
    void setUp() {
        codec = new OpAmpOTelCodec(Configuration.EMPTY_CONFIGURATION, messageFactory);
    }

    @Test
    void bodyMapsToMessageField() {
        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder().setStringValue("test message"))
                .setTimeUnixNano(1700000000000000000L)
                .build();

        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(logRecord)
                .build();

        final var record = OTelJournal.Record.newBuilder()
                .setLog(log)
                .build();

        final var rawMessage = new RawMessage(record.toByteArray());
        final var decoded = codec.decodeSafe(rawMessage);

        assertThat(decoded).isPresent();
        assertThat(decoded.get().getMessage()).isEqualTo("test message");
    }

    @Test
    void timeUnixNanoMapsToTimestamp() {
        // 1700000000000000000 nanoseconds = 1700000000000 milliseconds = 2023-11-14T22:13:20.000Z
        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder().setStringValue("test"))
                .setTimeUnixNano(1700000000000000000L)
                .build();

        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(logRecord)
                .build();

        final var record = OTelJournal.Record.newBuilder()
                .setLog(log)
                .build();

        final var rawMessage = new RawMessage(record.toByteArray());
        final var decoded = codec.decodeSafe(rawMessage);

        assertThat(decoded).isPresent();
        final var expectedTimestamp = new DateTime(1700000000000L, DateTimeZone.UTC);
        assertThat(decoded.get().getTimestamp()).isEqualTo(expectedTimestamp);
    }

    @Test
    void observedTimeUnixNanoFallback() {
        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder().setStringValue("test"))
                .setObservedTimeUnixNano(1700000000000000000L)
                .build();

        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(logRecord)
                .build();

        final var record = OTelJournal.Record.newBuilder()
                .setLog(log)
                .build();

        final var rawMessage = new RawMessage(record.toByteArray());
        final var decoded = codec.decodeSafe(rawMessage);

        assertThat(decoded).isPresent();
        final var expectedTimestamp = new DateTime(1700000000000L, DateTimeZone.UTC);
        assertThat(decoded.get().getTimestamp()).isEqualTo(expectedTimestamp);
    }

    @Test
    void severityTextMapsToLevel() {
        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder().setStringValue("error occurred"))
                .setTimeUnixNano(1700000000000000000L)
                .setSeverityText("ERROR")
                .build();

        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(logRecord)
                .build();

        final var record = OTelJournal.Record.newBuilder()
                .setLog(log)
                .build();

        final var rawMessage = new RawMessage(record.toByteArray());
        final var decoded = codec.decodeSafe(rawMessage);

        assertThat(decoded).isPresent();
        assertThat(decoded.get().getField("level")).isEqualTo("ERROR");
    }

    @Test
    void doesNotProduceOtelPrefixedFields() {
        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder().setStringValue("test message"))
                .setTimeUnixNano(1700000000000000000L)
                .setSeverityText("INFO")
                .build();

        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(logRecord)
                .build();

        final var record = OTelJournal.Record.newBuilder()
                .setLog(log)
                .build();

        final var rawMessage = new RawMessage(record.toByteArray());
        final var decoded = codec.decodeSafe(rawMessage);

        assertThat(decoded).isPresent();
        final var fields = decoded.get().getFields();
        assertThat(fields.keySet())
                .filteredOn(key -> key.startsWith("otel_"))
                .isEmpty();
    }

    @Test
    void handlesAgentInstanceUidWhenPresent() {
        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder().setStringValue("agent log"))
                .setTimeUnixNano(1700000000000000000L)
                .build();

        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(logRecord)
                .build();

        final var record = OTelJournal.Record.newBuilder()
                .setLog(log)
                .setAgentInstanceUid("agent-123")
                .build();

        final var rawMessage = new RawMessage(record.toByteArray());
        final var decoded = codec.decodeSafe(rawMessage);

        assertThat(decoded).isPresent();
        assertThat(decoded.get().getField("agent_instance_uid")).isEqualTo("agent-123");
    }

    @Test
    void handlesMissingAgentInstanceUidGracefully() {
        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder().setStringValue("no agent uid"))
                .setTimeUnixNano(1700000000000000000L)
                .build();

        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(logRecord)
                .build();

        final var record = OTelJournal.Record.newBuilder()
                .setLog(log)
                .build();

        final var rawMessage = new RawMessage(record.toByteArray());
        final var decoded = codec.decodeSafe(rawMessage);

        assertThat(decoded).isPresent();
        assertThat(decoded.get().getField("agent_instance_uid")).isNull();
    }

    @Test
    void missingPayloadThrowsInputProcessingException() {
        final var record = OTelJournal.Record.newBuilder().build();

        final var rawMessage = new RawMessage(record.toByteArray());

        assertThatThrownBy(() -> codec.decodeSafe(rawMessage))
                .isInstanceOf(InputProcessingException.class)
                .hasMessageContaining("No payload set");
    }

    @Test
    void sourceFromRemoteAddress() {
        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder().setStringValue("test"))
                .setTimeUnixNano(1700000000000000000L)
                .build();

        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(logRecord)
                .build();

        final var record = OTelJournal.Record.newBuilder()
                .setLog(log)
                .build();

        final var rawMessage = new RawMessage(record.toByteArray(),
                new InetSocketAddress(Inet4Address.getLoopbackAddress(), 12345));
        final var decoded = codec.decodeSafe(rawMessage);

        assertThat(decoded).isPresent();
        assertThat(decoded.get().getSource()).isEqualTo("127.0.0.1");
    }

    @Test
    void invalidProtobufThrowsInputProcessingException() {
        final var rawMessage = new RawMessage(new byte[]{0x01, 0x02, 0x03});

        assertThatThrownBy(() -> codec.decodeSafe(rawMessage))
                .isInstanceOf(InputProcessingException.class)
                .hasMessageContaining("Error parsing");
    }
}
