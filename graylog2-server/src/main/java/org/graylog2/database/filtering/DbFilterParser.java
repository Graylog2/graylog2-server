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
package org.graylog2.database.filtering;

import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.stream.Collectors;

//TODO: discuss with FE format for more complex filters as well, mainly range filters
public class DbFilterParser {

    static final String FIELD_AND_VALUE_SEPARATOR = ":";
    static final String WRONG_FILTER_EXPR_FORMAT_ERROR_MSG =
            "Wrong filter expression, <field_name>" + FIELD_AND_VALUE_SEPARATOR + "<field_value> format should be used";

    public List<Bson> parse(final List<String> filterExpressions) {
        if (filterExpressions == null || filterExpressions.isEmpty()) {
            return List.of();
        }
        return filterExpressions.stream()
                .map(this::parseSingleExpression)
                .collect(Collectors.toList());

    }

    public Bson parseSingleExpression(final String filterExpression) {
        if (!filterExpression.contains(FIELD_AND_VALUE_SEPARATOR)) {
            throw new IllegalArgumentException(WRONG_FILTER_EXPR_FORMAT_ERROR_MSG);
        }
        final String[] split = filterExpression.split(FIELD_AND_VALUE_SEPARATOR, 2);

        if (split[0] == null || split[0].isEmpty()) {
            throw new IllegalArgumentException(WRONG_FILTER_EXPR_FORMAT_ERROR_MSG);
        }
        if (split[1] == null || split[1].isEmpty()) {
            throw new IllegalArgumentException(WRONG_FILTER_EXPR_FORMAT_ERROR_MSG);
        }

        return Filters.eq(split[0], split[1]);

    }


}
