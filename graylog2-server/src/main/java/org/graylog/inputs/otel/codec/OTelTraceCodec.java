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
package org.graylog.inputs.otel.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.InetAddresses;
import com.google.protobuf.ByteString;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.ArrayValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.common.v1.KeyValueList;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.graylog.inputs.otel.OTelJournal;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.ResolvableInetSocketAddress;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.opentelemetry.proto.common.v1.AnyValue.ValueCase.ARRAY_VALUE;
import static io.opentelemetry.proto.common.v1.AnyValue.ValueCase.KVLIST_VALUE;
import static org.slf4j.LoggerFactory.getLogger;

public class OTelTraceCodec {
    private static final Logger LOG = getLogger(OTelTraceCodec.class);
    private static final ByteString INVALID_TRACE_ID = ByteString.copyFrom(new byte[16]);
    private static final ByteString INVALID_SPAN_ID = ByteString.copyFrom(new byte[8]);

    private final MessageFactory messageFactory;
    private final ObjectMapper objectMapper;

    @Inject
    public OTelTraceCodec(MessageFactory messageFactory, ObjectMapper objectMapper) {
        this.messageFactory = messageFactory;
        this.objectMapper = objectMapper;
    }

    public Optional<Message> decode(@Nonnull OTelJournal.Trace trace, DateTime receiveTimestamp, ResolvableInetSocketAddress remoteAddress) {
        final var span = trace.getSpan();

        if (LOG.isTraceEnabled()) {
            LOG.trace("Decoding span: {}", span);
        }

        final var body = StringUtils.isNotBlank(span.getName()) ? span.getName() : "unnamed span";
        final var source = remoteAddress == null ? "unknown" : source(remoteAddress);
        final var timestamp = timestamp(span).orElse(receiveTimestamp);

        final var message = messageFactory.createMessage(body, source, timestamp);

        final var fieldMap = transformToMessageFields(trace, span);

        message.addFields(fieldMap);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Parsed message: {}", message);
        }

        return Optional.of(message);
    }

    private HashMap<String, Object> transformToMessageFields(OTelJournal.Trace trace, Span span) {
        final var fieldMap = new HashMap<String, Object>();

        // Trace and span IDs
        if (span.getTraceId().size() == 16 && !span.getTraceId().equals(INVALID_TRACE_ID)) {
            fieldMap.put("otel_trace_id", Hex.encodeHexString(span.getTraceId().toByteArray()));
        }
        if (span.getSpanId().size() == 8 && !span.getSpanId().equals(INVALID_SPAN_ID)) {
            fieldMap.put("otel_span_id", Hex.encodeHexString(span.getSpanId().toByteArray()));
        }
        if (span.getParentSpanId().size() == 8 && !span.getParentSpanId().equals(INVALID_SPAN_ID)) {
            fieldMap.put("otel_parent_span_id", Hex.encodeHexString(span.getParentSpanId().toByteArray()));
        }

        // Trace state
        if (StringUtils.isNotBlank(span.getTraceState())) {
            fieldMap.put("otel_trace_state", span.getTraceState());
        }

        // Span name
        if (StringUtils.isNotBlank(span.getName())) {
            fieldMap.put("otel_span_name", span.getName());
        }

        // Span kind
        if (span.getKindValue() > 0) {
            fieldMap.put("otel_span_kind", span.getKind().name());
        }

        // Timestamps
        if (span.getStartTimeUnixNano() > 0) {
            fieldMap.put("otel_start_time_unix_nano", span.getStartTimeUnixNano());
        }
        if (span.getEndTimeUnixNano() > 0) {
            fieldMap.put("otel_end_time_unix_nano", span.getEndTimeUnixNano());
        }

        // Duration
        if (span.getStartTimeUnixNano() > 0 && span.getEndTimeUnixNano() > 0) {
            final long durationNs = span.getEndTimeUnixNano() - span.getStartTimeUnixNano();
            fieldMap.put("otel_duration_ns", durationNs);
            fieldMap.put("otel_duration_ms", durationNs / 1_000_000.0);
        }

        // Flags
        if (span.getFlags() > 0) {
            fieldMap.put("otel_flags", span.getFlags());
        }

        // Status
        if (span.hasStatus()) {
            final Status status = span.getStatus();
            if (status.getCodeValue() > 0) {
                fieldMap.put("otel_status_code", status.getCode().name());
            }
            if (StringUtils.isNotBlank(status.getMessage())) {
                fieldMap.put("otel_status_message", status.getMessage());
            }
        }

        // Dropped counts
        if (span.getDroppedAttributesCount() > 0) {
            fieldMap.put("otel_dropped_attributes_count", span.getDroppedAttributesCount());
        }
        if (span.getDroppedEventsCount() > 0) {
            fieldMap.put("otel_dropped_events_count", span.getDroppedEventsCount());
        }
        if (span.getDroppedLinksCount() > 0) {
            fieldMap.put("otel_dropped_links_count", span.getDroppedLinksCount());
        }

        // Schema URLs
        if (StringUtils.isNotBlank(trace.getResourceSchemaUrl())) {
            fieldMap.put("otel_resource_schema_url", trace.getResourceSchemaUrl());
        }
        if (StringUtils.isNotBlank(trace.getSpanSchemaUrl())) {
            fieldMap.put("otel_schema_url", trace.getSpanSchemaUrl());
        }

        // Scope
        if (trace.hasScope()) {
            final var scope = trace.getScope();
            if (StringUtils.isNotBlank(scope.getName())) {
                fieldMap.put("otel_scope_name", scope.getName());
            }
            if (StringUtils.isNotBlank(scope.getVersion())) {
                fieldMap.put("otel_scope_version", scope.getVersion());
            }
            convertKvList("otel_scope_attributes", scope.getAttributesList())
                    .forEach(f -> fieldMap.put(f.getKey(), f.getValue()));
        }

        // Resource attributes
        convertKvList("otel_resource_attributes", trace.getResource().getAttributesList())
                .forEach(f -> fieldMap.put(f.getKey(), f.getValue()));

        // Span attributes
        convertKvList("otel_attributes", span.getAttributesList())
                .forEach(f -> fieldMap.put(f.getKey(), f.getValue()));

        // Events (as JSON)
        if (!span.getEventsList().isEmpty()) {
            final List<Map<String, Object>> events = new ArrayList<>();
            for (Span.Event event : span.getEventsList()) {
                final Map<String, Object> eventMap = new LinkedHashMap<>();
                eventMap.put("time_unix_nano", event.getTimeUnixNano());
                eventMap.put("name", event.getName());
                if (!event.getAttributesList().isEmpty()) {
                    eventMap.put("attributes", attributesToMap(event.getAttributesList()));
                }
                if (event.getDroppedAttributesCount() > 0) {
                    eventMap.put("dropped_attributes_count", event.getDroppedAttributesCount());
                }
                events.add(eventMap);
            }
            asJson("otel_events", events).ifPresent(json -> fieldMap.put("otel_events", json));
        }

        // Links (as JSON)
        if (!span.getLinksList().isEmpty()) {
            final List<Map<String, Object>> links = new ArrayList<>();
            for (Span.Link link : span.getLinksList()) {
                final Map<String, Object> linkMap = new LinkedHashMap<>();
                if (link.getTraceId().size() == 16 && !link.getTraceId().equals(INVALID_TRACE_ID)) {
                    linkMap.put("trace_id", Hex.encodeHexString(link.getTraceId().toByteArray()));
                }
                if (link.getSpanId().size() == 8 && !link.getSpanId().equals(INVALID_SPAN_ID)) {
                    linkMap.put("span_id", Hex.encodeHexString(link.getSpanId().toByteArray()));
                }
                if (StringUtils.isNotBlank(link.getTraceState())) {
                    linkMap.put("trace_state", link.getTraceState());
                }
                if (!link.getAttributesList().isEmpty()) {
                    linkMap.put("attributes", attributesToMap(link.getAttributesList()));
                }
                if (link.getDroppedAttributesCount() > 0) {
                    linkMap.put("dropped_attributes_count", link.getDroppedAttributesCount());
                }
                if (link.getFlags() > 0) {
                    linkMap.put("flags", link.getFlags());
                }
                links.add(linkMap);
            }
            asJson("otel_links", links).ifPresent(json -> fieldMap.put("otel_links", json));
        }

        return fieldMap;
    }

    private Map<String, Object> attributesToMap(List<KeyValue> attributes) {
        final Map<String, Object> result = new LinkedHashMap<>();
        for (KeyValue kv : attributes) {
            asJavaObject(kv.getValue()).ifPresent(v -> result.put(kv.getKey(), v));
        }
        return result;
    }

    private Optional<DateTime> timestamp(Span span) {
        if (span.getStartTimeUnixNano() > 0) {
            return Optional.of(dateTime(span.getStartTimeUnixNano()));
        }
        return Optional.empty();
    }

    private static DateTime dateTime(long timeUnixNano) {
        return new DateTime(timeUnixNano / 1_000_000L, DateTimeZone.UTC);
    }

    private String source(ResolvableInetSocketAddress remoteAddress) {
        if (remoteAddress.getHostName() != null) {
            return remoteAddress.getHostName();
        }
        return InetAddresses.toAddrString(remoteAddress.getAddress());
    }

    private Stream<Map.Entry<String, ?>> convertAnyValue(String key, AnyValue anyValue) {
        return switch (anyValue.getValueCase()) {
            case STRING_VALUE, BOOL_VALUE, INT_VALUE, DOUBLE_VALUE ->
                    asJavaObject(anyValue).stream().map(v -> Map.entry(key, v));
            case BYTES_VALUE -> asString(key, anyValue).stream().map(v -> Map.entry(key, v));
            case ARRAY_VALUE -> convertArray(key, anyValue.getArrayValue()).stream().map(v -> Map.entry(key, v));
            case KVLIST_VALUE -> convertKvList(key, anyValue.getKvlistValue().getValuesList());
            case VALUE_NOT_SET -> Stream.empty();
        };
    }

    private Optional<?> asJavaObject(AnyValue anyValue) {
        final var value = switch (anyValue.getValueCase()) {
            case STRING_VALUE -> anyValue.getStringValue();
            case BOOL_VALUE -> anyValue.getBoolValue();
            case INT_VALUE -> anyValue.getIntValue();
            case DOUBLE_VALUE -> anyValue.getDoubleValue();
            case BYTES_VALUE -> anyValue.getBytesValue().toByteArray();
            case ARRAY_VALUE -> asJavaList(anyValue.getArrayValue());
            case KVLIST_VALUE -> asJavaMap(anyValue.getKvlistValue());
            case VALUE_NOT_SET -> null;
        };
        return Optional.ofNullable(value);
    }

    private List<?> asJavaList(ArrayValue arrayValue) {
        return arrayValue.getValuesList().stream().flatMap(anyValue -> asJavaObject(anyValue).stream()).toList();
    }

    private Map<String, ?> asJavaMap(KeyValueList kvList) {
        return kvList.getValuesList().stream()
                .flatMap(kv -> asJavaObject(kv.getValue()).map(v -> Map.entry(kv.getKey(), v)).stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
    }

    private Stream<Map.Entry<String, ?>> convertKvList(String key, List<KeyValue> kvList) {
        return kvList.stream().flatMap(kv -> convertAnyValue(key + "_" + kv.getKey().replace('.', '_'), kv.getValue()));
    }

    private Optional<?> convertArray(String key, ArrayValue arrayValue) {
        final Set<AnyValue.ValueCase> valueCases = arrayValue.getValuesList().stream()
                .map(AnyValue::getValueCase)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(AnyValue.ValueCase.class)));
        if (valueCases.isEmpty()) {
            return Optional.empty();
        }
        // contains arrays or maps? -> serialize the whole structure as json
        if (valueCases.contains(ARRAY_VALUE) || valueCases.contains(KVLIST_VALUE)) {
            return asJson(key, asJavaList(arrayValue));
        }
        // contains just a single primitive type? -> keep it, but with individual type conversions applied
        if (valueCases.size() == 1) {
            return Optional.of(
                    arrayValue.getValuesList().stream()
                            .flatMap(anyValue -> convertAnyValue(key, anyValue))
                            .map(Map.Entry::getValue)
                            .toList());
        }
        // contains mixed primitive types? -> convert to a list of stringified elements
        return Optional.of(arrayValue.getValuesList().stream()
                .flatMap(anyValue -> asString(key, anyValue).stream())
                .toList());
    }

    private Optional<String> asString(String key, AnyValue anyValue) {
        return switch (anyValue.getValueCase()) {
            case STRING_VALUE, BOOL_VALUE, INT_VALUE, DOUBLE_VALUE -> asJavaObject(anyValue).map(String::valueOf);
            case BYTES_VALUE -> Optional.of(Base64.getEncoder().encodeToString(anyValue.getBytesValue().toByteArray()));
            case ARRAY_VALUE, KVLIST_VALUE -> asJavaObject(anyValue).flatMap(v -> asJson(key, v));
            case VALUE_NOT_SET -> Optional.empty();
        };
    }

    private Optional<String> asJson(String key, Object value) {
        try {
            return Optional.of(objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            LOG.error("Error serializing value \"{}\". Field \"{}\" will be skipped.", value, key, e);
        }
        return Optional.empty();
    }
}
