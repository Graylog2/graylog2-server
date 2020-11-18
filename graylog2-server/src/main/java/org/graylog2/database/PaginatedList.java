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
package org.graylog2.database;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ForwardingList;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PaginatedList<E> extends ForwardingList<E> {

    private final List<E> delegate;

    private final PaginationInfo paginationInfo;

    private final Long grandTotal;

    /**
     * Creates a PaginatedList
     * @param delegate the actual entries
     * @param total the count of all entries (ignoring pagination)
     * @param page  the page this PaginatedList represents
     * @param perPage the size limit for each page
     */
    public PaginatedList(@Nonnull List<E> delegate, int total, int page, int perPage) {
        this(delegate, total, page, perPage, null);
    }

    /**
     * Creates a PaginatedList
     * @param delegate the actual entries
     * @param total the count of all entries (ignoring pagination)
     * @param page  the page this PaginatedList represents
     * @param perPage the size limit for each page
     * @param grandTotal the count of all entries (ignoring query filters and pagination)
     */
    public PaginatedList(@Nonnull List<E> delegate, int total, int page, int perPage, Long grandTotal) {
        this.delegate = delegate;
        this.paginationInfo = PaginationInfo.create(total, delegate.size(), page, perPage);
        this.grandTotal = grandTotal;
    }

    @Override
    public List<E> delegate() {
        return delegate;
    }

    public PaginationInfo pagination() {
        return paginationInfo;
    }

    public Optional<Long> grandTotal() {
        return Optional.ofNullable(grandTotal);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaginatedList)) return false;
        PaginatedList<?> that = (PaginatedList<?>) o;
        return Objects.equals(pagination(), that.pagination()) &&
                Objects.equals(delegate(), that.delegate()) &&
                Objects.equals(grandTotal(), that.grandTotal());
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate(), pagination(), grandTotal());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("content", delegate)
                .add("pagination_info", pagination())
                .add("grand_total", grandTotal())
                .toString();
    }

    public static <T> PaginatedList<T> emptyList(int page, int perPage) {
        return new PaginatedList<>(Collections.emptyList(), 0, page, perPage, 0L);
    }

    public static <T> PaginatedList<T> singleton(T entry, int page, int perPage) {
        return new PaginatedList<>(Collections.singletonList(entry), 1, page, perPage, 1L);
    }

    @JsonAutoDetect
    @AutoValue
    public static abstract class PaginationInfo {
        @JsonProperty("total")
        public abstract int total();

        @JsonProperty("count")
        public abstract int count();

        @JsonProperty("page")
        public abstract int page();

        @JsonProperty("per_page")
        public abstract int perPage();

        public static PaginationInfo create(int total, int count, int page, int perPage) {
            return new AutoValue_PaginatedList_PaginationInfo(total, count, page, perPage);
        }

        public ImmutableMap<String, Object> asMap() {
            return ImmutableMap.of(
                    "total", total(),
                    "page", page(),
                    "per_page", perPage(),
                    "count", count()
            );
        }
    }
}
