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
package org.graylog2.database.utils;

import org.graylog2.database.entities.SourcedMongoEntity;
import org.graylog2.database.entities.source.EntitySource;

import java.util.List;
import java.util.Optional;

import static org.graylog2.database.filtering.inmemory.SingleFilterParser.FIELD_AND_VALUE_SEPARATOR;
import static org.graylog2.database.filtering.inmemory.SingleFilterParser.WRONG_FILTER_EXPR_FORMAT_ERROR_MSG;

public class SourcedMongoEntityUtils {
    public static String FILTERABLE_FIELD = SourcedMongoEntity.FIELD_ENTITY_SOURCE + "." + EntitySource.FIELD_SOURCE;

    public static Optional<String> filterValue(List<String> filters) {
        final Optional<String> filter = filters.stream()
                .filter(f -> f.startsWith(FILTERABLE_FIELD + FIELD_AND_VALUE_SEPARATOR))
                .findFirst();
        String value = null;
        if (filter.isPresent()) {
            final String[] split = filter.get().split(FIELD_AND_VALUE_SEPARATOR, 2);

            value = split[1];
            if (value == null || value.isEmpty()) {
                throw new IllegalArgumentException(WRONG_FILTER_EXPR_FORMAT_ERROR_MSG);
            }
        }
        return Optional.ofNullable(value);
    }

    public static List<String> removeEntitySourceFilter(List<String> filters) {
        return filters.stream()
                .filter(f -> !f.startsWith(FILTERABLE_FIELD + FIELD_AND_VALUE_SEPARATOR))
                .toList();
    }
}
