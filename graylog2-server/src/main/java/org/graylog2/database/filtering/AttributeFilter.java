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
package org.graylog2.database.filtering;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static com.cronutils.utils.Preconditions.checkArgument;

public record AttributeFilter(@JsonProperty(FIELD_FIELD) String field,
                              @JsonProperty(FIELD_VALUE) List<String> value) {
    private static final String FIELD_FIELD = "field";
    private static final String FIELD_VALUE = "value";

    @JsonCreator
    public static AttributeFilter create(@JsonProperty(FIELD_FIELD) String field,
                                         @JsonProperty(FIELD_VALUE) @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY) List<String> value) {
        checkArgument(!value.isEmpty(), "Value should not be empty.");
        return new AttributeFilter(field, value);
    }

    private String quoted(String value) {
        if (value.contains(" ")) {
            return "\"" + value + "\"";
        } else {
            return value;
        }
    }

    @JsonIgnore
    public List<String> toQueryStrings() {
        return value.stream()
                .map(v -> field + ":" + quoted(v))
                .toList();
    }
}
