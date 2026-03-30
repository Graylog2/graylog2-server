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
package org.graylog.collectors.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.time.Duration;

/**
 * Serializes {@link Duration} to a string compatible with Go's {@code time.ParseDuration}.
 * <p>
 * Decomposes the duration into integer components from largest to smallest unit
 * ({@code h}, {@code m}, {@code s}, {@code ms}, {@code us}, {@code ns}), omitting zero components.
 * This preserves nanosecond precision and produces human-readable output.
 * <p>
 * Examples: {@code "24h"}, {@code "1h30m"}, {@code "30s"}, {@code "500ms"}, {@code "1s500ms"}.
 *
 * @see <a href="https://pkg.go.dev/time#ParseDuration">Go time.ParseDuration</a>
 */
public class GoDurationSerializer extends JsonSerializer<Duration> {

    @Override
    public void serialize(Duration value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(toGoString(value));
    }

    @VisibleForTesting
    static String toGoString(Duration duration) {
        if (duration.isZero()) {
            return "0s";
        }

        final var sb = new StringBuilder();
        if (duration.isNegative()) {
            sb.append('-');
            duration = duration.negated();
        }

        final long hours = duration.toHours();
        final int minutes = duration.toMinutesPart();
        final int seconds = duration.toSecondsPart();
        final int nanos = duration.toNanosPart();

        if (hours > 0) {
            sb.append(hours).append('h');
        }
        if (minutes > 0) {
            sb.append(minutes).append('m');
        }
        if (seconds > 0) {
            sb.append(seconds).append('s');
        }

        final int millis = nanos / 1_000_000;
        final int micros = (nanos % 1_000_000) / 1_000;
        final int remainingNanos = nanos % 1_000;

        if (millis > 0) {
            sb.append(millis).append("ms");
        }
        if (micros > 0) {
            sb.append(micros).append("us");
        }
        if (remainingNanos > 0) {
            sb.append(remainingNanos).append("ns");
        }

        return sb.toString();
    }
}
