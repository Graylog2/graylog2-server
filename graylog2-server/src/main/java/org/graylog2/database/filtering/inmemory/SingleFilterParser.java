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
package org.graylog2.database.filtering.inmemory;

import org.graylog2.database.filtering.Filter;
import org.graylog2.database.filtering.RangeFilter;
import org.graylog2.database.filtering.SingleValueFilter;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.search.SearchQueryField;
import org.joda.time.DateTime;

import java.util.List;

public class SingleFilterParser {

    public static final String FIELD_AND_VALUE_SEPARATOR = ":";
    public static final String RANGE_VALUES_SEPARATOR = "><";
    static final String WRONG_FILTER_EXPR_FORMAT_ERROR_MSG =
            "Wrong filter expression, <field_name>" + FIELD_AND_VALUE_SEPARATOR + "<field_value> format should be used";

    public Filter parseSingleExpression(final String filterExpression, final List<EntityAttribute> attributes) {
        if (!filterExpression.contains(FIELD_AND_VALUE_SEPARATOR)) {
            throw new IllegalArgumentException(WRONG_FILTER_EXPR_FORMAT_ERROR_MSG);
        }
        final String[] split = filterExpression.split(FIELD_AND_VALUE_SEPARATOR, 2);

        final String fieldPart = split[0];
        if (fieldPart == null || fieldPart.isEmpty()) {
            throw new IllegalArgumentException(WRONG_FILTER_EXPR_FORMAT_ERROR_MSG);
        }
        final String valuePart = split[1];
        if (valuePart == null || valuePart.isEmpty()) {
            throw new IllegalArgumentException(WRONG_FILTER_EXPR_FORMAT_ERROR_MSG);
        }

        final EntityAttribute attributeMetaData = getAttributeMetaData(attributes, fieldPart);

        final SearchQueryField.Type fieldType = attributeMetaData.type();
        if (isRangeValueExpression(valuePart, fieldType)) {
            if (valuePart.startsWith(RANGE_VALUES_SEPARATOR)) {
                return new RangeFilter(attributeMetaData.id(),
                        null,
                        extractValue(fieldType, valuePart.substring(RANGE_VALUES_SEPARATOR.length()))
                );
            } else if (valuePart.endsWith(RANGE_VALUES_SEPARATOR)) {
                return new RangeFilter(attributeMetaData.id(),
                        extractValue(fieldType, valuePart.substring(0, valuePart.length() - RANGE_VALUES_SEPARATOR.length())),
                        null
                );
            } else {
                final String[] ranges = valuePart.split(RANGE_VALUES_SEPARATOR);
                return new RangeFilter(attributeMetaData.id(),
                        extractValue(fieldType, ranges[0]),
                        extractValue(fieldType, ranges[1])
                );
            }
        } else {
            return new SingleValueFilter(attributeMetaData.id(), extractValue(fieldType, valuePart));
        }

    }

    private Object extractValue(SearchQueryField.Type fieldType, String valuePart) {
        final Object converted = fieldType.getMongoValueConverter().apply(valuePart);
        if (converted instanceof DateTime && fieldType == SearchQueryField.Type.DATE) {
            return ((DateTime) converted).toDate(); //MongoDB does not like Joda
        } else {
            return converted;
        }

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

    private boolean isRangeValueExpression(String valuePart, SearchQueryField.Type fieldType) {
        return SearchQueryField.Type.NUMERIC_TYPES.contains(fieldType) && valuePart.contains(RANGE_VALUES_SEPARATOR);
    }

    private boolean isFilterable(final EntityAttribute attr) {
        return Boolean.TRUE.equals(attr.filterable());
    }
}
