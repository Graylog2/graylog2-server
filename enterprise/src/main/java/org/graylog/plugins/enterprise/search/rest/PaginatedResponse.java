package org.graylog.plugins.enterprise.search.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.google.common.collect.ImmutableMap;
import org.graylog.plugins.enterprise.database.PaginatedList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Map;

@JsonAutoDetect
public class PaginatedResponse<T> {
    @SuppressWarnings("unused")
    @JsonUnwrapped
    private final Map<String, Object> data;

    private PaginatedResponse(String listKey, PaginatedList<T> paginatedList, @Nullable String query) {
        final ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder()
                .putAll(paginatedList.pagination().asMap())
                .put(listKey, new ArrayList<>(paginatedList));

        if (query != null) {
            builder.put("query", query);
        }

        this.data = builder.build();
    }

    public static <T> PaginatedResponse<T> create(String listKey, PaginatedList<T> paginatedList) {
        return new PaginatedResponse<>(listKey, paginatedList, null);
    }

    public static <T> PaginatedResponse<T> create(String listKey, PaginatedList<T> paginatedList, String query) {
        return new PaginatedResponse<>(listKey, paginatedList, query);
    }
}
