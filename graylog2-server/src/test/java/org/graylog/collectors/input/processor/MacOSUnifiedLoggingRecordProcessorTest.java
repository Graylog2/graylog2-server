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
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import org.graylog.collectors.CollectorJournal;
import org.graylog.inputs.otel.OTelJournal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MacOSUnifiedLoggingRecordProcessorTest {

    private MacOSUnifiedLoggingRecordProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new MacOSUnifiedLoggingRecordProcessor();
    }

    @Test
    void mapsMacosAttributesToFields() {
        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder().setStringValue("hello world"))
                .addAttributes(strAttr("macos.subsystem", "com.apple.bluetooth"))
                .addAttributes(strAttr("macos.category", "Server.LE.Scan"))
                .addAttributes(strAttr("macos.eventType", "logEvent"))
                .addAttributes(strAttr("macos.formatString", "fmt %@"))
                .addAttributes(strAttr("macos.processImagePath", "/usr/sbin/bluetoothd"))
                .addAttributes(intAttr("macos.processID", 401))
                .addAttributes(intAttr("macos.userID", 205))
                .addAttributes(intAttr("macos.threadID", 11537025L))
                .addAttributes(strAttr("macos.bootUUID", "BOOT-A"))
                .addAttributes(intAttr("macos.machTimestamp", 12868010147176L))
                .addAttributes(intAttr("macos.traceID", 45473881108119556L))
                .addAttributes(intAttr("macos.activityIdentifier", 0))
                .addAttributes(intAttr("macos.parentActivityIdentifier", 0))
                .addAttributes(intAttr("macos.creatorActivityID", 0))
                .addAttributes(strAttr("macos.processImageUUID", "PUUID"))
                .addAttributes(strAttr("macos.senderImagePath", "/usr/sbin/bluetoothd"))
                .addAttributes(strAttr("macos.senderImageUUID", "SUUID"))
                .addAttributes(intAttr("macos.senderProgramCounter", 7787736L))
                .build();

        final var result = processor.process(wrap(logRecord));

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                Map.entry("event_source", "com.apple.bluetooth"),
                Map.entry("vendor_subtype", "Server.LE.Scan"),
                Map.entry("vendor_event_type", "logEvent"),
                Map.entry("vendor_event_description", "fmt %@"),
                Map.entry("process_path", "/usr/sbin/bluetoothd"),
                Map.entry("process_name", "bluetoothd"),
                Map.entry("process_id", 401L),
                Map.entry("user_id", 205L),
                Map.entry("macos_thread_id", 11537025L),
                Map.entry("macos_boot_uuid", "BOOT-A"),
                Map.entry("macos_mach_timestamp", 12868010147176L),
                Map.entry("macos_trace_id", 45473881108119556L),
                Map.entry("macos_activity_id", 0L),
                Map.entry("macos_parent_activity_id", 0L),
                Map.entry("macos_creator_activity_id", 0L),
                Map.entry("macos_process_image_uuid", "PUUID"),
                Map.entry("macos_sender_image_path", "/usr/sbin/bluetoothd"),
                Map.entry("macos_sender_image_uuid", "SUUID"),
                Map.entry("macos_sender_program_counter", 7787736L)
        ));
    }

    @Test
    void ignoresNonMacosAttributesAndEmptyStrings() {
        final var logRecord = LogRecord.newBuilder()
                .addAttributes(strAttr("macos.subsystem", ""))
                .addAttributes(strAttr("other.key", "value"))
                .addAttributes(strAttr("macos.eventType", "logEvent"))
                .build();

        final var result = processor.process(wrap(logRecord));

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of("vendor_event_type", "logEvent"));
    }

    @Test
    void derivesProcessNameFromPathWithoutSlash() {
        final var logRecord = LogRecord.newBuilder()
                .addAttributes(strAttr("macos.processImagePath", "standalone-binary"))
                .build();

        final var result = processor.process(wrap(logRecord));

        assertThat(result).containsEntry("process_path", "standalone-binary");
        assertThat(result).containsEntry("process_name", "standalone-binary");
    }

    @Test
    void mapsFieldsFromFixture() throws IOException {
        final var logRecord = parseFixture("macos-unified-log-record.json");

        final var result = processor.process(wrap(logRecord));

        assertThat(result).containsEntry("event_source", "com.example.network");
        assertThat(result).containsEntry("vendor_subtype", "connection");
        assertThat(result).containsEntry("process_name", "example-daemon");
        assertThat(result).containsEntry("user_id", 501L);
        assertThat(result).containsEntry("macos_boot_uuid", "FFEEDDCC-BBAA-9988-7766-554433221100");
        assertThat(result).containsEntry("macos_trace_id", 45473881108119556L);
    }

    private static OTelJournal.Log wrap(LogRecord logRecord) {
        return OTelJournal.Log.newBuilder().setLogRecord(logRecord).build();
    }

    private static KeyValue strAttr(String key, String value) {
        return KeyValue.newBuilder().setKey(key)
                .setValue(AnyValue.newBuilder().setStringValue(value)).build();
    }

    private static KeyValue intAttr(String key, long value) {
        return KeyValue.newBuilder().setKey(key)
                .setValue(AnyValue.newBuilder().setIntValue(value)).build();
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
