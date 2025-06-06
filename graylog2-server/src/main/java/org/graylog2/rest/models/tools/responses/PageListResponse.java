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
    public static final String TOTAL_FIELD_NAME = "total";
    public static final String QUERY_FIELD_NAME = "query";
    public static final String PAGINATION_FIELD_NAME = "pagination";
    public static final String SORT_FIELD_NAME = "sort";
    public static final String ORDER_FIELD_NAME = "order";
    public static final String ATTRIBUTES_FIELD_NAME = "attributes";
    public static final String DEFAULTS_FIELD_NAME = "defaults";

    @Nullable
    @JsonProperty(QUERY_FIELD_NAME)
    public abstract String query();

    @JsonProperty(PAGINATION_FIELD_NAME)
    public abstract PaginatedList.PaginationInfo paginationInfo();

    @JsonProperty(TOTAL_FIELD_NAME)
    public abstract long total();

    @Nullable
    @JsonProperty(SORT_FIELD_NAME)
    public abstract String sort();

    @Nullable
    @JsonProperty(ORDER_FIELD_NAME)
    public abstract SortOrder order();

    @JsonProperty(ELEMENTS_FIELD_NAME)
    public abstract List<T> elements();

    @JsonProperty(ATTRIBUTES_FIELD_NAME)
    public abstract List<EntityAttribute> attributes();

    @JsonProperty(DEFAULTS_FIELD_NAME)
    public abstract EntityDefaults defaults();

    @JsonCreator
    public static <T> PageListResponse<T> create(
            @JsonProperty(QUERY_FIELD_NAME) @Nullable String query,
            @JsonProperty(PAGINATION_FIELD_NAME) PaginatedList.PaginationInfo paginationInfo,
            @JsonProperty(TOTAL_FIELD_NAME) long total,
            @JsonProperty(SORT_FIELD_NAME) @Nullable String sort,
            @JsonProperty(ORDER_FIELD_NAME) @Nullable SortOrder order,
            @JsonProperty(ELEMENTS_FIELD_NAME) List<T> elements,
            @JsonProperty(ATTRIBUTES_FIELD_NAME) List<EntityAttribute> attributes,
            @JsonProperty(DEFAULTS_FIELD_NAME) EntityDefaults defaults) {
        return new AutoValue_PageListResponse<>(query, paginationInfo, total, sort, order, elements, attributes, defaults);
    }

    public static <T> PageListResponse<T> create(
            @JsonProperty(QUERY_FIELD_NAME) @Nullable String query,
            @JsonProperty(PAGINATION_FIELD_NAME) PaginatedList.PaginationInfo paginationInfo,
            @JsonProperty(TOTAL_FIELD_NAME) long total,
            @JsonProperty(SORT_FIELD_NAME) @Nullable String sort,
            @JsonProperty(ORDER_FIELD_NAME) @Nullable String order,
            @JsonProperty(ELEMENTS_FIELD_NAME) List<T> elements,
            @JsonProperty(ATTRIBUTES_FIELD_NAME) List<EntityAttribute> attributes,
            @JsonProperty(DEFAULTS_FIELD_NAME) EntityDefaults defaults) {
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
