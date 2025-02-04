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
package org.graylog.plugins.otel.input;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import org.graylog.plugins.otel.input.codec.LogsCodec;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;

class LogsCodecTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private LogsCodec codec;

    @BeforeEach
    void setUp() {
        codec = new LogsCodec(new TestMessageFactory(), new ObjectMapperProvider().get());
    }

    // Uses a modified official example that was copied from
    // https://github.com/open-telemetry/opentelemetry-proto/blob/7312bdf63218acf27fe96430b7231de37fd091f2/examples/logs.json
    // The only difference to the original is that the values trace_id and span_id have been converted from a hex
    // encoded string to a base64 encoded string in order to correctly parse the file with the generic protobuf utils
    @Test
    void decodeOfficialExample() throws IOException {
        final var decoded = codec.decode(parseFixture("logs.json"));
        assertThat(decoded).isNotEmpty();

        final var message = decoded.get();

        final Map<String, Object> expected = new HashMap<>();
        expected.put("message", "Example log record");
        expected.put("timestamp", DateTime.parse("2018-12-13T14:51:00.300Z"));
        expected.put("time_unix_nano", DateTime.parse("2018-12-13T14:51:00.300Z"));
        expected.put("observed_time_unix_nano", DateTime.parse("2018-12-13T14:51:00.300Z"));
        expected.put("severity_number", 10);
        expected.put("trace_id", "5b8efff798038103d269b633813fc60c");
        expected.put("span_id", "eee19b7ec3c1b174");
        expected.put("severity_text", "Information");
        expected.put("scope_name", "my.library");
        expected.put("scope_version", "1.0.0");
        expected.put("scope_attributes_my_scope_attribute", "some scope attribute");
        expected.put("attributes_string_attribute", "some string");
        expected.put("attributes_boolean_attribute", true);
        expected.put("attributes_int_attribute", 10L);
        expected.put("attributes_double_attribute", 637.704);
        expected.put("attributes_array_attribute", List.of("many", "values"));
        expected.put("attributes_map_attribute_some_map_key", "some value");
        expected.put("resource_attributes_service_name", "my.service");

        assertThat(message.getFields()).containsAllEntriesOf(expected);
        assertThat(message.getSource()).isNull();
    }

    @Test
    void decodeDeeplyNested() throws IOException {
        final var decoded = codec.decode(parseFixture("deeply_nested_log_record.json"));
        assertThat(decoded).isNotEmpty();
        final var message = decoded.get();

        assertThat(message.getFieldAs(String.class, "attributes_array_attribute")).isNotNull()
                .satisfies(value -> {
                    final var parsed = objectMapper.readValue(value, List.class);
                    assertThat(parsed).isEqualTo(List.of(Map.of("some.map.key", List.of("many", "values"))));
                });
    }

    @Test
    void decodeComplexResourceAndBody() throws IOException {
        final var decoded = codec.decode(parseFixture("complex_source_and_body.json"));
        assertThat(decoded).isNotEmpty();
        final var message = decoded.get();

        assertThat(message.getFields())
                .containsEntry("resource_attributes_host_name", "example.com")
                .containsEntry("resource_attributes_service_name", "my.service")
                .containsEntry("resource_attributes_map_attribute_some_map_key", "some value");
        assertThat(message.getField("source")).isEqualTo("example.com");
        assertThat(message.getFieldAs(String.class, "message")).isEqualTo(message.getMessage()).isNotNull()
                .satisfies(value -> {
                    final var parsed = objectMapper.readValue(value, List.class);
                    assertThat(parsed).isEqualTo(List.of("many", "values"));
                });
    }

    @Test
    void decodeArrayFlavors() throws IOException {
        final var decoded = codec.decode(parseFixture("log_record_array_flavors.json"));
        assertThat(decoded).isNotEmpty();
        final var message = decoded.get();

        assertThat(message.getFields()).containsEntry("attributes_string_array", List.of("a", "b", "c"));
        assertThat(message.getFields()).containsEntry("attributes_bool_array", List.of(true, false, true));
        assertThat(message.getFields()).containsEntry("attributes_int_array", List.of(1L, 2L, 3L));
        assertThat(message.getFields()).containsEntry("attributes_double_array", List.of(1.1, 2.2, 3.3));
        assertThat(message.getFields()).containsEntry("attributes_bytes_array", List.of("AQID", "BAUG", "BwgJ"));

        assertThat(message.getFieldAs(String.class, "attributes_array_array"))
                .satisfies(value -> {
                    final var parsed = objectMapper.readValue(value, new TypeReference<List<List<String>>>() {});
                    assertThat(parsed).satisfiesExactly(
                            e -> assertThat(e).isEqualTo(List.of("aa", "ab")),
                            e -> assertThat(e).isEqualTo(List.of("ba", "bb"))
                    );
                });
        assertThat(message.getFieldAs(String.class, "attributes_map_array"))
                .satisfies(value -> {
                    final var parsed = objectMapper.readValue(value, new TypeReference<List<Map<String, Object>>>() {});
                    assertThat(parsed).satisfiesExactly(
                            e -> assertThat(e).isEqualTo(Map.of("a", "b")),
                            e -> assertThat(e).isEqualTo(Map.of("b", "c"))
                    );
                });

        assertThat(message.getFields()).extracting("attributes_mixed_primitives_array", as(LIST))
                .isEqualTo(List.of("a", "true", "1", "1.1", "AQID"));

        assertThat(message.getFieldAs(String.class, "attributes_mixed_complex_array"))
                .satisfies(value -> {
                    final var parsed = objectMapper.readValue(value, List.class);
                    assertThat(parsed).isEqualTo(List.of("a", List.of("a", "b")));
                });
    }

    private Journal.Log parseFixture(String filename) throws IOException {
        final var requestBuilder = ExportLogsServiceRequest.newBuilder();
        JsonFormat.parser().merge(
                Resources.toString(Resources.getResource(
                        OpenTelemetryGrpcInput.class, filename), StandardCharsets.UTF_8),
                requestBuilder);
        return new JournalRecordFactory().createFromRequest(requestBuilder.build()).stream()
                .map(Journal.Record::getLog).findFirst().orElseThrow();
    }
}
