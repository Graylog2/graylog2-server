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

import com.floreysoft.jmte.DefaultModelAdaptor;
import com.floreysoft.jmte.TemplateContext;
import com.floreysoft.jmte.token.Token;
import com.google.common.collect.ImmutableMap;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes the search time range to template-based decorators.
 * <p>
 * The values are available in templates as properties of {@value #TEMPLATE_KEY}, e.g. {@code ${search.from}}
 * or {@code ${search.from;date(yyyy-MM-dd HH:mm)}}. Time range boundaries are exposed as {@link org.joda.time.DateTime}
 * so the {@code date} named renderer can format them, and additionally as epoch milliseconds.
 * <p>
 * To avoid collisions with message fields, the time range is stored in the template model under {@value #MODEL_KEY},
 * which is not a valid message field name, and {@link ModelAdaptor} redirects dotted {@code search.*} lookups to it.
 * A bare {@code ${search}} still resolves to a message field with that name.
 */
public final class TimeRangeTemplateContext {
    private static final String TEMPLATE_KEY = "search";
    private static final String MODEL_KEY = "#search";

    private static final String FROM = "from";
    private static final String TO = "to";
    private static final String FROM_MS = "from_ms";
    private static final String TO_MS = "to_ms";

    private final Map<String, Object> timeRange;

    private TimeRangeTemplateContext(Map<String, Object> timeRange) {
        this.timeRange = timeRange;
    }

    public static TimeRangeTemplateContext of(SearchResponse searchResponse) {
        final DateTime from = searchResponse.from().withZone(DateTimeZone.UTC);
        final DateTime to = searchResponse.to().withZone(DateTimeZone.UTC);
        return new TimeRangeTemplateContext(ImmutableMap.of(
                FROM, from,
                TO, to,
                FROM_MS, from.getMillis(),
                TO_MS, to.getMillis()
        ));
    }

    /**
     * Builds the template model for a single message. Requires an engine using {@link ModelAdaptor}.
     */
    public Map<String, Object> model(Map<String, Object> messageFields) {
        final Map<String, Object> model = new HashMap<>(messageFields);
        model.put(MODEL_KEY, timeRange);
        return model;
    }

    /**
     * @return true if the given template variable name refers to a time range property (e.g. {@code search.from}).
     * A bare {@code search} is treated as a message field.
     */
    public static boolean isContextVariable(String variableName) {
        return variableName.startsWith(TEMPLATE_KEY + ".");
    }

    /**
     * Resolves {@code search.*} variables from the time range stored under {@value #MODEL_KEY} instead of
     * from a message field named {@value #TEMPLATE_KEY}.
     */
    static class ModelAdaptor extends DefaultModelAdaptor {
        @Override
        public Object getValue(TemplateContext context, Token token, List<String> segments, String expression) {
            if (segments.size() > 1 && TEMPLATE_KEY.equals(segments.getFirst())) {
                final List<String> redirected = new ArrayList<>(segments);
                redirected.set(0, MODEL_KEY);
                return super.getValue(context, token, redirected, expression);
            }
            return super.getValue(context, token, segments, expression);
        }
    }
}
