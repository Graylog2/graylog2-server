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

import com.google.common.base.Predicates;
import org.graylog2.database.filtering.Filter;
import org.graylog2.rest.resources.entities.EntityAttribute;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

public class InMemoryFilterExpressionParser {

    private final SingleFilterParser singleFilterParser = new SingleFilterParser();

    public Predicate<InMemoryFilterable> parse(final List<String> filterExpressions,
                                               final List<EntityAttribute> attributes) {
        if (filterExpressions == null || filterExpressions.isEmpty()) {
            return Predicates.alwaysTrue();
        }
        final Map<String, List<Filter>> groupedByField = filterExpressions.stream()
                .map(expr -> singleFilterParser.parseSingleExpression(expr, attributes))
                .collect(groupingBy(Filter::field));

        return groupedByField.values().stream()
                .map(grouped -> grouped.stream()
                        .map(Filter::toPredicate)
                        .collect(Collectors.toList()))
                .map(groupedPredicates -> groupedPredicates.stream().reduce(Predicate::or).orElse(Predicates.alwaysTrue()))
                .reduce(Predicate::and).orElse(Predicates.alwaysTrue());
    }

}
