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
import org.graylog2.rest.resources.entities.EntityAttribute;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

//TODO: discuss with FE format for more complex filters as well, mainly range filters
public class DbFilterParser {

    static final String FIELD_AND_VALUE_SEPARATOR = ":";
    static final String WRONG_FILTER_EXPR_FORMAT_ERROR_MSG =
            "Wrong filter expression, <field_name>" + FIELD_AND_VALUE_SEPARATOR + "<field_value> format should be used";

    private record SingleValueFilter(String field, Object value) {

        Bson toBson() {
            return Filters.eq(field(), value());
        }
    }

    public List<Bson> parse(final List<String> filterExpressions,
                            final List<EntityAttribute> attributes) {
        if (filterExpressions == null || filterExpressions.isEmpty()) {
            return List.of();
        }
        final Map<String, List<SingleValueFilter>> groupedByField = filterExpressions.stream()
                .map(expr -> parseSingleExpressionInner(expr, attributes))
                .collect(groupingBy(SingleValueFilter::field));

        return groupedByField.values().stream()
                .map(grouped -> grouped.stream()
                        .map(SingleValueFilter::toBson)
                        .collect(Collectors.toList()))
                .map(groupedFilters -> {
                    if (groupedFilters.size() == 1) {
                        return groupedFilters.get(0);
                    } else {
                        return Filters.or(groupedFilters);
                    }
                })
                .toList();
    }

    public Bson parseSingleExpression(final String filterExpression, final List<EntityAttribute> attributes) {
        final SingleValueFilter filter = parseSingleExpressionInner(filterExpression, attributes);
        return Filters.eq(filter.field(), filter.value());
    }

    private SingleValueFilter parseSingleExpressionInner(final String filterExpression, final List<EntityAttribute> attributes) {
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

        final EntityAttribute attributeMetaData = getAttributeMetaData(attributes, split[0]);

        return new SingleValueFilter(attributeMetaData.id(), attributeMetaData.type().getMongoValueConverter().apply(split[1]));

    }

    private EntityAttribute getAttributeMetaData(final List<EntityAttribute> attributes,
                                                 final String attributeName) {
        EntityAttribute matchingByTitle = null;

        for (EntityAttribute attr : attributes) {
            if (attributeName.equals(attr.id()) && isFilterable(attr)) {
                return attr;
            } else if (attributeName.equalsIgnoreCase(attr.title()) && isFilterable(attr)) {
                matchingByTitle = attr;
            }
        }

        if (matchingByTitle != null) {
            return matchingByTitle;
        } else {
            throw new IllegalArgumentException(attributeName + " is not a field that can be used for filtering");
        }

    }

    private boolean isFilterable(final EntityAttribute attr) {
        return attr.filterable() != null && attr.filterable();
    }

}
