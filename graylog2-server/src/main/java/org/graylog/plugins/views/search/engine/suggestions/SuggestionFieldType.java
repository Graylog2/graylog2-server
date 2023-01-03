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
package org.graylog.plugins.views.search.engine.suggestions;

import java.util.Arrays;
import java.util.HashSet;

public enum SuggestionFieldType {
    TEXTUAL("string"),
    NUMERICAL("long", "int", "short", "byte", "double", "float"),
    OTHER("date", "binary", "geo-point", "ip");

    private final HashSet<String> fieldTypes;

    SuggestionFieldType(String... fieldTypes) {
        this.fieldTypes = new HashSet<>(Arrays.asList(fieldTypes));
    }

    public static SuggestionFieldType fromFieldType(String fieldType) {
        return Arrays.stream(values())
                .filter(it -> it.fieldTypes.contains(fieldType))
                .findFirst()
                .orElse(OTHER);
    }
}
