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

import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import io.opentelemetry.proto.resource.v1.Resource;
import org.graylog.collectors.config.OtelAttributes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CollectorJournalRecordFactoryTest {

    @Test
    void setsInstanceUidAndReceiverType() {
        final var request = ExportLogsServiceRequest.newBuilder()
                .addResourceLogs(ResourceLogs.newBuilder()
                        .setResource(Resource.newBuilder()
                                .addAttributes(KeyValue.newBuilder()
                                        .setKey(OtelAttributes.COLLECTOR_RECEIVER_TYPE)
                                        .setValue(AnyValue.newBuilder().setStringValue("filelog"))))
                        .addScopeLogs(ScopeLogs.newBuilder()
                                .addLogRecords(LogRecord.newBuilder()
                                        .setBody(AnyValue.newBuilder().setStringValue("hello")))))
                .build();

        final var records = CollectorJournalRecordFactory.createFromRequest(request, "agent-42");

        assertThat(records).hasSize(1);
        final var record = records.get(0);
        assertThat(record.getCollectorInstanceUid()).isEqualTo("agent-42");
        assertThat(record.getCollectorReceiverType()).isEqualTo("filelog");
        assertThat(record.getOtelRecord().getLog().getLogRecord().getBody().getStringValue()).isEqualTo("hello");
    }

    @Test
    void receiverTypeDefaultsToEmptyWhenAttributeMissing() {
        final var request = ExportLogsServiceRequest.newBuilder()
                .addResourceLogs(ResourceLogs.newBuilder()
                        .addScopeLogs(ScopeLogs.newBuilder()
                                .addLogRecords(LogRecord.newBuilder()
                                        .setBody(AnyValue.newBuilder().setStringValue("no receiver type")))))
                .build();

        final var records = CollectorJournalRecordFactory.createFromRequest(request, "agent-1");

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getCollectorReceiverType()).isEmpty();
    }

    @Test
    void multipleResourceLogsWithDifferentReceiverTypes() {
        final var request = ExportLogsServiceRequest.newBuilder()
                .addResourceLogs(ResourceLogs.newBuilder()
                        .setResource(Resource.newBuilder()
                                .addAttributes(KeyValue.newBuilder()
                                        .setKey(OtelAttributes.COLLECTOR_RECEIVER_TYPE)
                                        .setValue(AnyValue.newBuilder().setStringValue("filelog"))))
                        .addScopeLogs(ScopeLogs.newBuilder()
                                .addLogRecords(LogRecord.newBuilder()
                                        .setBody(AnyValue.newBuilder().setStringValue("file log")))))
                .addResourceLogs(ResourceLogs.newBuilder()
                        .setResource(Resource.newBuilder()
                                .addAttributes(KeyValue.newBuilder()
                                        .setKey(OtelAttributes.COLLECTOR_RECEIVER_TYPE)
                                        .setValue(AnyValue.newBuilder().setStringValue("macosunifiedlogging"))))
                        .addScopeLogs(ScopeLogs.newBuilder()
                                .addLogRecords(LogRecord.newBuilder()
                                        .setBody(AnyValue.newBuilder().setStringValue("macos log")))))
                .build();

        final var records = CollectorJournalRecordFactory.createFromRequest(request, "agent-1");

        assertThat(records).hasSize(2);
        assertThat(records.get(0).getCollectorReceiverType()).isEqualTo("filelog");
        assertThat(records.get(1).getCollectorReceiverType()).isEqualTo("macosunifiedlogging");
    }
}
