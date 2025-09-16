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
import org.graylog2.database.entities.SourcedScopedEntity;
import org.graylog2.database.entities.source.EntitySource;
import org.graylog2.search.SearchQuery;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.graylog2.database.filtering.inmemory.SingleFilterParser.FIELD_AND_VALUE_SEPARATOR;
import static org.graylog2.database.filtering.inmemory.SingleFilterParser.WRONG_FILTER_EXPR_FORMAT_ERROR_MSG;

public class SourcedMongoEntityUtils {
    public static String SEARCH_QUERY_TITLE = EntitySource.FIELD_SOURCE;
    public static String FILTERABLE_FIELD = SourcedMongoEntity.FIELD_ENTITY_SOURCE + "." + EntitySource.FIELD_SOURCE;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends SourcedMongoEntity> FilterPredicate<T> handleEntitySourceFilter(List<String> filters,
                                                                                             Predicate<T> predicate) {
        final List<String> entitySourceFilters = filterValues(filters);
        if (!entitySourceFilters.isEmpty()) {
            predicate = predicate.and(entity -> entitySourceFilters.stream()
                    .anyMatch(source -> sourceMatches(entity.entitySource().orElse(null), source)));
            filters = removeEntitySourceFilter(filters);
        }
        return new FilterPredicate<>(filters, predicate);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends SourcedScopedEntity> FilterPredicate<T> handleScopedEntitySourceFilter(List<String> filters,
                                                                                                    Predicate<T> predicate) {
        final List<String> entitySourceFilters = filterValues(filters);
        if (!entitySourceFilters.isEmpty()) {
            predicate = predicate.and(entity -> entitySourceFilters.stream()
                    .anyMatch(source -> sourceMatches(entity.entitySource().orElse(null), source)));
            filters = removeEntitySourceFilter(filters);
        }
        return new FilterPredicate<>(filters, predicate);
    }

    private static List<String> filterValues(List<String> filters) {
        return filters.stream()
                .filter(f -> f.startsWith(FILTERABLE_FIELD + FIELD_AND_VALUE_SEPARATOR))
                .map(f -> {
                    String[] split = f.split(FIELD_AND_VALUE_SEPARATOR, 2);
                    String value = (split.length > 1) ? split[1] : null;
                    if (value == null || value.isEmpty()) {
                        throw new IllegalArgumentException(WRONG_FILTER_EXPR_FORMAT_ERROR_MSG);
                    }
                    return Optional.of(value);
                })
                .map(Optional::get)
                .toList();
    }

    private static List<String> removeEntitySourceFilter(List<String> filters) {
        return filters.stream()
                .filter(f -> !f.startsWith(FILTERABLE_FIELD + FIELD_AND_VALUE_SEPARATOR))
                .toList();
    }

    private static boolean sourceMatches(Object o, String source) {
        if (o == null) {
            return source.equals(EntitySource.USER_DEFINED);
        }
        if (o instanceof EntitySource entitySource) {
            return entitySource.source().equals(source);
        }
        return false;
    }

    public record FilterPredicate<T>(List<String> filters, Predicate<T> predicate) {}

    public record QueryPredicate<T>(SearchQuery query, Predicate<T> predicate) {}
}
