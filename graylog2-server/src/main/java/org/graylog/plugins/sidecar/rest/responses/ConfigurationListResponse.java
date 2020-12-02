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
import org.graylog.plugins.sidecar.rest.models.ConfigurationSummary;
import org.graylog2.database.PaginatedList;

import javax.annotation.Nullable;
import java.util.Collection;

@AutoValue
public abstract class ConfigurationListResponse {
    @Nullable
    @JsonProperty
    public abstract String query();

    @JsonProperty("pagination")
    public abstract PaginatedList.PaginationInfo paginationInfo();

    @JsonProperty
    public abstract long total();

    @Nullable
    @JsonProperty
    public abstract String sort();

    @Nullable
    @JsonProperty
    public abstract String order();

    @JsonProperty
    public abstract Collection<ConfigurationSummary> configurations();

    @JsonCreator
    public static ConfigurationListResponse create(@JsonProperty("query") String query,
                                                   @JsonProperty("pagination") PaginatedList.PaginationInfo paginationInfo,
                                                   @JsonProperty("total") long total,
                                                   @JsonProperty("sort") String sort,
                                                   @JsonProperty("order") String order,
                                                   @JsonProperty("configurations") Collection<ConfigurationSummary> configurations) {
        return new AutoValue_ConfigurationListResponse(query, paginationInfo, total, sort, order, configurations);
    }
}
