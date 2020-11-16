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
package org.graylog2.search;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.mongojack.DBQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SearchQuery {
    private final Multimap<String, SearchQueryParser.FieldValue> queryMap;
    private final Set<String> disallowedKeys;
    private final String queryString;

    public SearchQuery(String queryString) {
        this(queryString, HashMultimap.create(), Collections.emptySet());
    }

    public SearchQuery(String queryString, Multimap<String, SearchQueryParser.FieldValue> queryMap, Set<String> disallowedKeys) {
        this.queryString = queryString;
        this.queryMap = queryMap;
        this.disallowedKeys = disallowedKeys;
    }

    public Multimap<String, SearchQueryParser.FieldValue> getQueryMap() {
        return queryMap;
    }

    public DBQuery.Query toDBQuery() {
        if (queryMap.isEmpty()) {
            return DBQuery.empty();
        }

        final List<DBQuery.Query> dbQueries = new ArrayList<>();

        for (Map.Entry<String, Collection<SearchQueryParser.FieldValue>> entry : queryMap.asMap().entrySet()) {
            final List<DBQuery.Query> queries = new ArrayList<>();

            final List<SearchQueryParser.FieldValue> include = selectValues(entry.getValue(), value -> !value.isNegate());
            final List<SearchQueryParser.FieldValue> exclude = selectValues(entry.getValue(), SearchQueryParser.FieldValue::isNegate);

            if (!include.isEmpty()) {
                queries.add(DBQuery.or(toQuery(entry.getKey(), include)));
            }
            if (!exclude.isEmpty()) {
                queries.add(DBQuery.nor(toQuery(entry.getKey(), exclude)));
            }

            dbQueries.add(DBQuery.and(queries.toArray(new DBQuery.Query[0])));
        }

        return DBQuery.and(dbQueries.toArray(new DBQuery.Query[0]));
    }

    private DBQuery.Query[] toQuery(String field, List<SearchQueryParser.FieldValue> values) {
        return values.stream()
                .map(value -> value.getOperator().buildQuery(field, value.getValue()))
                .collect(Collectors.toList())
                .toArray(new DBQuery.Query[0]);
    }

    private List<SearchQueryParser.FieldValue> selectValues(Collection<SearchQueryParser.FieldValue> values,
                                                            Function<SearchQueryParser.FieldValue, Boolean> callback) {
        return values.stream()
                .filter(callback::apply)
                .collect(Collectors.toList());
    }

    public String getQueryString() {
        return queryString;
    }

    public Set<String> getDisallowedKeys() {
        return disallowedKeys;
    }

    public boolean hasDisallowedKeys() {
        return !disallowedKeys.isEmpty();
    }
}
