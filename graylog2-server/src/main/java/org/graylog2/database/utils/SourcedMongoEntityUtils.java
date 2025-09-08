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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.graylog2.database.entities.SourcedMongoEntity;
import org.graylog2.database.entities.SourcedScopedEntity;
import org.graylog2.database.entities.source.EntitySource;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryParser;

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
        final Optional<String> entitySourceFilter = filterValue(filters);
        if (entitySourceFilter.isPresent()) {
            final String source = entitySourceFilter.get();
            predicate = predicate.and(entity -> sourceMatches(entity.entitySource().orElse(null), source));
            filters = removeEntitySourceFilter(filters);
        }
        return new FilterPredicate<>(filters, predicate);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends SourcedScopedEntity> QueryPredicate<T> handleScopedEntitySourceFilter(SearchQuery searchQuery, Predicate<T> predicate) {
        final Multimap<String, SearchQueryParser.FieldValue> queryMap = searchQuery.getQueryMap();
        if (queryMap.containsKey(FILTERABLE_FIELD)) {
            final Optional<SearchQueryParser.FieldValue> fieldValue = queryMap.get(FILTERABLE_FIELD).stream().findFirst();
            if (fieldValue.isPresent()) {
                final String source = fieldValue.get().getValue().toString();
                predicate = predicate.and(entity -> sourceMatches(entity.entitySource().orElse(null), source));
                final Multimap<String, SearchQueryParser.FieldValue> updatedQueryMap = HashMultimap.create();
                queryMap.entries().forEach(entry -> {
                    if (!FILTERABLE_FIELD.equals(entry.getKey())) {
                        updatedQueryMap.put(entry.getKey(), entry.getValue());
                    }
                });
                searchQuery = new SearchQuery(searchQuery.getQueryString(), updatedQueryMap, searchQuery.getDisallowedKeys());
            }
        }
        return new QueryPredicate<>(searchQuery, predicate);
    }

    private static Optional<String> filterValue(List<String> filters) {
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
