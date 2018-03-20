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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import org.graylog2.database.PaginatedList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Map;

@JsonAutoDetect
public class PaginatedResponse<T> {
    private final String listKey;
    private final PaginatedList<T> paginatedList;
    private final String query;

    private PaginatedResponse(String listKey, PaginatedList<T> paginatedList, @Nullable String query) {
        this.listKey = listKey;
        this.paginatedList = paginatedList;
        this.query = query;
    }

    @JsonValue
    public Map<String, Object> jsonValue() {
        final ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder()
                .putAll(paginatedList.pagination().asMap())
                .put(listKey, new ArrayList<>(paginatedList));

        if (query != null) {
            builder.put("query", query);
        }

        return builder.build();
    }

    public static <T> PaginatedResponse<T> create(String listKey, PaginatedList<T> paginatedList) {
        return new PaginatedResponse<>(listKey, paginatedList, null);
    }

    public static <T> PaginatedResponse<T> create(String listKey, PaginatedList<T> paginatedList, String query) {
        return new PaginatedResponse<>(listKey, paginatedList, query);
    }
}
