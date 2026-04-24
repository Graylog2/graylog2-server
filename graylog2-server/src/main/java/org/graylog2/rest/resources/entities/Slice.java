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
package org.graylog2.rest.resources.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record Slice(@JsonProperty(FIELD_ID) String value,
                    @JsonProperty(FIELD_TITLE) String title,
                    @JsonProperty(FIELD_COUNT) Integer count,
                    @JsonProperty(FIELD_META) Map<String, Object> meta) {
    private static final String FIELD_ID = "value";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_COUNT = "count";
    private static final String FIELD_META = "meta";

    public Slice(String value, String title, Integer count) {
        this(value, title, count, Map.of());
    }
    public Slice(String value, Integer count, Map<String, Object> meta) {
        this(value, null, count, meta);
    }

    // used to make sure, that we have a minimal count of 1 to prevent hiding with current FE logic. Will change in the future
    public static Slice minimalCount1(Slice slice) {
        return new Slice(slice.value(), slice.title(), slice.count() > 0 ? slice.count() : 1, slice.meta());
    }
}
