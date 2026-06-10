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
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import org.graylog.collectors.CollectorJournal;
import org.graylog.collectors.config.CollectorAttributes;
import org.graylog.inputs.otel.OTelJournal;

import java.util.ArrayList;
import java.util.List;

public class CollectorJournalRecordFactory {

    private CollectorJournalRecordFactory() {
    }

    public static List<CollectorJournal.Record> createFromRequest(ExportLogsServiceRequest request,
                                                                  String instanceUid) {
        final List<CollectorJournal.Record> records = new ArrayList<>();
        for (final var resourceLogs : request.getResourceLogsList()) {
            final var attributes = extractResourceAttributes(resourceLogs);
            for (final var scopeLogs : resourceLogs.getScopeLogsList()) {
                for (final var logRecord : scopeLogs.getLogRecordsList()) {
                    records.add(CollectorJournal.Record.newBuilder()
                            .setOtelRecord(OTelJournal.Record.newBuilder()
                                    .setLog(OTelJournal.Log.newBuilder()
                                            .setResource(resourceLogs.getResource())
                                            .setResourceSchemaUrl(resourceLogs.getSchemaUrl())
                                            .setScope(scopeLogs.getScope())
                                            .setLogRecord(logRecord)
                                            .setLogRecordSchemaUrl(scopeLogs.getSchemaUrl())))
                            .setCollectorInstanceUid(instanceUid)
                            .setCollectorReceiverType(attributes.receiverType())
                            .setCollectorSourceId(attributes.sourceId())
                            .setCollectorFleetId(attributes.fleetId())
                            .build());
                }
            }
        }
        return records;
    }

    private record ResourceAttributes(String receiverType, String sourceId, String fleetId) {}

    private static ResourceAttributes extractResourceAttributes(ResourceLogs resourceLogs) {
        String receiverType = "";
        String sourceId = "";
        String fleetId = "";
        for (final var attr : resourceLogs.getResource().getAttributesList()) {
            switch (attr.getKey()) {
                case CollectorAttributes.COLLECTOR_RECEIVER_TYPE -> receiverType = attr.getValue().getStringValue();
                case CollectorAttributes.COLLECTOR_SOURCE_ID -> sourceId = attr.getValue().getStringValue();
                case CollectorAttributes.COLLECTOR_FLEET_ID -> fleetId = attr.getValue().getStringValue();
            }
        }
        return new ResourceAttributes(receiverType, sourceId, fleetId);
    }
}
