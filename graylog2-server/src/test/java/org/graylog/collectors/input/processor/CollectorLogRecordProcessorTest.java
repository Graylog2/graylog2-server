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
    void extractsServiceName() {
        // TODO: Update once we finalize the schema with real collector self-log payloads.
        final var log = OTelJournal.Log.newBuilder()
                .setLogRecord(LogRecord.newBuilder().build())
                .setResource(Resource.newBuilder()
                        .addAttributes(KeyValue.newBuilder()
                                .setKey("service.name")
                                .setValue(AnyValue.newBuilder().setStringValue("otel-collector").build())
                                .build())
                        .build())
                .build();
        final var result = processor.process(log);
        assertThat(result).containsEntry("collector_service_name", "otel-collector");
    }
}
