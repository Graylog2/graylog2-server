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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record MessagesRequestSpec(@JsonProperty("query") String queryString,
                                  @JsonProperty("streams") Set<String> streams,
                                  @JsonProperty("stream_categories") Set<String> streamCategories,
                                  @JsonProperty("timerange") TimeRange timerange,
                                  @JsonProperty("sort") String sort,
                                  @JsonProperty("sort_order") SortSpec.Direction sortOrder,
                                  @JsonProperty("from") int from,
                                  @JsonProperty("size") int size,
                                  @JsonProperty("fields") List<String> fields) implements SearchRequestSpec {


    public static final List<String> DEFAULT_FIELDS = List.of("source", "timestamp");
    public static final String DEFAULT_SORT = Message.FIELD_TIMESTAMP;
    public static final SortSpec.Direction DEFAULT_SORT_ORDER = SortSpec.Direction.Descending;
    public static final int DEFAULT_SIZE = 10;
    public static final int DEFAULT_FROM = 0;

    public MessagesRequestSpec {
        if (Strings.isNullOrEmpty(queryString)) {
            queryString = DEFAULT_QUERY_STRING;
        }
        if (Strings.isNullOrEmpty(sort)) {
            sort = DEFAULT_SORT;
        }
        if (sortOrder == null) {
            sortOrder = DEFAULT_SORT_ORDER;
        }
        if (timerange == null) {
            timerange = DEFAULT_TIMERANGE;
        }
        if (streams == null) {
            streams = Set.of();
        }
        if (streamCategories == null) {
            streamCategories = Set.of();
        }
        if (from < 0) {
            from = DEFAULT_FROM;
        }
        if (size <= 0) {
            size = DEFAULT_SIZE;
        }
        if (fields == null || fields.isEmpty()) {
            fields = DEFAULT_FIELDS;
        }

    }

    @Deprecated
    @Override
    public List<String> fields() {
        return fieldNames();
    }

    public List<RequestedField> requestedFields() {
        return fields.stream().map(RequestedField::parse).collect(Collectors.toList());
    }

    public List<String> fieldNames() {
        return requestedFields().stream().map(RequestedField::name).collect(Collectors.toList());
    }
}
