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
package org.graylog2.database.slicing;

import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Asks for the slices of one column, for the entities the list currently shows.
 *
 * @param sliceColumn the id of the column to slice by
 * @param filters     the list's filters, as {@code <column>:<value>} expressions
 * @param query       the list's search query
 */
public record SliceRequest(String sliceColumn, List<String> filters, String query) {
    public SliceRequest {
        Objects.requireNonNull(sliceColumn, "sliceColumn");
        filters = List.copyOf(filters);
        Objects.requireNonNull(query, "query");
    }

    /**
     * Creates a request from REST parameters, which may be absent. A missing column is rejected when slicing.
     */
    public static SliceRequest of(@Nullable final String sliceColumn,
                                  @Nullable final List<String> filters,
                                  @Nullable final String query) {
        return new SliceRequest(sliceColumn == null ? "" : sliceColumn,
                filters == null ? List.of() : filters,
                query == null ? "" : query);
    }
}
