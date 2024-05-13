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
package org.graylog2.rest.models.tools.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;

import javax.annotation.Nullable;
import java.util.List;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class PageListResponse<T> {

    public static final String ELEMENTS_FIELD_NAME = "elements";

    @Nullable
    @JsonProperty("query")
    public abstract String query();

    @JsonProperty("pagination")
    public abstract PaginatedList.PaginationInfo paginationInfo();

    @JsonProperty("total")
    public abstract long total();

    @Nullable
    @JsonProperty("sort")
    public abstract String sort();

    @Nullable
    @JsonProperty("order")
    public abstract SortOrder order();

    @JsonProperty(ELEMENTS_FIELD_NAME)
    public abstract List<T> elements();

    @JsonProperty
    public abstract List<EntityAttribute> attributes();

    @JsonProperty
    public abstract EntityDefaults defaults();

    @JsonCreator
    public static <T> PageListResponse<T> create(
            @JsonProperty("query") @Nullable String query,
            @JsonProperty("pagination") PaginatedList.PaginationInfo paginationInfo,
            @JsonProperty("total") long total,
            @JsonProperty("sort") @Nullable String sort,
            @JsonProperty("order") @Nullable SortOrder order,
            @JsonProperty(ELEMENTS_FIELD_NAME) List<T> elements,
            @JsonProperty("attributes") List<EntityAttribute> attributes,
            @JsonProperty("defaults") EntityDefaults defaults) {
        return new AutoValue_PageListResponse<>(query, paginationInfo, total, sort, order, elements, attributes, defaults);
    }

    public static <T> PageListResponse<T> create(
            @JsonProperty("query") @Nullable String query,
            @JsonProperty("pagination") PaginatedList.PaginationInfo paginationInfo,
            @JsonProperty("total") long total,
            @JsonProperty("sort") @Nullable String sort,
            @JsonProperty("order") @Nullable String order,
            @JsonProperty(ELEMENTS_FIELD_NAME) List<T> elements,
            @JsonProperty("attributes") List<EntityAttribute> attributes,
            @JsonProperty("defaults") EntityDefaults defaults) {
        return new AutoValue_PageListResponse<>(query, paginationInfo, total, sort,
                order == null ? null : SortOrder.fromString(order), elements, attributes, defaults);
    }

    public static <T> PageListResponse<T> create(
            @Nullable String query,
            final PaginatedList<T> paginatedList,
            @Nullable String sort,
            @Nullable String order,
            List<EntityAttribute> attributes,
            EntityDefaults defaults) {
        return new AutoValue_PageListResponse<>(query, paginatedList.pagination(), paginatedList.pagination().total(),
                sort, order == null ? null : SortOrder.fromString(order), paginatedList, attributes, defaults);
    }

}
