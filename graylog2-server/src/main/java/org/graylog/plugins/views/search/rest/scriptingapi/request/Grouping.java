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
package org.graylog.plugins.views.search.rest.scriptingapi.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

//no column/row choice, assuming API does not care about visualization, and we can ignore it
public record Grouping(@JsonProperty("field") @Valid @NotBlank String fieldName,
                       @JsonProperty("limit") int limit) {

    public Grouping(String fieldName) {
        this(fieldName, Values.DEFAULT_LIMIT);
    }

    public Grouping(@JsonProperty("field") @Valid @NotBlank String fieldName,
                    @JsonProperty("limit") int limit) {
        this.fieldName = fieldName;
        if (limit <= 0) {
            this.limit = Values.DEFAULT_LIMIT;
        } else {
            this.limit = limit;
        }
    }

    @Deprecated
    @Override
    public String fieldName() {
        return fieldName;
    }

    public RequestedField requestedField() {
        return RequestedField.parse(fieldName);
    }

}
