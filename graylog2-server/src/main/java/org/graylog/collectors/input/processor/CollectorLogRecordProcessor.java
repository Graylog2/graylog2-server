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

import org.graylog.inputs.otel.OTelJournal;

import java.util.HashMap;
import java.util.Map;

/**
 * LogRecordProcessor for collector self-logs (supervisor and OTel collector process).
 * <p>
 * Extracts resource attributes (service metadata), scope name, and log record attributes
 * that carry operational context such as endpoints, component IDs, errors, and health status.
 */
public class CollectorLogRecordProcessor implements LogRecordProcessor {

    public static final String RECEIVER_TYPE = "collector_log";

    @Override
    public Map<String, Object> process(OTelJournal.Log log) {
        final Map<String, Object> result = new HashMap<>();

        extractResourceAttributes(log, result);
        extractScopeName(log, result);
        extractLogRecordAttributes(log, result);

        return result;
    }

    private static void extractResourceAttributes(OTelJournal.Log log, Map<String, Object> result) {
        for (final var attr : log.getResource().getAttributesList()) {
            switch (attr.getKey()) {
                case "service.name" -> result.put("collector_service_name",
                        attr.getValue().getStringValue());
                case "service.version" -> result.put("collector_service_version",
                        attr.getValue().getStringValue());
            }
        }
    }

    private static void extractScopeName(OTelJournal.Log log, Map<String, Object> result) {
        final var scopeName = log.getScope().getName();
        if (!scopeName.isEmpty()) {
            result.put("collector_scope", scopeName);
        }
    }

    private static void extractLogRecordAttributes(OTelJournal.Log log, Map<String, Object> result) {
        for (final var attr : log.getLogRecord().getAttributesList()) {
            switch (attr.getKey()) {
                case "endpoint" -> result.put("collector_endpoint",
                        attr.getValue().getStringValue());
                case "error" -> result.put("collector_error",
                        attr.getValue().getStringValue());
                case "otelcol.component.id" -> result.put("collector_component_id",
                        attr.getValue().getStringValue());
                case "otelcol.component.kind" -> result.put("collector_component_kind",
                        attr.getValue().getStringValue());
                case "otelcol.signal" -> result.put("collector_signal",
                        attr.getValue().getStringValue());
                case "status" -> result.put("collector_status",
                        attr.getValue().getStringValue());
                case "signal" -> result.put("collector_os_signal",
                        attr.getValue().getStringValue());
                case "interval" -> result.put("collector_retry_interval",
                        attr.getValue().getStringValue());
                case "path" -> result.put("collector_path",
                        attr.getValue().getStringValue());
                case "component" -> result.put("collector_component",
                        attr.getValue().getStringValue());
                case "operator_id" -> result.put("collector_operator_id",
                        attr.getValue().getStringValue());
                case "operator_type" -> result.put("collector_operator_type",
                        attr.getValue().getStringValue());
                case "instance_uid" -> result.put("collector_supervisor_instance_uid",
                        attr.getValue().getStringValue());
                case "cert_fingerprint" -> result.put("collector_cert_fingerprint",
                        attr.getValue().getStringValue());
                default -> {
                    // Skip other attributes
                }
            }
        }
    }
}
