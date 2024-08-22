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
package org.graylog.plugins.views.search.views.widgets.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Filter(@JsonProperty(FIELD_FIELD) String field, @JsonProperty(FIELD_VALUE) List<String> value) {
    private static final String FIELD_FIELD = "field";
    private static final String FIELD_VALUE = "value";

    @JsonCreator
    public static Filter create(@JsonProperty(FIELD_FIELD) String field, @JsonProperty(FIELD_VALUE) List<String> value) {
        return new Filter(field, value);
    }
}
