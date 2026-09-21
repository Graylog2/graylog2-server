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
package org.graylog.collectors.input.processor;

import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.InstrumentationScope;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.common.v1.KeyValueList;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.resource.v1.Resource;
import org.graylog.inputs.otel.OTelJournal;
import org.graylog.inputs.otel.codec.OTelTypeConverter;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CollectorLogRecordProcessorTest {

    // Instrumentation scope of records emitted by unnamed zap loggers (otelzap's fallback
    // instrumentation name), carried by all embedded-collector records.
    private static final String MODULE_PATH_SCOPE = "github.com/Graylog2/collector/superv";

    private final CollectorLogRecordProcessor processor =
            new CollectorLogRecordProcessor(new OTelTypeConverter(new ObjectMapperProvider().get()));

    @Test
    void producesUnaccountedMessages() {
        // Collector self-logs are Graylog-internal telemetry and must not count against the license.
        assertThat(processor.producesUnaccountedMessages()).isTrue();
    }

    @Test
    void processesEmptyLogRecord() {
        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(LogRecord.newBuilder().build())
                .build();
        assertThat(processor.process(log)).isEmpty();
    }

    // --- resource attributes ---

    @Test
    void promotesServiceResourceAttributes() {
        final var log = logWithResource(
                kv("service.name", "supervisor"),
                kv("service.version", "2.0.0-SNAPSHOT+54f4e66"));
        assertThat(processor.process(log))
                .containsEntry("service_name", "supervisor")
                .containsEntry("service_version", "2.0.0-SNAPSHOT+54f4e66");
    }

    @Test
    void skipsResourceAttributesDuplicatedByCodecFields() {
        // service.instance.id and collector.receiver.type are already covered by the
        // codec-written agent_id/agent_receiver_type and must be mapped nowhere.
        final var log = logWithResource(
                kv("service.instance.id", "f33b59b9-fa59-47a9-908e-73cda4ac5695"),
                kv("collector.receiver.type", "collector_log"));
        assertThat(processor.process(log)).isEmpty();
    }

    // --- scope ---

    @Test
    void extractsScopeName() {
        final var log = logWith(scope("supervisor.opamp"), List.of());
        assertThat(processor.process(log)).containsEntry("collector_scope", "supervisor.opamp");
    }

    @Test
    void doesNotExtractEmptyScopeName() {
        final var log = logWith(InstrumentationScope.newBuilder().build(), List.of());
        assertThat(processor.process(log)).doesNotContainKey("collector_scope");
    }

    // --- event_component ---

    @Test
    void derivesEventComponentFromComponentIdPrefix() {
        final var log = logWithAttrs(kv("otelcol.component.id", "file_log/69dfb9d78b3d36fe13d10ef4"));
        assertThat(processor.process(log))
                .containsEntry("event_component", "file_log")
                // the full component id stays available for exact attribution
                .containsEntry("collector_component_id", "file_log/69dfb9d78b3d36fe13d10ef4");
    }

    @Test
    void usesWholeComponentIdAsEventComponentWithoutSlash() {
        final var log = logWithAttrs(kv("otelcol.component.id", "otlp_http"));
        assertThat(processor.process(log)).containsEntry("event_component", "otlp_http");
    }

    @Test
    void derivesEventComponentFromSupervisorScope() {
        final var log = OTelJournal.Log.newBuilder()
                .setResource(Resource.newBuilder().addAttributes(kv("service.name", "supervisor")))
                .setScope(scope("supervisor.auth"))
                .setLogRecord(LogRecord.newBuilder().build())
                .build();
        assertThat(processor.process(log)).containsEntry("event_component", "supervisor.auth");
    }

    @Test
    void doesNotDeriveEventComponentFromModulePathScope() {
        // The few supervisor log calls that run before the root logger is named carry the
        // otelzap fallback scope, which identifies no subsystem.
        final var log = OTelJournal.Log.newBuilder()
                .setResource(Resource.newBuilder().addAttributes(kv("service.name", "supervisor")))
                .setScope(scope(MODULE_PATH_SCOPE))
                .setLogRecord(LogRecord.newBuilder().build())
                .build();
        assertThat(processor.process(log)).doesNotContainKey("event_component");
    }

    @Test
    void doesNotDeriveEventComponentFromScopeOfOtherServices() {
        // "archive" is a real logger name in the embedded collector (stanza), but scope only
        // identifies a subsystem for supervisor-process records.
        final var log = OTelJournal.Log.newBuilder()
                .setResource(Resource.newBuilder().addAttributes(kv("service.name", "collector")))
                .setScope(scope("archive"))
                .setLogRecord(LogRecord.newBuilder().build())
                .build();
        assertThat(processor.process(log)).doesNotContainKey("event_component");
    }

    // --- simple promotions ---

    @Test
    void promotesOperationalStringAttributes() {
        final var log = logWithAttrs(
                kv("endpoint", "https://opamp.example.com/v1/opamp"),
                kv("exception.message", "connection refused"),
                kv("otelcol.component.kind", "receiver"),
                kv("status", "StatusRecoverableError"),
                kv("interval", "21.9s"),
                kv("path", "/var/log/syslog"),
                kv("cert_fingerprint", "ab:cd:ef"));
        assertThat(processor.process(log))
                .containsEntry("collector_endpoint", "https://opamp.example.com/v1/opamp")
                .containsEntry("event_error_description", "connection refused")
                .containsEntry("collector_component_kind", "receiver")
                .containsEntry("collector_status", "StatusRecoverableError")
                .containsEntry("collector_retry_interval", "21.9s")
                .containsEntry("collector_path", "/var/log/syslog")
                .containsEntry("collector_cert_fingerprint", "ab:cd:ef")
                .doesNotContainKey("collector_log_attributes");
    }

    @Test
    void preservesWrongTypedStringAttribute() {
        // A non-string value on a string-typed field is not promoted; the raw value must
        // survive in the attributes map instead.
        final var log = logWithAttrs(kvLong("endpoint", 42));
        final var result = processor.process(log);
        assertThat(result).doesNotContainKey("collector_endpoint");
        assertThat(preservedAttributes(result)).containsEntry("endpoint", 42L);
    }

    // --- counters ---

    @Test
    void promotesDroppedItemsToDroppedRecords() {
        final var log = logWithAttrs(kvLong("dropped_items", 17));
        assertThat(processor.process(log)).containsEntry("collector_dropped_records", 17L);
    }

    @Test
    void promotesDroppedLogRecordsToDroppedRecords() {
        final var log = logWithAttrs(kvLong("dropped_log_records", 3));
        assertThat(processor.process(log)).containsEntry("collector_dropped_records", 3L);
    }

    @Test
    void preservesLosingDroppedCounterWhenBothArePresent() {
        // The two loss counters never co-occur at the current collector version. If a future
        // version emits both, the first must win and the loser must survive in the attributes map.
        final var log = logWithAttrs(kvLong("dropped_items", 17), kvLong("dropped_log_records", 3));
        final var result = processor.process(log);
        assertThat(result).containsEntry("collector_dropped_records", 17L);
        assertThat(preservedAttributes(result)).containsEntry("dropped_log_records", 3L);
    }

    @Test
    void promotesRejectedItemsToSeparateField() {
        // Rejections are backpressure, not data loss, and must not be folded into the
        // dropped counter.
        final var log = logWithAttrs(kvLong("rejected_items", 8));
        assertThat(processor.process(log))
                .containsEntry("collector_rejected_records", 8L)
                .doesNotContainKey("collector_dropped_records");
    }

    @Test
    void promotesProcessLifecycleCounters() {
        final var log = logWithAttrs(kvLong("exit_code", 2), kvLong("crash_count", 4));
        assertThat(processor.process(log))
                .containsEntry("collector_exit_code", 2L)
                .containsEntry("collector_crash_count", 4L);
    }

    @Test
    void preservesNonIntegerCounterValue() {
        // Counter fields are mapped as long in the index; anything but an integer value is
        // not promoted (a numeric string would otherwise index a misleading value).
        final var log = logWithAttrs(kv("dropped_items", "17"));
        final var result = processor.process(log);
        assertThat(result).doesNotContainKey("collector_dropped_records");
        assertThat(preservedAttributes(result)).containsEntry("dropped_items", "17");
    }

    // --- preserved attributes map ---

    @Test
    void preservesUnknownAttributesInAttributesMap() {
        final var log = logWithAttrs(
                kv("code.file.path", "/build/supervisor/auth.go"),
                kvLong("code.line.number", 126),
                kv("otelcol.signal", "logs"));
        assertThat(preservedAttributes(processor.process(log)))
                .containsEntry("code.file.path", "/build/supervisor/auth.go")
                .containsEntry("code.line.number", 126L)
                .containsEntry("otelcol.signal", "logs");
    }

    @Test
    void omitsAttributesMapWhenAllAttributesArePromoted() {
        final var log = logWithAttrs(kv("endpoint", "https://example.com"));
        assertThat(processor.process(log)).doesNotContainKey("collector_log_attributes");
    }

    @Test
    void preservesEveryAttributeValueExactlyOnce() {
        // Promoted values must not additionally appear in the attributes map, and unpromoted
        // values must not be lost.
        final var log = logWithAttrs(
                kv("endpoint", "https://example.com"),
                kv("exception.message", "boom"),
                kv("cursor", "abc123"),
                kvLong("bytes", 4096));
        final var result = processor.process(log);
        assertThat(result)
                .containsEntry("collector_endpoint", "https://example.com")
                .containsEntry("event_error_description", "boom");
        assertThat(preservedAttributes(result)).containsOnlyKeys("cursor", "bytes")
                .containsEntry("cursor", "abc123")
                .containsEntry("bytes", 4096L);
    }

    @Test
    void preservesNestedAttributeValuesInAttributesMap() {
        final var nested = AnyValue.newBuilder()
                .setKvlistValue(KeyValueList.newBuilder().addValues(kv("inner", "value")))
                .build();
        final var log = logWithAttrs(KeyValue.newBuilder().setKey("outer").setValue(nested).build());
        assertThat(preservedAttributes(processor.process(log)))
                .containsEntry("outer", Map.of("inner", "value"));
    }

    // --- helpers ---

    @SuppressWarnings("unchecked")
    private Map<String, Object> preservedAttributes(Map<String, Object> result) {
        assertThat(result).containsKey("collector_log_attributes");
        assertThat(result.get("collector_log_attributes")).isInstanceOf(Map.class);
        return (Map<String, Object>) result.get("collector_log_attributes");
    }

    private static KeyValue kv(String key, String value) {
        return KeyValue.newBuilder().setKey(key)
                .setValue(AnyValue.newBuilder().setStringValue(value)).build();
    }

    private static KeyValue kvLong(String key, long value) {
        return KeyValue.newBuilder().setKey(key)
                .setValue(AnyValue.newBuilder().setIntValue(value)).build();
    }

    private static InstrumentationScope scope(String name) {
        return InstrumentationScope.newBuilder().setName(name).build();
    }

    private static OTelJournal.Log logWithResource(KeyValue... resourceAttrs) {
        return OTelJournal.Log.newBuilder()
                .setResource(Resource.newBuilder().addAllAttributes(List.of(resourceAttrs)))
                .setLogRecord(LogRecord.newBuilder().build())
                .build();
    }

    private static OTelJournal.Log logWithAttrs(KeyValue... attrs) {
        return OTelJournal.Log.newBuilder()
                .setLogRecord(LogRecord.newBuilder().addAllAttributes(List.of(attrs)))
                .build();
    }

    private static OTelJournal.Log logWith(InstrumentationScope scope, List<KeyValue> attrs) {
        return OTelJournal.Log.newBuilder()
                .setScope(scope)
                .setLogRecord(LogRecord.newBuilder().addAllAttributes(attrs))
                .build();
    }
}
