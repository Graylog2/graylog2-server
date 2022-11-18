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
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

//no column/row choice, assuming API does not care about visualization, and we can ignore it
public record Grouping(@JsonProperty("field_name") @Valid @NotBlank String fieldName,
                       @JsonProperty("limit") int limit,
                       @JsonProperty("sort") SortSpec.Direction sort) implements Sortable {


    public Grouping(@JsonProperty("field_name") @Valid @NotBlank String fieldName,
                    @JsonProperty("limit") int limit,
                    @JsonProperty("sort") SortSpec.Direction sort) {
        this.fieldName = fieldName;
        this.sort = sort;
        if (limit <= 0) {
            this.limit = Values.DEFAULT_LIMIT;
        } else {
            this.limit = limit;
        }
    }

    @Override
    public String sortColumnName() {
        return fieldName();
    }
}
