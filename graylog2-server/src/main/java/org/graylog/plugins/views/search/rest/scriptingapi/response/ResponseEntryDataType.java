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
package org.graylog.plugins.views.search.rest.scriptingapi.response;

import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

public enum ResponseEntryDataType {
    STRING(Set.of("keyword", "text")),
    NUMERIC(Set.of("long", "integer", "short", "byte", "double", "float", "half_float", "scaled_float")),
    DATE(Set.of("date")),
    BOOLEAN(Set.of("boolean")),
    BINARY(Set.of("binary")),
    IP(Set.of("ip")),
    GEO(Set.of("geo_point", "geo_shape")),
    UNKNOWN(Set.of());

    private final Set<String> correspondingSearchEngineTypes;

    ResponseEntryDataType(Set<String> correspondingSearchEngineTypes) {
        this.correspondingSearchEngineTypes = correspondingSearchEngineTypes;
    }

    public static ResponseEntryDataType fromSearchEngineType(final String type) {
        if (StringUtils.isBlank(type)) {
            return UNKNOWN;
        }
        return Arrays.stream(ResponseEntryDataType.values())
                .filter(dataType -> dataType.correspondingSearchEngineTypes.contains(type))
                .findFirst()
                .orElse(UNKNOWN);
    }

    @JsonValue
    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
