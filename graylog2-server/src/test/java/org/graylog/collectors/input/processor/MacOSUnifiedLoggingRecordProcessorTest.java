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

import com.google.common.io.Resources;
import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import org.graylog.collectors.CollectorJournal;
import org.graylog.inputs.otel.OTelJournal;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MacOSUnifiedLoggingRecordProcessorTest {

    private MacOSUnifiedLoggingRecordProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new MacOSUnifiedLoggingRecordProcessor(new ObjectMapperProvider().get());
    }

    @Test
    void mapsAllSupportedBodyFields() {
        final var body = """
                {
                    "eventMessage": "Service started successfully",
                    "eventType": "logEvent",
                    "messageType": "Default",
                    "subsystem": "com.example.app",
                    "category": "lifecycle",
                    "formatString": "%{public}s started",
                    "processID": 42,
                    "userID": 501,
                    "processImagePath": "/usr/libexec/example-daemon",
                    "traceID": "AABB1122-3344-5566-7788-99AABBCCDDEE",
                    "timestamp": "2026-02-26 14:30:45.123456+0000"
                }
                """;
        final var logRecord = logRecordWithBody(body);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                Map.entry("message", "Service started successfully"),
                Map.entry("vendor_event_type", "logEvent"),
                Map.entry("vendor_event_severity", "Default"),
                Map.entry("event_source", "com.example.app"),
                Map.entry("vendor_subtype", "lifecycle"),
                Map.entry("vendor_event_description", "%{public}s started"),
                Map.entry("process_id", "42"),
                Map.entry("user_id", "501"),
                Map.entry("process_path", "/usr/libexec/example-daemon"),
                Map.entry("process_name", "example-daemon"),
                Map.entry("event_uid", "AABB1122-3344-5566-7788-99AABBCCDDEE"),
                Map.entry("vendor_event_timestamp", Instant.parse("2026-02-26T14:30:45.123456Z"))
        ));
    }

    @Test
    void returnsEmptyMapWhenBodyIsBlank() {
        final var logRecord = logRecordWithBody("");

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyMapWhenBodyIsNotJson() {
        final var logRecord = logRecordWithBody("this is not json");

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyMapWhenBodyHasNoKnownFields() {
        final var logRecord = logRecordWithBody("""
                {"unknownField": "value", "anotherUnknown": 123}
                """);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result).isEmpty();
    }

    @Test
    void handlesMinimalBody() {
        final var logRecord = logRecordWithBody("""
                {"eventMessage": "hello"}
                """);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
                "message", "hello"
        ));
    }

    @Test
    void derivesProcessNameFromPath() {
        final var logRecord = logRecordWithBody("""
                {"processImagePath": "/System/Library/Frameworks/CoreServices.framework/Versions/A/Frameworks/Metadata.framework/Versions/A/Support/mdworker_shared"}
                """);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result).containsEntry("process_name", "mdworker_shared");
        assertThat(result).containsEntry("process_path", "/System/Library/Frameworks/CoreServices.framework/Versions/A/Frameworks/Metadata.framework/Versions/A/Support/mdworker_shared");
    }

    @Test
    void handlesProcessImagePathWithoutSlash() {
        final var logRecord = logRecordWithBody("""
                {"processImagePath": "standalone-binary"}
                """);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result).containsEntry("process_name", "standalone-binary");
        assertThat(result).containsEntry("process_path", "standalone-binary");
    }

    @Test
    void skipsEmptyStringFields() {
        final var logRecord = logRecordWithBody("""
                {"eventMessage": "", "subsystem": "", "eventType": "logEvent"}
                """);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
                "vendor_event_type", "logEvent"
        ));
    }

    @Test
    void handlesNumericFieldsAsStrings() {
        final var logRecord = logRecordWithBody("""
                {"processID": 99999, "userID": 0}
                """);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result).containsEntry("process_id", "99999");
        assertThat(result).containsEntry("user_id", "0");
    }

    @Test
    void skipsUnparseableTimestamp() {
        final var logRecord = logRecordWithBody("""
                {"timestamp": "not-a-timestamp", "eventMessage": "hello"}
                """);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result).containsEntry("message", "hello");
        assertThat(result).doesNotContainKey("vendor_event_timestamp");
    }

    @Test
    void mapsFieldsFromFixture() throws IOException {
        final var logRecord = parseFixture("macos-unified-log-record.json");

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                Map.entry("message", "Connection established to endpoint 10.0.1.50:443"),
                Map.entry("vendor_event_type", "logEvent"),
                Map.entry("vendor_event_severity", "Default"),
                Map.entry("event_source", "com.example.network"),
                Map.entry("vendor_subtype", "connection"),
                Map.entry("vendor_event_description", "%{public}s connection to %{public}s:%{public}d"),
                Map.entry("process_id", "1234"),
                Map.entry("user_id", "501"),
                Map.entry("process_path", "/usr/libexec/example-daemon"),
                Map.entry("process_name", "example-daemon"),
                Map.entry("event_uid", "AABBCCDD-1122-3344-5566-778899001122"),
                Map.entry("vendor_event_timestamp", Instant.parse("2026-02-26T14:30:45.123456Z"))
        ));
    }

    @Test
    void handlesActivityCreateEvent() {
        final var logRecord = logRecordWithBody("""
                {"eventMessage": "Activity created", "eventType": "activityCreateEvent", "processID": 100}
                """);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result).containsEntry("vendor_event_type", "activityCreateEvent");
        assertThat(result).containsEntry("message", "Activity created");
        assertThat(result).containsEntry("process_id", "100");
    }

    @Test
    void handlesBodyWithNonStringValues() {
        final var logRecord = logRecordWithBody("""
                {"eventMessage": "test", "processImagePath": null}
                """);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result).containsEntry("message", "test");
        assertThat(result).doesNotContainKey("process_path");
        assertThat(result).doesNotContainKey("process_name");
    }

    private static OTelJournal.Log wrapLogRecord(LogRecord logRecord) {
        return OTelJournal.Log.newBuilder().setLogRecord(logRecord).build();
    }

    private static LogRecord logRecordWithBody(String body) {
        return LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder().setStringValue(body))
                .build();
    }

    private static LogRecord parseFixture(String filename) throws IOException {
        final var builder = CollectorJournal.Record.newBuilder();
        final var json = Resources.toString(
                Resources.getResource(MacOSUnifiedLoggingRecordProcessorTest.class, filename),
                StandardCharsets.UTF_8);
        JsonFormat.parser().merge(json, builder);
        return builder.build().getOtelRecord().getLog().getLogRecord();
    }
}
