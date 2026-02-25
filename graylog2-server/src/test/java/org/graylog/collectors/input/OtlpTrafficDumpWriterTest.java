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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import org.graylog.collectors.CollectorJournal;
import org.graylog.inputs.otel.OTelJournal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OtlpTrafficDumpWriterTest {

    @TempDir
    Path tempDir;
    private OtlpTrafficDumpWriter writer;

    @BeforeEach
    void setUp() {
        writer = new OtlpTrafficDumpWriter(tempDir);
    }

    @Test
    void writesCollectorRecordAsNdjsonLine() throws Exception {
        final var record = buildCollectorRecord("test message", "agent-1");
        writer.write(record);
        writer.stop();

        final var dumpFile = tempDir.resolve("collector-otlp-dump.ndjson");
        assertThat(dumpFile).exists();
        final var lines = Files.readAllLines(dumpFile);
        assertThat(lines).hasSize(1);
        assertThat(lines.getFirst()).contains("test message");
        assertThat(lines.getFirst()).contains("agent-1");
    }

    @Test
    void writesMultipleRecordsAsMultipleLines() throws Exception {
        writer.write(buildCollectorRecord("msg1", "agent-1"));
        writer.write(buildCollectorRecord("msg2", "agent-2"));
        writer.stop();

        final var dumpFile = tempDir.resolve("collector-otlp-dump.ndjson");
        final var lines = Files.readAllLines(dumpFile);
        assertThat(lines).hasSize(2);
    }

    @Test
    void createsDirectoryIfNotExists() throws Exception {
        final var nestedDir = tempDir.resolve("sub/dir");
        final var writerInNested = new OtlpTrafficDumpWriter(nestedDir);
        writerInNested.write(buildCollectorRecord("test", "agent-1"));
        writerInNested.stop();

        assertThat(nestedDir.resolve("collector-otlp-dump.ndjson")).exists();
    }

    @Test
    void outputIsValidJson() throws Exception {
        final var record = buildCollectorRecord("hello world", "agent-42");
        writer.write(record);
        writer.stop();

        final var dumpFile = tempDir.resolve("collector-otlp-dump.ndjson");
        final var line = Files.readAllLines(dumpFile).getFirst();
        final var om = new ObjectMapper();
        om.readTree(line);
    }

    private CollectorJournal.Record buildCollectorRecord(String body, String agentUid) {
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
                .build();
    }
}
