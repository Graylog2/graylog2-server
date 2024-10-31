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
package org.graylog.plugins.views.search.querystrings;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Date;

public record QueryString(@JsonProperty(FIELD_QUERY) @NotNull @NotEmpty String query,
                          @JsonProperty(FIELD_LAST_USED) @NotNull Date lastUsed) {
    public static final String FIELD_QUERY = "query";
    public static final String FIELD_LAST_USED = "last_used";

    public static QueryString create(@NotNull @NotEmpty String query, @NotNull Date lastUsed) {
        return new QueryString(query, lastUsed);
    }
}
