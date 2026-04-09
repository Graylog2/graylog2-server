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
package org.graylog.collectors.input;

import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import org.graylog.collectors.CollectorJournal;
import org.graylog.collectors.input.debug.OtlpTrafficDump;
import org.graylog.inputs.otel.OTelJournal;
import org.graylog.inputs.otel.codec.OTelTypeConverter;
import org.graylog.schema.EventFields;
import org.graylog.schema.VendorFields;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.failure.InputProcessingException;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CollectorIngestCodecTest {

    private static final String TEST_RECEIVER_TYPE = "filelog";
    private static final String TEST_INSTANCE_UID = "test-agent";

    private final MessageFactory messageFactory = new TestMessageFactory();

    @Mock
    private OtlpTrafficDump dumpWriter;

    private OTelTypeConverter typeConverter;
    private CollectorIngestCodec codec;

    @BeforeEach
    void setUp() {
        typeConverter = new OTelTypeConverter(new ObjectMapperProvider().get());
        codec = new CollectorIngestCodec(Configuration.EMPTY_CONFIGURATION, messageFactory,
                dumpWriter, typeConverter, Map.of(TEST_RECEIVER_TYPE, log -> Map.of()));
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

        final var otelRecord = OTelJournal.Record.newBuilder()
                .setLog(log)
                .build();

        final var collectorRecord = CollectorJournal.Record.newBuilder()
                .setOtelRecord(otelRecord)
                .setCollectorReceiverType(TEST_RECEIVER_TYPE)
                .setCollectorInstanceUid(TEST_INSTANCE_UID)
                .build();

        final var rawMessage = new RawMessage(collectorRecord.toByteArray());
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

        final var otelRecord = OTelJournal.Record.newBuilder()
                .setLog(log)
                .build();

        final var collectorRecord = CollectorJournal.Record.newBuilder()
                .setOtelRecord(otelRecord)
                .setCollectorReceiverType(TEST_RECEIVER_TYPE)
                .setCollectorInstanceUid(TEST_INSTANCE_UID)
                .build();

        final var rawMessage = new RawMessage(collectorRecord.toByteArray());
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

        final var otelRecord = OTelJournal.Record.newBuilder()
                .setLog(log)
                .build();

        final var collectorRecord = CollectorJournal.Record.newBuilder()
                .setOtelRecord(otelRecord)
                .setCollectorReceiverType(TEST_RECEIVER_TYPE)
                .setCollectorInstanceUid(TEST_INSTANCE_UID)
                .build();

        final var rawMessage = new RawMessage(collectorRecord.toByteArray());
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

        final var otelRecord = OTelJournal.Record.newBuilder()
                .setLog(log)
                .build();

        final var collectorRecord = CollectorJournal.Record.newBuilder()
                .setOtelRecord(otelRecord)
                .setCollectorReceiverType(TEST_RECEIVER_TYPE)
                .setCollectorInstanceUid(TEST_INSTANCE_UID)
                .build();

        final var rawMessage = new RawMessage(collectorRecord.toByteArray());
        final var decoded = codec.decodeSafe(rawMessage);

        assertThat(decoded).isPresent();
        assertThat(decoded.get().getField(VendorFields.VENDOR_EVENT_SEVERITY)).isEqualTo("ERROR");
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

        final var otelRecord = OTelJournal.Record.newBuilder()
                .setLog(log)
                .build();

        final var collectorRecord = CollectorJournal.Record.newBuilder()
                .setOtelRecord(otelRecord)
                .setCollectorReceiverType(TEST_RECEIVER_TYPE)
                .setCollectorInstanceUid(TEST_INSTANCE_UID)
                .build();

        final var rawMessage = new RawMessage(collectorRecord.toByteArray());
        final var decoded = codec.decodeSafe(rawMessage);

        assertThat(decoded).isPresent();
        final var fields = decoded.get().getFields();
        assertThat(fields.keySet())
                .filteredOn(key -> key.startsWith("otel_"))
                .isEmpty();
    }

    @Test
    void doesMapCollectorInstanceUidToMessageField() {
        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder().setStringValue("agent log"))
                .setTimeUnixNano(1700000000000000000L)
                .build();

        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(logRecord)
                .build();

        final var otelRecord = OTelJournal.Record.newBuilder()
                .setLog(log)
                .build();

        final var collectorRecord = CollectorJournal.Record.newBuilder()
                .setOtelRecord(otelRecord)
                .setCollectorReceiverType(TEST_RECEIVER_TYPE)
                .setCollectorInstanceUid(TEST_INSTANCE_UID)
                .build();

        final var rawMessage = new RawMessage(collectorRecord.toByteArray());
        final var decoded = codec.decodeSafe(rawMessage);

        assertThat(decoded).isPresent();
        assertThat(decoded.get().getField(CollectorIngestCodec.FIELD_COLLECTOR_INSTANCE_UID)).isEqualTo(TEST_INSTANCE_UID);
    }

    @Test
    void handlesMissingCollectorInstanceUidGracefully() {
        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder().setStringValue("no agent uid"))
                .setTimeUnixNano(1700000000000000000L)
                .build();

        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(logRecord)
                .build();

        final var otelRecord = OTelJournal.Record.newBuilder()
                .setLog(log)
                .build();

        final var collectorRecord = CollectorJournal.Record.newBuilder()
                .setOtelRecord(otelRecord)
                .setCollectorReceiverType(TEST_RECEIVER_TYPE)
                .build();

        final var rawMessage = new RawMessage(collectorRecord.toByteArray());
        final var decoded = codec.decodeSafe(rawMessage);

        assertThat(decoded).isPresent();
        assertThat(decoded.get().getField(CollectorIngestCodec.FIELD_COLLECTOR_INSTANCE_UID)).isNull();
    }

    @Test
    void missingPayloadThrowsInputProcessingException() {
        final var otelRecord = OTelJournal.Record.newBuilder().build();

        final var collectorRecord = CollectorJournal.Record.newBuilder()
                .setOtelRecord(otelRecord)
                .setCollectorReceiverType(TEST_RECEIVER_TYPE)
                .setCollectorInstanceUid(TEST_INSTANCE_UID)
                .build();

        final var rawMessage = new RawMessage(collectorRecord.toByteArray());

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

        final var otelRecord = OTelJournal.Record.newBuilder()
                .setLog(log)
                .build();

        final var collectorRecord = CollectorJournal.Record.newBuilder()
                .setOtelRecord(otelRecord)
                .setCollectorReceiverType(TEST_RECEIVER_TYPE)
                .setCollectorInstanceUid(TEST_INSTANCE_UID)
                .build();

        final var rawMessage = new RawMessage(collectorRecord.toByteArray(),
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

    @Test
    void delegatesToDumpWriter() {
        final var collectorRecord = buildCollectorRecord("dump me", "agent-42");
        final var rawMessage = new RawMessage(collectorRecord.toByteArray());
        codec.decodeSafe(rawMessage);

        verify(dumpWriter).write(collectorRecord);
    }

    @Test
    void nonStringBodyIsConvertedToString() {
        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder().setIntValue(42))
                .setTimeUnixNano(1700000000000000000L)
                .build();

        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(logRecord)
                .build();

        final var otelRecord = OTelJournal.Record.newBuilder()
                .setLog(log)
                .build();

        final var collectorRecord = CollectorJournal.Record.newBuilder()
                .setOtelRecord(otelRecord)
                .setCollectorReceiverType(TEST_RECEIVER_TYPE)
                .setCollectorInstanceUid(TEST_INSTANCE_UID)
                .build();

        final var rawMessage = new RawMessage(collectorRecord.toByteArray());
        final var decoded = codec.decodeSafe(rawMessage);

        assertThat(decoded).isPresent();
        assertThat(decoded.get().getMessage()).isEqualTo("42");
    }

    @Test
    void setsReceiverTypeField() {
        final var codecWithProcessor = new CollectorIngestCodec(
                Configuration.EMPTY_CONFIGURATION, messageFactory, dumpWriter, typeConverter,
                Map.of("filelog", log -> Map.of(EventFields.EVENT_LOG_NAME, "test.log")));

        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder().setStringValue("test"))
                .setTimeUnixNano(1700000000000000000L)
                .build();

        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(logRecord)
                .build();
        final var otelRecord = OTelJournal.Record.newBuilder()
                .setLog(log)
                .build();
        final var collectorRecord = CollectorJournal.Record.newBuilder()
                .setOtelRecord(otelRecord)
                .setCollectorReceiverType("filelog")
                .setCollectorInstanceUid(TEST_INSTANCE_UID)
                .build();

        final var rawMessage = new RawMessage(collectorRecord.toByteArray());
        final var decoded = codecWithProcessor.decodeSafe(rawMessage);

        assertThat(decoded).isPresent();
        assertThat(decoded.get().getField(CollectorIngestCodec.FIELD_COLLECTOR_SOURCE_TYPE)).isEqualTo("filelog");
        assertThat(decoded.get().getField(EventFields.EVENT_LOG_NAME)).isEqualTo("test.log");
    }

    private static CollectorJournal.Record buildCollectorRecord(String body, String agentUid) {
        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder().setStringValue(body))
                .setTimeUnixNano(1700000000000000000L)
                .build();

        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(logRecord)
                .build();

        final var otelRecord = OTelJournal.Record.newBuilder()
                .setLog(log)
                .build();

        return CollectorJournal.Record.newBuilder()
                .setOtelRecord(otelRecord)
                .setCollectorInstanceUid(agentUid)
                .setCollectorReceiverType(TEST_RECEIVER_TYPE)
                .build();
    }
}
