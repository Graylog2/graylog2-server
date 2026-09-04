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
package org.graylog2.rest.resources.entities.preferences.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
public record SlicingPreferences(@JsonProperty("slice_column") String sliceColumn,
                                 @JsonProperty("sort_by") String sortBy,
                                 @JsonProperty("order") SortOrder sortOrder,
                                 @JsonProperty("read_only") boolean readOnly) {

    public SlicingPreferences(final String sliceColumn, final String sortBy, final SortOrder sortOrder) {
        this(sliceColumn, sortBy, sortOrder, false);
    }
}
