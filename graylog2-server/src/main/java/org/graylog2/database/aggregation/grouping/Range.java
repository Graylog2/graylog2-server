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
package org.graylog2.database.aggregation.grouping;

import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.Objects;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * A range of numbers or instants, including {@code from} and excluding {@code to}. Either end may be open.
 */
public record Range(String key, @Nullable Object from, @Nullable Object to) {
    public Range {
        Objects.requireNonNull(key, "key");
        if (from == null && to == null) {
            throw new IllegalArgumentException(f("Range <%s> needs at least one bound", key));
        }
        requireBound(key, from);
        requireBound(key, to);
        if (from != null && to != null) {
            if (from instanceof Instant != to instanceof Instant) {
                throw new IllegalArgumentException(f("Range <%s> must not mix numbers and instants", key));
            }
            if (compare(from, to) >= 0) {
                throw new IllegalArgumentException(f("Range <%s> must start before it ends", key));
            }
        }
    }

    public static Range of(final String key, @Nullable final Number from, @Nullable final Number to) {
        return new Range(key, from, to);
    }

    public static Range of(final String key, @Nullable final Instant from, @Nullable final Instant to) {
        return new Range(key, from, to);
    }

    public boolean isTemporal() {
        return (from != null ? from : to) instanceof Instant;
    }

    private static void requireBound(final String key, @Nullable final Object bound) {
        if (bound != null && !(bound instanceof Number) && !(bound instanceof Instant)) {
            throw new IllegalArgumentException(f("Range <%s> bounds must be numbers or instants", key));
        }
    }

    private static int compare(final Object from, final Object to) {
        if (from instanceof Instant fromInstant) {
            return fromInstant.compareTo((Instant) to);
        }
        return Double.compare(((Number) from).doubleValue(), ((Number) to).doubleValue());
    }
}
