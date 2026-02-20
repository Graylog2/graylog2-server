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
import org.bson.types.ObjectId;
import org.graylog2.database.filtering.inmemory.SingleFilterParser;
import org.graylog2.rest.resources.entities.EntityAttribute;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;

public class DbFilterExpressionParser {

    private final SingleFilterParser singleFilterParser = new SingleFilterParser();
    private final ComputedFieldRegistry computedFieldRegistry;

    public DbFilterExpressionParser(final ComputedFieldRegistry computedFieldRegistry) {
        this.computedFieldRegistry = computedFieldRegistry;
    }

    public List<Bson> parse(final List<String> filterExpressions,
                            final List<EntityAttribute> attributes) {
        return parse(filterExpressions, attributes, null);
    }

    public List<Bson> parse(final List<String> filterExpressions,
                            final List<EntityAttribute> attributes,
                            final String authToken) {
        if (filterExpressions == null || filterExpressions.isEmpty()) {
            return List.of();
        }

        final List<String> computedFieldExpressions = new ArrayList<>();
        final List<String> dbFieldExpressions = new ArrayList<>();

        for (String expr : filterExpressions) {
            String fieldName = extractFieldName(expr);
            if (computedFieldRegistry != null && computedFieldRegistry.isComputedField(fieldName)) {
                computedFieldExpressions.add(expr);
            } else {
                dbFieldExpressions.add(expr);
            }
        }

        final List<Bson> dbFilters = processDbFieldFilters(dbFieldExpressions, attributes);

        if (computedFieldExpressions.isEmpty()) {
            return dbFilters;
        }

        final Bson computedFieldFilter = processComputedFieldFilters(computedFieldExpressions, authToken);
        final List<Bson> allFilters = new ArrayList<>(dbFilters);
        allFilters.add(computedFieldFilter);

        return allFilters;
    }

    /**
     * Extracts the field name from a filter expression (e.g., "runtime_status:RUNNING" -> "runtime_status")
     */
    private String extractFieldName(String filterExpression) {
        int colonIndex = filterExpression.indexOf(':');
        if (colonIndex > 0) {
            return filterExpression.substring(0, colonIndex);
        }
        return filterExpression;
    }

    private List<Bson> processDbFieldFilters(final List<String> dbFieldExpressions,
                                             final List<EntityAttribute> attributes) {
        if (dbFieldExpressions.isEmpty()) {
            return List.of();
        }

        final Map<String, List<Filter>> groupedByField = dbFieldExpressions.stream()
                .map(expr -> singleFilterParser.parseSingleExpression(expr, attributes))
                .collect(groupingBy(Filter::field));

        return groupedByField.values().stream()
                .map(grouped -> grouped.stream()
                        .map(Filter::toBson)
                        .toList())
                .map(groupedFilters -> {
                    if (groupedFilters.size() == 1) {
                        return groupedFilters.get(0);
                    } else {
                        return Filters.or(groupedFilters);
                    }
                })
                .toList();
    }

    /**
     * Processes computed field filters by delegating to ComputedFieldProviders
     * and building an $in filter on entity IDs.
     */
    private Bson processComputedFieldFilters(final List<String> computedFieldExpressions,
                                             final String authToken) {
        // Group computed field expressions by field name
        final Map<String, List<String>> groupedByField = computedFieldExpressions.stream()
                .collect(groupingBy(this::extractFieldName));

        // Start with all possible IDs (null means no constraint yet)
        Set<String> resultIds = null;

        for (Map.Entry<String, List<String>> entry : groupedByField.entrySet()) {
            final String fieldName = entry.getKey();
            final ComputedFieldProvider provider = computedFieldRegistry.getProvider(fieldName)
                    .orElseThrow(() -> new IllegalStateException(
                            "No provider found for computed field: " + fieldName));

            // Get matching IDs for all values (OR within same field)
            final Set<String> fieldMatches = new HashSet<>();
            for (String expr : entry.getValue()) {
                String value = extractFieldValue(expr);
                try {
                    Set<String> matchingIds = provider.getMatchingIds(value, authToken);
                    fieldMatches.addAll(matchingIds);
                } catch (Exception e) {
                    throw new IllegalStateException(
                            "Filtering failed for computed field '" + fieldName + "' with value '" + value + "'", e);
                }
            }

            // AND across different fields (intersection)
            if (resultIds == null) {
                resultIds = fieldMatches;
            } else {
                resultIds.retainAll(fieldMatches);
            }
        }

        if (resultIds == null || resultIds.isEmpty()) {
            // Use an impossible ObjectId to ensure no results
            return Filters.eq("_id", new ObjectId("000000000000000000000000"));
        } else {
            final List<ObjectId> objectIds = resultIds.stream()
                    .sorted()
                    .map(ObjectId::new).toList();
            return Filters.in("_id", objectIds);
        }
    }

    /**
     * Extracts the value from a filter expression (e.g., "runtime_status:RUNNING" -> "RUNNING")
     */
    private String extractFieldValue(String filterExpression) {
        int colonIndex = filterExpression.indexOf(':');
        if (colonIndex > 0 && colonIndex < filterExpression.length() - 1) {
            return filterExpression.substring(colonIndex + 1);
        }
        return "";
    }

    public Bson parseSingleExpression(final String filterExpression, final List<EntityAttribute> attributes) {
        final Filter filter = singleFilterParser.parseSingleExpression(filterExpression, attributes);
        return filter.toBson();
    }



}
