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
package org.graylog.plugins.sidecar.rest.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.sidecar.rest.models.SidecarSummary;
import org.graylog2.database.PaginatedList;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

@AutoValue
public abstract class SidecarListResponse {
    @Nullable
    @JsonProperty
    public abstract String query();

    @JsonProperty("pagination")
    public abstract PaginatedList.PaginationInfo paginationInfo();

    @JsonProperty
    public abstract long total();

    @JsonProperty
    public abstract Boolean onlyActive();

    @Nullable
    @JsonProperty
    public abstract String sort();

    @Nullable
    @JsonProperty
    public abstract String order();

    @JsonProperty
    public abstract Collection<SidecarSummary> sidecars();

    @Nullable
    @JsonProperty
    public abstract Map<String, String> filters();

    @JsonCreator
    public static SidecarListResponse create(@JsonProperty("query") @Nullable String query,
                                             @JsonProperty("pagination") PaginatedList.PaginationInfo paginationInfo,
                                             @JsonProperty("total") long total,
                                             @JsonProperty("only_active") Boolean onlyActive,
                                             @JsonProperty("sort") @Nullable String sort,
                                             @JsonProperty("order") @Nullable String order,
                                             @JsonProperty("sidecars") Collection<SidecarSummary> sidecars,
                                             @JsonProperty("filters") @Nullable Map<String, String> filters) {
        return new AutoValue_SidecarListResponse(query, paginationInfo, total, onlyActive, sort, order, sidecars, filters);
    }

    public static SidecarListResponse create(@JsonProperty("query") @Nullable String query,
                                             @JsonProperty("pagination") PaginatedList.PaginationInfo paginationInfo,
                                             @JsonProperty("total") long total,
                                             @JsonProperty("only_active") Boolean onlyActive,
                                             @JsonProperty("sort") @Nullable String sort,
                                             @JsonProperty("order") @Nullable String order,
                                             @JsonProperty("sidecars") Collection<SidecarSummary> sidecars) {
        return create(query, paginationInfo, total, onlyActive, sort, order, sidecars, null);
    }
}
