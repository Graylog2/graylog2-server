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
package org.graylog.plugins.views.search.engine;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.graph.Traverser;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.Parameter;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryMetadata;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.QueryStringParser;
import org.graylog.plugins.views.search.filter.QueryStringFilter;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;

public class QueryParser {
    private final QueryStringParser queryStringParser;

    public QueryParser(QueryStringParser queryStringParser) {
        this.queryStringParser = queryStringParser;
    }

    public QueryMetadata parse(ImmutableSet<Parameter> declaredParameters, Query query) {
        checkArgument(query.query() instanceof ElasticsearchQueryString);
        final String mainQueryString = ((ElasticsearchQueryString) query.query()).queryString();
        final java.util.stream.Stream<String> queryStringStreams = java.util.stream.Stream.concat(
                java.util.stream.Stream.of(mainQueryString),
                query.searchTypes().stream().flatMap(this::queryStringsFromSearchType)
        );

        return queryStringStreams
                .map(queryStringParser::parse)
                .reduce(QueryMetadata.builder().build(), (meta1, meta2) -> QueryMetadata.builder().usedParameterNames(
                        Sets.union(meta1.usedParameterNames(), meta2.usedParameterNames())
                ).build());
    }


    private java.util.stream.Stream<String> queryStringsFromSearchType(SearchType searchType) {
        return java.util.stream.Stream.concat(
                searchType.query().filter(query -> query instanceof ElasticsearchQueryString)
                        .map(query -> ((ElasticsearchQueryString) query).queryString())
                        .map(java.util.stream.Stream::of)
                        .orElse(java.util.stream.Stream.empty()),
                queryStringsFromFilter(searchType.filter()).stream()
        );
    }

    private Set<String> queryStringsFromFilter(Filter entry) {
        if (entry != null) {
            final Traverser<Filter> filterTraverser = Traverser.forTree(filter -> firstNonNull(filter.filters(), Collections.emptySet()));
            return StreamSupport.stream(filterTraverser.breadthFirst(entry).spliterator(), false)
                    .filter(filter -> filter instanceof QueryStringFilter)
                    .map(queryStringFilter -> ((QueryStringFilter) queryStringFilter).query())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }
}
