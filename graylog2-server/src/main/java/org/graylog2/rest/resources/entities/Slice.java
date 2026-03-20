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

public record Slice(@JsonProperty(FIELD_ID) String value, @JsonProperty(FIELD_TITLE) String title, @JsonProperty(FIELD_TYPE) String type, @JsonProperty(FIELD_COUNT) Integer count) {
    private static final String FIELD_ID = "value";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_COUNT = "count";

    public static Slice create(final String value, final String type, final Integer count) {
        return new Slice(value, null, type, count);
    }

    public static Slice create(final String value, final Integer count) {
        return new Slice(value, null, null, count);
    }
}
