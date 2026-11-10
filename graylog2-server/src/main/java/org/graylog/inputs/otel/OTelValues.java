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

import com.google.common.base.Strings;
import io.opentelemetry.proto.common.v1.AnyValue;

/**
 * Decodes numeric {@link AnyValue}s that follow OpenTelemetry's
 * <a href="https://opentelemetry.io/docs/specs/otel/common/attribute-type-mapping/">Mapping Arbitrary
 * Data to OTLP AnyValue</a>.
 * <p>
 * {@code AnyValue} carries only a <em>signed</em> 64-bit integer, so that mapping tells producers to
 * send an integer while the source value fits {@code [-2^63..2^63-1]} and a decimal string when it
 * does not. A field whose source type is unsigned 64-bit therefore arrives as either OTLP type
 * depending on its magnitude, which is a property of the protocol rather than of any one sender.
 */
public final class OTelValues {

    private OTelValues() {
    }

    /**
     * Renders an integer whose source type is unsigned 64-bit as a decimal string, whichever way the
     * sender encoded it. Strings cover the whole {@code uint64} range without loss and keep the
     * field's type stable across records — a field alternating between long and string would cause a
     * mapping conflict in the index.
     *
     * @return the decimal representation, or {@code null} if the value is absent or not numeric
     */
    public static String unsignedAsString(AnyValue value) {
        return switch (value.getValueCase()) {
            case INT_VALUE -> Long.toUnsignedString(value.getIntValue());
            case STRING_VALUE -> Strings.emptyToNull(value.getStringValue());
            default -> null;
        };
    }

    /**
     * Reads a signed integer, accepting both the integer and the decimal-string encoding.
     * <p>
     * Values outside the signed 64-bit range are rejected rather than wrapped: use
     * {@link #unsignedAsString(AnyValue)} for fields whose source type is unsigned.
     *
     * @return the value, or {@code null} if it is absent, not numeric, or does not fit a {@code long}
     */
    public static Long asLong(AnyValue value) {
        if (value.getValueCase() == AnyValue.ValueCase.INT_VALUE) {
            return value.getIntValue();
        }

        if (value.getValueCase() != AnyValue.ValueCase.STRING_VALUE) {
            return null;
        }

        final var stringValue = value.getStringValue();
        if (stringValue.isEmpty()) {
            return null;
        }

        try {
            return Long.parseLong(stringValue);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
