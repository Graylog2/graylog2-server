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
package org.graylog.plugins.otel.input.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.ArrayValue;
import io.opentelemetry.proto.common.v1.InstrumentationScope;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.common.v1.KeyValueList;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.semconv.incubating.HostIncubatingAttributes;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.apache.commons.codec.binary.Hex;
import org.graylog.plugins.otel.input.Journal;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;

import java.util.Base64;
import java.util.EnumSet;
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

public class LogsCodec {
    private static final Logger LOG = getLogger(LogsCodec.class);
    private final MessageFactory messageFactory;
    private final ObjectMapper objectMapper;

    @Inject
    public LogsCodec(MessageFactory messageFactory, ObjectMapper objectMapper) {
        this.messageFactory = messageFactory;
        this.objectMapper = objectMapper;
    }

    public Optional<Message> decode(@Nonnull Journal.Log log, DateTime receiveTimestamp) {
        final var logRecord = log.getLogRecord();

        final String body = asString("body", logRecord.getBody()).orElse("");
        final Message message = messageFactory.createMessage(body, source(log), timestamp(logRecord).orElse(receiveTimestamp));

        message.addField("trace_id", Hex.encodeHexString(logRecord.getTraceId().toByteArray()));
        message.addField("span_id", Hex.encodeHexString(logRecord.getSpanId().toByteArray()));
        message.addField("trace_flags", logRecord.getFlags());
        message.addField("severity_text", logRecord.getSeverityText());
        message.addField("severity_number", logRecord.getSeverityNumberValue());

        if (logRecord.getTimeUnixNano() > 0) {
            message.addField("time_unix_nano", dateTime(logRecord.getTimeUnixNano()));
        }
        if (logRecord.getObservedTimeUnixNano() > 0) {
            message.addField("observed_time_unix_nano", dateTime(logRecord.getObservedTimeUnixNano()));
        }

        Stream.of(
                convertKvList("resource_attributes", log.getResource().getAttributesList()),
                convertKvList("attributes", logRecord.getAttributesList()),
                scope(log.getScope())
        ).flatMap(s -> s).forEach(field -> message.addField(field.getKey().replace('.', '_'), field.getValue()));

        return Optional.of(message);
    }

    private Optional<DateTime> timestamp(LogRecord logRecord) {
        if (logRecord.getTimeUnixNano() > 0) {
            return Optional.of(dateTime(logRecord.getTimeUnixNano()));
        }
        if (logRecord.getObservedTimeUnixNano() > 0) {
            return Optional.of(dateTime(logRecord.getObservedTimeUnixNano()));
        }
        return Optional.empty();
    }

    private static DateTime dateTime(long timeUnixNano) {
        return new DateTime(timeUnixNano / 1_000_000L, DateTimeZone.UTC);
    }

    private @Nullable String source(Journal.Log log) {
        return log.getResource().getAttributesList().stream()
                .filter(kv -> kv.getKey().equals(HostIncubatingAttributes.HOST_NAME.getKey()))
                .flatMap(kv -> asString(kv.getKey(), kv.getValue()).stream())
                .findFirst()
                .orElse(null);
    }

    private Stream<Map.Entry<String, ?>> scope(InstrumentationScope scope) {
        return Stream.concat(Stream.of(
                        Map.entry("scope_name", scope.getName()),
                        Map.entry("scope_version", scope.getVersion())),
                convertKvList("scope_attributes", scope.getAttributesList())
        );
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
        return kvList.stream().flatMap(kv -> convertAnyValue(key + "_" + kv.getKey(), kv.getValue()));
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
