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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import org.graylog.inputs.otel.OTelGrpcInput;
import org.graylog.inputs.otel.OTelJournal;
import org.graylog.inputs.otel.OTelJournalRecordFactory;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.ResolvableInetSocketAddress;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OTelTraceCodecTest {
    private static final String FIXED_MESSAGE_ID = UUID.randomUUID().toString();
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private OTelTraceCodec codec;

    private final ResolvableInetSocketAddress remoteAddress = ResolvableInetSocketAddress.wrap(
            new InetSocketAddress(Inet4Address.getLoopbackAddress(), 12345)
    );

    // Creates messages with a single, fixed id
    private final MessageFactory messageFactory = new TestMessageFactory() {

        @Override
        public Message createMessage(String message, String source, DateTime timestamp) {
            final var msg = super.createMessage(message, source, timestamp);
            final var fields = new HashMap<>(msg.getFields());
            fields.remove(Message.FIELD_ID);
            return super.createMessage(FIXED_MESSAGE_ID, fields);
        }
    };

    @BeforeEach
    void setUp() {
        codec = new OTelTraceCodec(messageFactory, new ObjectMapperProvider().get());
    }

    @Test
    void decodeTraceExample() throws IOException {
        final var decoded = codec.decode(parseFixture("traces.json"), new DateTime(DateTimeZone.UTC), remoteAddress);
        assertThat(decoded).isNotEmpty();

        final var message = decoded.get();

        assertThat(message.getMessage()).isEqualTo("Example span");
        assertThat(message.getTimestamp()).isEqualTo(DateTime.parse("2018-12-13T14:51:00.300Z"));
        assertThat(message.getSource()).isEqualTo("127.0.0.1");

        assertThat(message.getFields())
                .containsEntry("otel_trace_id", "5b8efff798038103d269b633813fc60c")
                .containsEntry("otel_span_id", "eee19b7ec3c1b174")
                .containsEntry("otel_parent_span_id", "0101010101010101")
                .containsEntry("otel_trace_state", "vendor1=value1")
                .containsEntry("otel_span_name", "Example span")
                .containsEntry("otel_span_kind", "SPAN_KIND_SERVER")
                .containsEntry("otel_start_time_unix_nano", 1544712660300000000L)
                .containsEntry("otel_end_time_unix_nano", 1544712660400000000L)
                .containsEntry("otel_duration_ns", 100000000L)
                .containsEntry("otel_duration_ms", 100.0)
                .containsEntry("otel_status_code", "STATUS_CODE_OK")
                .containsEntry("otel_status_message", "OK")
                .containsEntry("otel_scope_name", "my.library")
                .containsEntry("otel_scope_version", "1.0.0")
                .containsEntry("otel_scope_attributes_my_scope_attribute", "some scope attribute")
                .containsEntry("otel_attributes_string_attribute", "some string")
                .containsEntry("otel_attributes_boolean_attribute", true)
                .containsEntry("otel_attributes_int_attribute", 10L)
                .containsEntry("otel_attributes_double_attribute", 637.704)
                .containsEntry("otel_resource_attributes_service_name", "my.service")
                .containsEntry("otel_resource_schema_url", "https://opentelemetry.io/schemas/1.0.0")
                .containsEntry("otel_schema_url", "https://opentelemetry.io/schemas/1.0.0");

        // Verify events are serialized as JSON
        assertThat(message.getFieldAs(String.class, "otel_events")).isNotNull()
                .satisfies(value -> {
                    final var parsed = objectMapper.readValue(value, List.class);
                    assertThat(parsed).hasSize(1);
                    @SuppressWarnings("unchecked")
                    final var event = (Map<String, Object>) parsed.get(0);
                    assertThat(event).containsEntry("name", "exception");
                    assertThat(event).containsEntry("time_unix_nano", 1544712660350000000L);
                });

        // Verify links are serialized as JSON
        assertThat(message.getFieldAs(String.class, "otel_links")).isNotNull()
                .satisfies(value -> {
                    final var parsed = objectMapper.readValue(value, List.class);
                    assertThat(parsed).hasSize(1);
                    @SuppressWarnings("unchecked")
                    final var link = (Map<String, Object>) parsed.get(0);
                    assertThat(link).containsEntry("trace_id", "5b8efff798038103d269b633813fc60c");
                    assertThat(link).containsEntry("span_id", "eee19b7ec3c1b174");
                    assertThat(link).containsEntry("trace_state", "vendor2=value2");
                });
    }

    @Test
    void decodeMinimalTrace() throws IOException {
        final DateTime receiveTimestamp = new DateTime(2025, 2, 7, 14, 0, 0, DateTimeZone.UTC);
        final var decoded = codec.decode(parseFixture("minimal_trace.json"), receiveTimestamp, remoteAddress);
        assertThat(decoded).isNotEmpty();

        final var message = decoded.get();

        assertThat(message.getMessage()).isEqualTo("minimal");
        assertThat(message.getTimestamp()).isEqualTo(receiveTimestamp);
        assertThat(message.getSource()).isEqualTo("127.0.0.1");
        assertThat(message.getFields())
                .containsEntry("otel_trace_id", "5b8efff798038103d269b633813fc60c")
                .containsEntry("otel_span_id", "eee19b7ec3c1b174")
                .containsEntry("otel_span_name", "minimal");
    }

    @Test
    void testSource() throws IOException {
        final var address = ResolvableInetSocketAddress.wrap(
                new InetSocketAddress(Inet4Address.getLoopbackAddress(), 12345));

        var decoded = codec.decode(parseFixture("minimal_trace.json"), DateTime.now(DateTimeZone.UTC), null);
        assertThat(decoded).isNotEmpty();
        assertThat(decoded.get().getSource()).isEqualTo("unknown");

        decoded = codec.decode(parseFixture("minimal_trace.json"), DateTime.now(DateTimeZone.UTC), address);
        assertThat(decoded).isNotEmpty();
        assertThat(decoded.get().getSource()).isEqualTo("127.0.0.1");

        address.reverseLookup();

        decoded = codec.decode(parseFixture("minimal_trace.json"), DateTime.now(DateTimeZone.UTC), address);
        assertThat(decoded).isNotEmpty();
        assertThat(decoded.get().getSource()).isEqualTo("localhost");
    }

    private OTelJournal.Trace parseFixture(String filename) throws IOException {
        final var requestBuilder = ExportTraceServiceRequest.newBuilder();
        JsonFormat.parser().merge(
                Resources.toString(Resources.getResource(
                        OTelGrpcInput.class, filename), StandardCharsets.UTF_8),
                requestBuilder);
        return new OTelJournalRecordFactory().createFromTraceRequest(requestBuilder.build()).stream()
                .map(OTelJournal.Record::getTrace).findFirst().orElseThrow();
    }
}