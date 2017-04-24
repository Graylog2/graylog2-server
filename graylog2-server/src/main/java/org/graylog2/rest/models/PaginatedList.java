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
package org.graylog2.rest.models;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ForwardingList;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

public class PaginatedList<E> extends ForwardingList<E> {

    private final List<E> delegate;

    private final PaginationInfo paginationInfo;

    public PaginatedList(@Nonnull List<E> delegate, int globalTotal, int page, int perPage) {
        this.delegate = delegate;
        this.paginationInfo = new PaginationInfo(globalTotal, page, perPage);
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
    public class PaginationInfo {
        @JsonProperty("total")
        private final int globalTotal;

        @JsonProperty("page")
        private final int page;

        @JsonProperty("per_page")
        private final int perPage;

        public PaginationInfo(int globalTotal, int page, int perPage) {
            this.globalTotal = globalTotal;
            this.page = page;
            this.perPage = perPage;
        }

        @JsonProperty("count")
        public int getCount() {
            return delegate().size();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PaginatedList.PaginationInfo)) return false;
            PaginationInfo that = (PaginationInfo) o;
            return globalTotal == that.globalTotal &&
                    page == that.page &&
                    perPage == that.perPage;
        }

        @Override
        public int hashCode() {
            return Objects.hash(globalTotal, page, perPage);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("globalTotal", globalTotal)
                    .add("page", page)
                    .add("perPage", perPage)
                    .toString();
        }
    }
}
