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
package org.graylog2.decorators;

import com.google.common.collect.ImmutableMap;
import org.graylog2.rest.resources.search.responses.SearchResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Exposes the search time range to template-based decorators.
 * <p>
 * The values are available in templates under the reserved {@value #KEY} key, e.g. {@code ${search.from}}
 * or {@code ${search.from;date(yyyy-MM-dd HH:mm)}}. Time range boundaries are exposed as {@link org.joda.time.DateTime}
 * so the {@code date} named renderer can format them, and additionally as epoch milliseconds.
 */
public final class TimeRangeTemplateContext {
    public static final String KEY = "search";

    private static final String FROM = "from";
    private static final String TO = "to";
    private static final String FROM_MS = "from_ms";
    private static final String TO_MS = "to_ms";

    private TimeRangeTemplateContext() {
    }

    public static Map<String, Object> from(SearchResponse searchResponse) {
        return ImmutableMap.<String, Object>builder()
                .put(FROM, searchResponse.from())
                .put(TO, searchResponse.to())
                .put(FROM_MS, searchResponse.from().getMillis())
                .put(TO_MS, searchResponse.to().getMillis())
                .build();
    }

    /**
     * Builds the template model for a single message. A message field with the same name as {@link #KEY} takes
     * precedence over the time range, so existing templates referring to such a field keep working.
     */
    public static Map<String, Object> model(Map<String, Object> messageFields, Map<String, Object> timeRange) {
        final Map<String, Object> model = new HashMap<>(messageFields);
        model.putIfAbsent(KEY, timeRange);
        return model;
    }

    /**
     * @return true if the given template variable name refers to a time range property (e.g. {@code search.from}).
     * A bare {@code search} is treated as a message field.
     */
    public static boolean isContextVariable(String variableName) {
        return variableName.startsWith(KEY + ".");
    }
}
