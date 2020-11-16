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
package org.graylog2.indexer.indices;

import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;

public enum HealthStatus {
    Red,
    Yellow,
    Green;

    public static HealthStatus fromString(String value) {
        checkNotNull(value);
        final String normalizedValue = value.toUpperCase(Locale.ENGLISH);
        switch (normalizedValue) {
            case "RED": return Red;
            case "YELLOW": return Yellow;
            case "GREEN": return Green;

            default: throw new IllegalArgumentException("Unable to parse health status from string (known: GREEN/YELLOW/RED): " + normalizedValue);
        }
    }
}
