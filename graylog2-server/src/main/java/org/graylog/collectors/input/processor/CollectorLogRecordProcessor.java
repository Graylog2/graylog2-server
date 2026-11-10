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

import io.opentelemetry.proto.common.v1.KeyValue;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.graylog.inputs.otel.OTelJournal;
import org.graylog.inputs.otel.codec.OTelTypeConverter;
import org.graylog.schema.EventFields;
import org.graylog.schema.ServiceFields;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import static io.opentelemetry.proto.common.v1.AnyValue.ValueCase.INT_VALUE;
import static io.opentelemetry.proto.common.v1.AnyValue.ValueCase.STRING_VALUE;

/**
 * LogRecordProcessor for collector self-logs (supervisor and OTel collector process).
 * <p>
 * Extracts resource attributes (service metadata), scope name, and log record attributes
 * that carry operational context such as endpoints, component IDs, errors, and health status.
 */
public class CollectorLogRecordProcessor implements LogRecordProcessor {
    public static final String RECEIVER_TYPE = "collector_log";

    public static final String FIELD_COLLECTOR_CERT_FINGERPRINT = "collector_cert_fingerprint";
    public static final String FIELD_COLLECTOR_COMPONENT_ID = "collector_component_id";
    public static final String FIELD_COLLECTOR_COMPONENT_KIND = "collector_component_kind";
    public static final String FIELD_COLLECTOR_CRASH_COUNT = "collector_crash_count";
    public static final String FIELD_COLLECTOR_DROPPED_RECORDS = "collector_dropped_records";
    public static final String FIELD_COLLECTOR_ENDPOINT = "collector_endpoint";
    public static final String FIELD_COLLECTOR_EXIT_CODE = "collector_exit_code";
    public static final String FIELD_COLLECTOR_LOG_ATTRIBUTES = "collector_log_attributes";
    public static final String FIELD_COLLECTOR_PATH = "collector_path";
    public static final String FIELD_COLLECTOR_REJECTED_RECORDS = "collector_rejected_records";
    public static final String FIELD_COLLECTOR_RETRY_INTERVAL = "collector_retry_interval";
    public static final String FIELD_COLLECTOR_SCOPE = "collector_scope";
    public static final String FIELD_COLLECTOR_STATUS = "collector_status";

    private final OTelTypeConverter typeConverter;

    @Inject
    public CollectorLogRecordProcessor(OTelTypeConverter typeConverter) {
        this.typeConverter = typeConverter;
    }

    @Override
    public boolean producesUnaccountedMessages() {
        return true;
    }

    @Override
    public Map<String, Object> process(OTelJournal.Log log) {
        final Map<String, Object> result = new HashMap<>();

        // ordering is important here because some extractions might look at already extracted fields from a previous
        // step
        extractResourceAttributes(log, result);
        extractScopeName(log, result);
        extractAttributes(log, result);

        return result;
    }

    private static void extractResourceAttributes(OTelJournal.Log log, Map<String, Object> result) {
        for (final var attr : log.getResource().getAttributesList()) {
            switch (attr.getKey()) {
                case "service.name" -> result.put(ServiceFields.SERVICE_NAME, attr.getValue().getStringValue()); // GIM
                case "service.version" ->
                        result.put(ServiceFields.SERVICE_VERSION, attr.getValue().getStringValue()); // GIM
            }
        }
    }

    private static void extractScopeName(OTelJournal.Log log, Map<String, Object> result) {
        final var scopeName = log.getScope().getName();
        if (!scopeName.isEmpty()) {
            result.put(FIELD_COLLECTOR_SCOPE, scopeName);
        }
    }

    private void extractAttributes(OTelJournal.Log log, Map<String, Object> result) {
        // Each attribute is either promoted to a top-level message field or preserved as-is in the
        // collector_log_attributes map. A failed promotion (wrong type) falls back to preservation.
        final var extraction = new Extraction();

        for (final var attr : log.getLogRecord().getAttributesList()) {
            switch (attr.getKey()) {
                case "endpoint" -> extraction.promoteString(FIELD_COLLECTOR_ENDPOINT, attr);
                case "exception.message" -> extraction.promoteString(EventFields.EVENT_ERROR_DESCRIPTION, attr); // GIM
                case "otelcol.component.id" -> {
                    final var componentId = extraction.promoteString(FIELD_COLLECTOR_COMPONENT_ID, attr);
                    // We extract the part before the slash (e.g. from "file_storage/default") as the GIM
                    // event_component field.
                    componentId.map(id -> StringUtils.substringBefore(id, "/"))
                            .filter(StringUtils::isNotBlank)
                            .ifPresent(component ->
                                    extraction.putPromoted(EventFields.EVENT_COMPONENT, component));
                }
                case "otelcol.component.kind" -> extraction.promoteString(FIELD_COLLECTOR_COMPONENT_KIND, attr);
                case "status" -> extraction.promoteString(FIELD_COLLECTOR_STATUS, attr);
                case "interval" -> extraction.promoteString(FIELD_COLLECTOR_RETRY_INTERVAL, attr);
                case "path" -> extraction.promoteString(FIELD_COLLECTOR_PATH, attr);
                case "cert_fingerprint" -> extraction.promoteString(FIELD_COLLECTOR_CERT_FINGERPRINT, attr);
                case "dropped_items", "dropped_log_records" ->
                        extraction.promoteLongIfAbsent(FIELD_COLLECTOR_DROPPED_RECORDS, attr);
                case "rejected_items" -> extraction.promoteLong(FIELD_COLLECTOR_REJECTED_RECORDS, attr);
                case "exit_code" -> extraction.promoteLong(FIELD_COLLECTOR_EXIT_CODE, attr);
                case "crash_count" -> extraction.promoteLong(FIELD_COLLECTOR_CRASH_COUNT, attr);
                default -> extraction.preserve(attr);
            }
        }

        // If the component ID was not present, this is most likely a log from the supervisor process. In that case
        // we will derive the event component field from the scope, which is a good fit.
        if (extraction.getPromoted(EventFields.EVENT_COMPONENT) == null) {
            final var scopeName = log.getScope().getName();
            final var serviceName = result.get(ServiceFields.SERVICE_NAME);
            if ("supervisor".equals(serviceName) && scopeName.startsWith("supervisor")) {
                extraction.putPromoted(EventFields.EVENT_COMPONENT, scopeName);
            }
        }

        extraction.writeTo(result);
    }

    private class Extraction {
        final Map<String, Object> promoted = new HashMap<>();
        final List<KeyValue> unpromoted = new ArrayList<>();

        Optional<String> promoteString(String field, KeyValue attr) {
            if (attr.getValue().getValueCase() == STRING_VALUE) {
                final var value = attr.getValue().getStringValue();
                promoted.put(field, value);
                return Optional.of(value);
            }
            unpromoted.add(attr);
            return Optional.empty();
        }

        OptionalLong promoteLong(String field, KeyValue attr) {
            if (attr.getValue().getValueCase() == INT_VALUE) {
                final var value = attr.getValue().getIntValue();
                promoted.put(field, value);
                return OptionalLong.of(value);
            }
            unpromoted.add(attr);
            return OptionalLong.empty();
        }

        OptionalLong promoteLongIfAbsent(String field, KeyValue attr) {
            if (attr.getValue().getValueCase() == INT_VALUE) {
                final var value = attr.getValue().getIntValue();
                if (promoted.putIfAbsent(field, value) == null) {
                    return OptionalLong.of(value);
                }
            }
            unpromoted.add(attr);
            return OptionalLong.empty();
        }

        void putPromoted(String field, Object value) {
            promoted.put(field, value);
        }

        Object getPromoted(String field) {
            return promoted.get(field);
        }

        void preserve(KeyValue attr) {
            unpromoted.add(attr);
        }

        void writeTo(Map<String, Object> target) {
            target.putAll(promoted);
            if (!unpromoted.isEmpty()) {
                target.put(FIELD_COLLECTOR_LOG_ATTRIBUTES, typeConverter.toJavaMap(unpromoted));
            }
        }
    }
}
