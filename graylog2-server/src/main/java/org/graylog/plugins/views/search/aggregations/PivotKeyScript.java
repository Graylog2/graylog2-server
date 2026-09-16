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
package org.graylog.plugins.views.search.aggregations;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Painless script which concatenates the values of the given fields into a single composite bucket key, used by the
 * scripted terms aggregations of the search backends.
 * <p>
 * Field names are never interpolated into the script source: they are passed as script parameters instead. Besides
 * making field names harmless, this keeps the script source identical for all pivots with the same number of fields,
 * which avoids a script compilation per field combination.
 */
public record PivotKeyScript(String source, Map<String, Object> params) {
    private static final String FIELD_PARAM_PREFIX = "field";
    private static final String MISSING_VALUE_PARAM = "missingValue";

    private static final String FIELD_EXPRESSION = """
            (doc.containsKey(params.%1$s) && doc[params.%1$s].size() > 0
            ? doc[params.%1$s].size() > 1
                ? doc[params.%1$s]
                : String.valueOf(doc[params.%1$s].value)
            : params.%2$s)
            """;

    /**
     * @param fields             the fields whose values make up the composite bucket key
     * @param keySeparatorPhrase the script expression joining two field values, including the key separator
     */
    public static PivotKeyScript create(final Collection<String> fields, final String keySeparatorPhrase) {
        final Map<String, Object> params = new LinkedHashMap<>();
        final List<String> expressions = new ArrayList<>(fields.size());
        for (final String field : fields) {
            final String fieldParam = FIELD_PARAM_PREFIX + expressions.size();
            params.put(fieldParam, field);
            expressions.add(FIELD_EXPRESSION.formatted(fieldParam, MISSING_VALUE_PARAM));
        }
        params.put(MISSING_VALUE_PARAM, MissingBucketConstants.MISSING_BUCKET_NAME);

        return new PivotKeyScript(Joiner.on(keySeparatorPhrase).join(expressions), Map.copyOf(params));
    }
}
