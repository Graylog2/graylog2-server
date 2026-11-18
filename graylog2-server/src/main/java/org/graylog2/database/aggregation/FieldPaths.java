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
package org.graylog2.database.aggregation;

import jakarta.annotation.Nullable;

import java.util.regex.Pattern;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Validates the names that end up inside aggregation stages. Field paths become {@code "$path"} expressions, so a
 * leading {@code $} or an operator in a name would change what the pipeline does.
 */
final class FieldPaths {
    private static final Pattern FIELD_PATH = Pattern.compile("[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*");
    private static final Pattern COLLECTION_NAME = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9_.-]*");

    private FieldPaths() {}

    static String requireFieldPath(@Nullable final String path) {
        if (path == null || !FIELD_PATH.matcher(path).matches()) {
            throw new IllegalArgumentException(f("Invalid field path <%s>, it must match %s", path, FIELD_PATH));
        }
        return path;
    }

    static String requireCollectionName(@Nullable final String name) {
        if (name == null || !COLLECTION_NAME.matcher(name).matches() || name.startsWith("system.")) {
            throw new IllegalArgumentException(f("Invalid collection name <%s>", name));
        }
        return name;
    }
}
