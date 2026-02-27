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
import io.opentelemetry.proto.common.v1.ArrayValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.common.v1.KeyValueList;
import io.opentelemetry.proto.logs.v1.LogRecord;
import org.graylog.collectors.CollectorJournal;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JournaldRecordProcessorTest {

    private final JournaldRecordProcessor processor = new JournaldRecordProcessor();

    @Test
    void mapsSupportedBodyFields() {
        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder()
                        .setKvlistValue(KeyValueList.newBuilder()
                                .addValues(stringField("MESSAGE", "pam_unix(cron:session): session opened for user root(uid=0) by root(uid=0)"))
                                .addValues(stringField("PRIORITY", "6"))
                                .addValues(stringField("_SYSTEMD_UNIT", "cron.service"))
                                .addValues(stringField("__SEQNUM", "12219287"))
                                .build())
                        .build())
                .build();

        final var result = processor.process(logRecord);

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
                "message", "pam_unix(cron:session): session opened for user root(uid=0) by root(uid=0)",
                "vendor_event_severity_level", 6L,
                //"event_id", "12219287",
                "service_name", "cron.service"
        ));
    }

    @Test
    void dropsOutOfRangePriorityValues() {
        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder()
                        .setKvlistValue(KeyValueList.newBuilder()
                                .addValues(stringField("MESSAGE", "example"))
                                .addValues(stringField("PRIORITY", "9"))
                                .build())
                        .build())
                .build();

        final var result = processor.process(logRecord);

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
                "message", "example"
        ));
    }

    @Test
    void prefersSourceRealtimeTimestampOverRealtimeAndSyslogTimestamp() {
        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder()
                        .setKvlistValue(KeyValueList.newBuilder()
                                .addValues(stringField("_SOURCE_REALTIME_TIMESTAMP", "1772126101554715"))
                                .addValues(stringField("__REALTIME_TIMESTAMP", "1772126100000000"))
                                .addValues(stringField("SYSLOG_TIMESTAMP", "Feb 26 18:15:01 "))
                                .build())
                        .build())
                .build();

        final var result = processor.process(logRecord);

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
                "vendor_event_timestamp", new DateTime(1772126101554L, DateTimeZone.UTC)
        ));
    }

    @Test
    void normalizesSyslogTimestampToIsoWhenReliableTimestampsMissing() {
        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder()
                        .setKvlistValue(KeyValueList.newBuilder()
                                .addValues(stringField("SYSLOG_TIMESTAMP", "Feb  6 18:15:01"))
                                .build())
                        .build())
                .build();

        final var now = ZonedDateTime.now(ZoneOffset.UTC);
        var expected = ZonedDateTime.of(LocalDate.of(now.getYear(), 2, 6), LocalTime.of(18, 15, 1), ZoneOffset.UTC);
        if (expected.isAfter(now.plusDays(1))) {
            expected = expected.minusYears(1);
        }

        final var result = processor.process(logRecord);

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
                "vendor_event_timestamp", new DateTime(expected.toInstant().toEpochMilli(), DateTimeZone.UTC)
        ));
    }

    @Test
    void dropsSyslogTimestampWhenFallbackValueIsUnparseable() {
        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder()
                        .setKvlistValue(KeyValueList.newBuilder()
                                .addValues(stringField("SYSLOG_TIMESTAMP", "not-a-timestamp"))
                                .build())
                        .build())
                .build();

        final var result = processor.process(logRecord);

        assertThat(result).isEmpty();
    }

    @Test
    void fallsBackToSystemdSessionWhenAuditSessionMissing() {
        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder()
                        .setKvlistValue(KeyValueList.newBuilder()
                                .addValues(stringField("_SYSTEMD_SESSION", "42"))
                                .build())
                        .build())
                .build();

        final var result = processor.process(logRecord);

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
                "user_session_id", "42"
        ));
    }

    @Test
    void returnsEmptyMapWhenNoSupportedBodyFieldsExist() {
        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder()
                        .setKvlistValue(KeyValueList.newBuilder()
                                .addValues(stringField("UNKNOWN_KEY", "value"))
                                .build())
                        .build())
                .build();

        final var result = processor.process(logRecord);

        assertThat(result).isEmpty();
    }

    @Test
    void decodesArrayBackedMessageAndStripsAnsiSequences() {
        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder()
                        .setKvlistValue(KeyValueList.newBuilder()
                                // "\u001b[38;5;208mWARN\u001b[0m 2026-02-26 test"
                                .addValues(doubleArrayField("MESSAGE",
                                        27, 91, 51, 56, 59, 53, 59, 50, 48, 56, 109,
                                        87, 65, 82, 78,
                                        27, 91, 48, 109,
                                        32, 50, 48, 50, 54, 45, 48, 50, 45, 50, 54, 32, 116, 101, 115, 116))
                                .build())
                        .build())
                .build();

        final var result = processor.process(logRecord);

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
                "message", "WARN 2026-02-26 test"
        ));
    }

    @Test
    void dropsArrayBackedMessageWhenBytesAreNotValidUtf8Text() {
        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder()
                        .setKvlistValue(KeyValueList.newBuilder()
                                .addValues(doubleArrayField("MESSAGE", 255, 254, 253, 252))
                                .build())
                        .build())
                .build();

        final var result = processor.process(logRecord);

        assertThat(result).isEmpty();
    }

    @Test
    void decodesIntArrayBackedMessageAndStripsAnsiSequences() {
        final var logRecord = LogRecord.newBuilder()
                .setBody(AnyValue.newBuilder()
                        .setKvlistValue(KeyValueList.newBuilder()
                                // "\u001b[38;5;208mWARN\u001b[0m int-array"
                                .addValues(intArrayField("MESSAGE",
                                        27, 91, 51, 56, 59, 53, 59, 50, 48, 56, 109,
                                        87, 65, 82, 78,
                                        27, 91, 48, 109,
                                        32, 105, 110, 116, 45, 97, 114, 114, 97, 121))
                                .build())
                        .build())
                .build();

        final var result = processor.process(logRecord);

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
                "message", "WARN int-array"
        ));
    }

    @Test
    void mapsFieldsFromSyslogFixture() throws IOException {
        final var logRecord = parseFixture("journald-syslog-cron-record.json");

        final var result = processor.process(logRecord);

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                Map.entry("vendor_transaction_id", "544611aab98d4df8bd045f3b0ab794bf"),
                Map.entry("host_hostname", "h2"),
                Map.entry("vendor_event_timestamp", new DateTime(1772126101554L, DateTimeZone.UTC)),
                Map.entry("process_name", "cron"),
                Map.entry("user_id", "0"),
                //Map.entry("group_id", "0"),
                Map.entry("service_name", "cron.service"),
                Map.entry("vendor_event_category", "10"),
                Map.entry("host_id", "3d758250c1e84341a6d2037786a25bdf"),
                Map.entry("associated_user_id", "0"),
                Map.entry("user_session_id", "358"),
                Map.entry("process_path", "/usr/sbin/cron"),
                Map.entry("vendor_event_severity_level", 6L),
                Map.entry("associated_session_id", "dcae62f433304b8290b9372b7bdcde6c"),
                Map.entry("process_id", "1072719"),
                //Map.entry("event_id", "12219237"),
                Map.entry("process_command_line", "/usr/sbin/CRON -f -P"),
                Map.entry("message", "pam_unix(cron:session): session closed for user root"),
                Map.entry("event_uid", "s=544611aab98d4df8bd045f3b0ab794bf;i=ba7365;b=dcae62f433304b8290b9372b7bdcde6c;m=254aa15ad9;t=64bbd42c80329;x=e335c65713149877"),
                Map.entry("process_uid", "c5688e445fef448ea255eb3755552620"),
                //Map.entry("source_type", "system"),
                Map.entry("event_source_input", "syslog")
        ));
    }

    @Test
    void mapsFieldsFromKernelFixture() throws IOException {
        final var logRecord = parseFixture("journald-kernel-record.json");

        final var result = processor.process(logRecord);

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                Map.entry("vendor_event_severity_level", 6L),
                Map.entry("vendor_event_category", "0"),
                Map.entry("host_id", "3d758250c1e84341a6d2037786a25bdf"),
                //Map.entry("vendor_subtype", "pci"),
                //Map.entry("event_id", "12219226"),
                Map.entry("host_device", "0000:00:08.1"),
                Map.entry("host_hostname", "h2"),
                Map.entry("message", "pcieport 0000:00:08.1: PME: Spurious native interrupt!"),
                Map.entry("service_name", "kernel"),
                Map.entry("associated_session_id", "dcae62f433304b8290b9372b7bdcde6c"),
                Map.entry("vendor_transaction_id", "544611aab98d4df8bd045f3b0ab794bf"),
                Map.entry("event_uid", "s=544611aab98d4df8bd045f3b0ab794bf;i=ba735a;b=dcae62f433304b8290b9372b7bdcde6c;m=25464369ad;t=64bbd3e6a11fc;x=453c549eb08e8a1b"),
                //Map.entry("source_type", "system"),
                Map.entry("event_source_input", "kernel")
        ));
    }

    private static LogRecord parseFixture(String filename) throws IOException {
        final var builder = CollectorJournal.Record.newBuilder();
        final var json = Resources.toString(Resources.getResource(JournaldRecordProcessorTest.class, filename), StandardCharsets.UTF_8);
        JsonFormat.parser().merge(json, builder);
        return builder.build().getOtelRecord().getLog().getLogRecord();
    }

    private static KeyValue stringField(String key, String value) {
        return KeyValue.newBuilder()
                .setKey(key)
                .setValue(AnyValue.newBuilder().setStringValue(value).build())
                .build();
    }

    private static KeyValue doubleArrayField(String key, double... values) {
        final var arrayBuilder = ArrayValue.newBuilder();
        for (final var value : values) {
            arrayBuilder.addValues(AnyValue.newBuilder().setDoubleValue(value).build());
        }

        return KeyValue.newBuilder()
                .setKey(key)
                .setValue(AnyValue.newBuilder().setArrayValue(arrayBuilder).build())
                .build();
    }

    private static KeyValue intArrayField(String key, long... values) {
        final var arrayBuilder = ArrayValue.newBuilder();
        for (final var value : values) {
            arrayBuilder.addValues(AnyValue.newBuilder().setIntValue(value).build());
        }

        return KeyValue.newBuilder()
                .setKey(key)
                .setValue(AnyValue.newBuilder().setArrayValue(arrayBuilder).build())
                .build();
    }
}
