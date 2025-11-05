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

import java.util.Arrays;
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

    /**
     * Handles the _entity_source filtering for entities that have not yet been converted to use the common entity
     * table on the frontend and still supply filters as a string. The filters for each field are separated by a
     * semicolon ';'. The filter values are comma-delimited. An example filter string with _entity_source.source:
     * filter1:'filter_value1';_entity_source.source:'ILLUMINATE';filter3:'value1,value2,value3'
     * <p>
     * The caller will be responsible for rejoining the list of filter strings (FilterPredicate.filters) with the
     * necessary delimiter.
     *
     * @param filters   string of filters separated by ';'.
     * @param predicate existing predicate for the SourcedScopedEntity
     * @param <T>       class that implements SourcedScopedEntity
     * @return modified filters and predicate that properly apply _entity_source.source filtering
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends SourcedScopedEntity> FilterPredicate<T> handleScopedEntitySourceFilter(String filters,
                                                                                                    Predicate<T> predicate) {
        List<String> cleanedFilters = Arrays.asList(filters.split(";"));
        final List<String> entitySourceFilter = filterValues(cleanedFilters);
        if (!entitySourceFilter.isEmpty()) {
            // entitySourceFilters should only have a size of 1 since it will be a comma-delimited string
            final String sourceString = entitySourceFilter.getFirst();

            final List<String> sourceFilters = List.of(sourceString.substring(1, sourceString.length() - 1).split(","));
            predicate = predicate.and(entity -> sourceFilters.stream()
                    .anyMatch(source -> sourceMatches(entity.entitySource().orElse(null), source)));
            cleanedFilters = removeEntitySourceFilter(cleanedFilters);
        }
        return new FilterPredicate<>(cleanedFilters, predicate);
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
