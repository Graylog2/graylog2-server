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

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class PageListResponse<T> {

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
    public abstract String order();

    @JsonProperty("elements")
    public abstract Collection<T> elements();

    @JsonCreator
    public static <T> PageListResponse<T> create(
            @JsonProperty("query") @Nullable String query,
            @JsonProperty("pagination") PaginatedList.PaginationInfo paginationInfo,
            @JsonProperty("total") long total,
            @JsonProperty("sort") @Nullable String sort,
            @JsonProperty("order") @Nullable String order,
            @JsonProperty("elements") Collection<T> elements) {
        return new AutoValue_PageListResponse<>(query, paginationInfo, total, sort, order, elements);
    }

    public static <T> PageListResponse<T> create(
            @Nullable String query,
            final PaginatedList<T> paginatedList,
            @Nullable String sort,
            @Nullable String order) {
        return new AutoValue_PageListResponse<>(query, paginatedList.pagination(), paginatedList.pagination().total(), sort, order, paginatedList);
    }

    public PageListResponse<T> withHiddenConfidentialInfo(final Predicate<T> hasConfidentialInfoChecker, final UnaryOperator<T> confidentialInfoHider) {
        if (elements().isEmpty()) {
            return this;
        }
        return PageListResponse.create(
                query(),
                paginationInfo(),
                total(),
                sort(),
                order(),
                elements()
                        .stream()
                        .map(element -> {
                            if (hasConfidentialInfoChecker.test(element)) {
                                return confidentialInfoHider.apply(element);
                            } else {
                                return element;
                            }
                        })
                        .collect(Collectors.toList()));
    }
}
