/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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

public class PaginatedList<E> extends ForwardingList<E> {

    private final List<E> delegate;

    private final PaginationInfo paginationInfo;

    public PaginatedList(@Nonnull List<E> delegate, int total, int page, int perPage) {
        this.delegate = delegate;
        this.paginationInfo = PaginationInfo.create(total, delegate.size(), page, perPage);
    }

    @Override
    public List<E> delegate() {
        return delegate;
    }

    public PaginationInfo pagination() {
        return paginationInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaginatedList)) return false;
        PaginatedList<?> that = (PaginatedList<?>) o;
        return Objects.equals(pagination(), that.pagination()) &&
                Objects.equals(delegate(), that.delegate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate(), pagination());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("content", delegate)
                .add("pagination_info", pagination())
                .toString();
    }

    public static <T> PaginatedList<T> emptyList(int page, int perPage) {
        return new PaginatedList<>(Collections.emptyList(), 0, page, perPage);
    }

    public static <T> PaginatedList<T> singleton(T entry, int page, int perPage) {
        return new PaginatedList<T>(Collections.singletonList(entry), 1, page, perPage);
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
