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

import java.util.ArrayList;
import java.util.List;

public class OTelJournalRecordFactory {

    private OTelJournalRecordFactory() {}

    public static List<OTelJournal.Record> createFromRequest(ExportLogsServiceRequest request) {
        final List<OTelJournal.Record> records = new ArrayList<>();
        for (final var resourceLogs : request.getResourceLogsList()) {
            for (final var scopeLogs : resourceLogs.getScopeLogsList()) {
                for (final var logRecord : scopeLogs.getLogRecordsList()) {
                    records.add(OTelJournal.Record.newBuilder()
                            .setLog(OTelJournal.Log.newBuilder()
                                    .setResource(resourceLogs.getResource())
                                    .setResourceSchemaUrl(resourceLogs.getSchemaUrl())
                                    .setScope(scopeLogs.getScope())
                                    .setLogRecord(logRecord)
                                    .setLogRecordSchemaUrl(scopeLogs.getSchemaUrl()))
                            .build());
                }
            }
        }
        return records;
    }
}
