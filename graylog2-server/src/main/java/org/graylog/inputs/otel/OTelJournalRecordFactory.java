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
package org.graylog.inputs.otel;

import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;

import java.util.ArrayList;
import java.util.List;

public class OTelJournalRecordFactory {

    public List<OTelJournal.Record> createFromRequest(ExportLogsServiceRequest request) {
        final List<OTelJournal.Record> journalRecords = new ArrayList<>();
        for (ResourceLogs resourceLogs : request.getResourceLogsList()) {
            for (ScopeLogs scopeLogs : resourceLogs.getScopeLogsList()) {
                for (LogRecord logRecord : scopeLogs.getLogRecordsList()) {
                    final var journalRecord = OTelJournal.Record.newBuilder()
                            .setLog(OTelJournal.Log.newBuilder()
                                    .setResource(resourceLogs.getResource())
                                    .setResourceSchemaUrl(resourceLogs.getSchemaUrl())
                                    .setScope(scopeLogs.getScope())
                                    .setLogRecord(logRecord)
                                    .setLogRecordSchemaUrl(scopeLogs.getSchemaUrl())
                            ).build();
                    journalRecords.add(journalRecord);
                }
            }
        }
        return journalRecords;
    }
}
