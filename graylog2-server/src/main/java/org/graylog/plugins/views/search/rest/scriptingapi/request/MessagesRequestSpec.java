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
package org.graylog.plugins.views.search.rest.scriptingapi.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseEntryDataType;
import org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseSchemaEntry;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record MessagesRequestSpec(@JsonProperty("query") String queryString,
                                  @JsonProperty("streams") Set<String> streams,
                                  @JsonProperty("timerange") TimeRange timerange,
                                  @JsonProperty("from") int from,
                                  @JsonProperty("size") int size,
                                  @JsonProperty("fields") List<String> fields) implements SearchRequestSpec {


    public static final List<String> DEFAULT_FIELDS = List.of("source", "timestamp");
    public static final int DEFAULT_SIZE = 10;
    public static final int DEFAULT_FROM = 0;

    public MessagesRequestSpec {
        if (Strings.isNullOrEmpty(queryString)) {
            queryString = DEFAULT_QUERY_STRING;
        }
        if (timerange == null) {
            timerange = DEFAULT_TIMERANGE;
        }
        if (streams == null) {
            streams = Set.of();
        }
        if (from < DEFAULT_FROM) {
            from = DEFAULT_FROM;
        }
        if (size <= DEFAULT_FROM) {
            size = DEFAULT_SIZE;
        }
        if (fields == null || fields.isEmpty()) {
            fields = DEFAULT_FIELDS;
        }

    }

    @JsonIgnore
    public List<ResponseSchemaEntry> getSchema(final Map<String, String> fieldTypes) {
        return fields()
                .stream()
                .map(field -> {
                    final String type = fieldTypes.getOrDefault(field, null);
                    return ResponseSchemaEntry.field(field, ResponseEntryDataType.fromSearchEngineType(type));
                })
                .collect(Collectors.toList());
    }
}
