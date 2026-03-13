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
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.resource.v1.Resource;
import org.graylog.inputs.otel.OTelJournal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CollectorLogRecordProcessorTest {

    private final CollectorLogRecordProcessor processor = new CollectorLogRecordProcessor();

    @Test
    void processesEmptyLogRecord() {
        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(LogRecord.newBuilder().build())
                .build();
        final var result = processor.process(log);
        assertThat(result).isEmpty();
    }

    @Test
    void extractsResourceAttributes() {
        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(LogRecord.newBuilder().build())
                .setResource(Resource.newBuilder()
                        .addAttributes(kv("service.name", "supervisor"))
                        .addAttributes(kv("service.version", "2.0.0-SNAPSHOT+54f4e66"))
                        .build())
                .build();
        final var result = processor.process(log);
        assertThat(result)
                .containsEntry("collector_service_name", "supervisor")
                .containsEntry("collector_service_version", "2.0.0-SNAPSHOT+54f4e66");
    }

    @Test
    void extractsScopeName() {
        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(LogRecord.newBuilder().build())
                .setScope(InstrumentationScope.newBuilder().setName("supervisor").build())
                .build();
        final var result = processor.process(log);
        assertThat(result).containsEntry("collector_scope", "supervisor");
    }

    @Test
    void doesNotExtractEmptyScopeName() {
        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(LogRecord.newBuilder().build())
                .setScope(InstrumentationScope.newBuilder().build())
                .build();
        final var result = processor.process(log);
        assertThat(result).doesNotContainKey("collector_scope");
    }

    @Test
    void extractsOpAMPConnectionAttributes() {
        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(LogRecord.newBuilder()
                        .addAttributes(kv("endpoint", "https://opamp.example.com/v1/opamp"))
                        .addAttributes(kv("code.file.path", "/some/path.go"))
                        .addAttributes(kv("code.line.number", "1097"))
                        .build())
                .build();
        final var result = processor.process(log);
        assertThat(result)
                .containsEntry("collector_endpoint", "https://opamp.example.com/v1/opamp")
                .doesNotContainKey("code.file.path")
                .doesNotContainKey("code.line.number");
    }

    @Test
    void extractsErrorAttributes() {
        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(LogRecord.newBuilder()
                        .addAttributes(kv("error", "connection refused"))
                        .addAttributes(kv("interval", "3.5s"))
                        .addAttributes(kv("otelcol.component.id", "otlp_grpc"))
                        .addAttributes(kv("otelcol.component.kind", "exporter"))
                        .addAttributes(kv("otelcol.signal", "logs"))
                        .build())
                .build();
        final var result = processor.process(log);
        assertThat(result)
                .containsEntry("collector_error", "connection refused")
                .containsEntry("collector_retry_interval", "3.5s")
                .containsEntry("collector_component_id", "otlp_grpc")
                .containsEntry("collector_component_kind", "exporter")
                .containsEntry("collector_signal", "logs");
    }

    @Test
    void extractsHealthCheckAttributes() {
        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(LogRecord.newBuilder()
                        .addAttributes(kv("otelcol.component.id", "health_check"))
                        .addAttributes(kv("otelcol.component.kind", "extension"))
                        .addAttributes(kv("status", "unavailable"))
                        .build())
                .build();
        final var result = processor.process(log);
        assertThat(result)
                .containsEntry("collector_component_id", "health_check")
                .containsEntry("collector_component_kind", "extension")
                .containsEntry("collector_status", "unavailable");
    }

    @Test
    void extractsReceiverLifecycleAttributes() {
        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(LogRecord.newBuilder()
                        .addAttributes(kv("otelcol.component.id", "filelog/699c94e23f694890ac6bd6c9"))
                        .addAttributes(kv("otelcol.component.kind", "receiver"))
                        .addAttributes(kv("otelcol.signal", "logs"))
                        .addAttributes(kv("component", "fileconsumer"))
                        .addAttributes(kv("path", "/var/log/syslog"))
                        .build())
                .build();
        final var result = processor.process(log);
        assertThat(result)
                .containsEntry("collector_component_id", "filelog/699c94e23f694890ac6bd6c9")
                .containsEntry("collector_component_kind", "receiver")
                .containsEntry("collector_signal", "logs")
                .containsEntry("collector_component", "fileconsumer")
                .containsEntry("collector_path", "/var/log/syslog");
    }

    @Test
    void extractsOsSignal() {
        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(LogRecord.newBuilder()
                        .addAttributes(kv("signal", "terminated"))
                        .build())
                .build();
        final var result = processor.process(log);
        assertThat(result).containsEntry("collector_os_signal", "terminated");
    }

    @Test
    void extractsJournalctlErrorAttributes() {
        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(LogRecord.newBuilder()
                        .addAttributes(kv("error", "signal: terminated"))
                        .addAttributes(kv("operator_id", "journald_input"))
                        .addAttributes(kv("operator_type", "journald_input"))
                        .addAttributes(kv("otelcol.component.id", "journald/69a2dfee"))
                        .addAttributes(kv("otelcol.component.kind", "receiver"))
                        .addAttributes(kv("otelcol.signal", "logs"))
                        .build())
                .build();
        final var result = processor.process(log);
        assertThat(result)
                .containsEntry("collector_error", "signal: terminated")
                .containsEntry("collector_operator_id", "journald_input")
                .containsEntry("collector_operator_type", "journald_input");
    }

    @Test
    void extractsSupervisorStartupAttributes() {
        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(LogRecord.newBuilder()
                        .addAttributes(kv("instance_uid", "ab355310-58f8-4af5-a496-7d9c31413c8a"))
                        .addAttributes(kv("endpoint", "https://opamp.example.com/v1/opamp"))
                        .build())
                .build();
        final var result = processor.process(log);
        assertThat(result)
                .containsEntry("collector_supervisor_instance_uid", "ab355310-58f8-4af5-a496-7d9c31413c8a")
                .containsEntry("collector_endpoint", "https://opamp.example.com/v1/opamp");
    }

    @Test
    void extractsCredentialsAndDiscoveryAttributes() {
        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(LogRecord.newBuilder()
                        .addAttributes(kv("cert_fingerprint", "df568c6dc083eebdbc3054493c984b6a"))
                        .build())
                .build();
        final var result = processor.process(log);
        assertThat(result).containsEntry("collector_cert_fingerprint", "df568c6dc083eebdbc3054493c984b6a");
    }

    @Test
    void extractsAllFieldsFromFullRecord() {
        final var log = OTelJournal.Log.newBuilder()
                .setResource(Resource.newBuilder()
                        .addAttributes(kv("service.name", "supervisor"))
                        .addAttributes(kv("service.version", "2.0.0-SNAPSHOT"))
                        .addAttributes(kv("collector.receiver.type", "collector_log"))
                        .build())
                .setScope(InstrumentationScope.newBuilder().setName("supervisor").build())
                .setLogRecord(LogRecord.newBuilder()
                        .addAttributes(kv("endpoint", "https://opamp.example.com/v1/opamp"))
                        .addAttributes(kv("code.file.path", "/src/supervisor.go"))
                        .addAttributes(kv("code.function.name", "some.func"))
                        .addAttributes(kv("code.line.number", "123"))
                        .build())
                .build();
        final var result = processor.process(log);
        assertThat(result)
                .hasSize(4)
                .containsEntry("collector_service_name", "supervisor")
                .containsEntry("collector_service_version", "2.0.0-SNAPSHOT")
                .containsEntry("collector_scope", "supervisor")
                .containsEntry("collector_endpoint", "https://opamp.example.com/v1/opamp");
    }

    private static KeyValue kv(String key, String value) {
        return KeyValue.newBuilder()
                .setKey(key)
                .setValue(AnyValue.newBuilder().setStringValue(value).build())
                .build();
    }
}
