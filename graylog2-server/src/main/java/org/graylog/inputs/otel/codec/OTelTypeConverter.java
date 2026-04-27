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
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.ArrayValue;
import io.opentelemetry.proto.common.v1.KeyValueList;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Converts OpenTelemetry {@link AnyValue} instances to Java types.
 */
@Singleton
public class OTelTypeConverter {
    private static final Logger LOG = getLogger(OTelTypeConverter.class);

    private final ObjectMapper objectMapper;

    @Inject
    public OTelTypeConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Converts an {@link AnyValue} to its string representation.
     * Primitives use {@link String#valueOf}, bytes are Base64-encoded,
     * arrays and key-value lists are serialized as JSON.
     *
     * @param anyValue the value to convert
     * @param context  label used in error log messages (e.g. field name or "body")
     * @return the string representation, or empty if the value case is not set
     */
    public Optional<String> toString(AnyValue anyValue, String context) {
        return switch (anyValue.getValueCase()) {
            case STRING_VALUE, BOOL_VALUE, INT_VALUE, DOUBLE_VALUE -> toJavaObject(anyValue).map(String::valueOf);
            case BYTES_VALUE -> Optional.of(Base64.getEncoder().encodeToString(anyValue.getBytesValue().toByteArray()));
            case ARRAY_VALUE, KVLIST_VALUE -> toJavaObject(anyValue).flatMap(v -> toJson(v, context));
            case VALUE_NOT_SET -> Optional.empty();
        };
    }

    /**
     * Converts an {@link AnyValue} to its natural Java type.
     * <ul>
     *     <li>STRING_VALUE -> String</li>
     *     <li>BOOL_VALUE -> Boolean</li>
     *     <li>INT_VALUE -> Long</li>
     *     <li>DOUBLE_VALUE -> Double</li>
     *     <li>BYTES_VALUE -> byte[]</li>
     *     <li>ARRAY_VALUE -> List</li>
     *     <li>KVLIST_VALUE -> Map</li>
     * </ul>
     */
    public Optional<?> toJavaObject(AnyValue anyValue) {
        final var value = switch (anyValue.getValueCase()) {
            case STRING_VALUE -> anyValue.getStringValue();
            case BOOL_VALUE -> anyValue.getBoolValue();
            case INT_VALUE -> anyValue.getIntValue();
            case DOUBLE_VALUE -> anyValue.getDoubleValue();
            case BYTES_VALUE -> anyValue.getBytesValue().toByteArray();
            case ARRAY_VALUE -> toJavaList(anyValue.getArrayValue());
            case KVLIST_VALUE -> toJavaMap(anyValue.getKvlistValue());
            case VALUE_NOT_SET -> null;
        };
        return Optional.ofNullable(value);
    }

    /**
     * Converts an OTel {@link ArrayValue} to a Java list.
     */
    public List<?> toJavaList(ArrayValue arrayValue) {
        return arrayValue.getValuesList().stream().flatMap(v -> toJavaObject(v).stream()).toList();
    }

    /**
     * Converts an OTel {@link KeyValueList} to a Java map.
     */
    public Map<String, ?> toJavaMap(KeyValueList kvList) {
        return kvList.getValuesList().stream()
                .flatMap(kv -> toJavaObject(kv.getValue()).map(v -> Map.entry(kv.getKey(), v)).stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
    }

    /**
     * Serializes a Java object to JSON string.
     *
     * @param value   the object to serialize
     * @param context label used in error log messages
     * @return the JSON string, or empty on serialization failure
     */
    public Optional<String> toJson(Object value, String context) {
        try {
            return Optional.of(objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            LOG.error("Error serializing value \"{}\". Field \"{}\" will be skipped.", value, context, e);
        }
        return Optional.empty();
    }
}
