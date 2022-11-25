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
package org.graylog2.rest.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import org.graylog2.database.PaginatedList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@JsonAutoDetect
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@AutoValue
public abstract class PaginatedResponse<T> {
    @JsonProperty("entities")
    @JsonInclude()
    public List<T> entities() {
        return new ArrayList<>(paginatedList());
    }

    @JsonIgnore
    public abstract PaginatedList<T> paginatedList();

    @JsonProperty("query")
    @Nullable
    @JsonInclude()
    public abstract String query();

    @JsonProperty("context")
    @Nullable
    public abstract Map<String, Object> context();

    @JsonProperty("grand_total")
    public Optional<Long> grandTotal() {
        return paginatedList().grandTotal();
    }

    @JsonProperty("total")
    public int total() {
        return paginatedList().pagination().total();
    }

    @JsonProperty("count")
    public int count() {
        return paginatedList().pagination().count();
    }

    @JsonProperty("page")
    public int page() {
        return paginatedList().pagination().page();
    }

    @JsonProperty("per_page")
    public int perPage() {
        return paginatedList().pagination().perPage();
    }


    public static <T> PaginatedResponse<T> create(PaginatedList<T> paginatedList) {
        return create(paginatedList, null, null);
    }

    public static <T> PaginatedResponse<T> create(PaginatedList<T> paginatedList, Map<String, Object> context) {
        return create(paginatedList, null, context);
    }

    public static <T> PaginatedResponse<T> create(PaginatedList<T> paginatedList, String query) {
        return create(paginatedList, query, null);
    }

    public static <T> PaginatedResponse<T> create(PaginatedList<T> paginatedList, String query, Map<String, Object> context) {
        return new AutoValue_PaginatedResponse<>(paginatedList, query, context);
    }
}
