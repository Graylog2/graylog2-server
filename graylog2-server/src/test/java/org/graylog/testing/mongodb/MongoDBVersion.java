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
package org.graylog.testing.mongodb;

import com.google.auto.value.AutoValue;

import java.util.regex.Pattern;

import static org.graylog2.shared.utilities.StringUtils.requireNonBlank;

@AutoValue
public abstract class MongoDBVersion {
    public static final MongoDBVersion DEFAULT = of("7.0");
    private static final Pattern PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)$");

    public abstract String version();

    public static MongoDBVersion of(String version) {
        final var value = requireNonBlank(version, "version can't be blank");

        if (version.matches("^(\\d+)\\.(\\d+)$")) {
            return new AutoValue_MongoDBVersion(value);
        }

        throw new IllegalArgumentException("MongoDB version must be in the format 'X.Y' where X and Y are integers (e.g., '7.0')");
    }
}
