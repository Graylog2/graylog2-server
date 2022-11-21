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
package org.graylog.plugins.views.search.rest.scriptingapi.mapping;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.scriptingapi.request.SearchRequestSpec;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

public class SearchRequestSpecToSearchMapper {

    public static final String QUERY_ID = "scripting_api_temporary_query";

    private final AggregationSpecToPivotMapper pivotCreator;

    @Inject
    public SearchRequestSpecToSearchMapper(final AggregationSpecToPivotMapper pivotCreator) {
        this.pivotCreator = pivotCreator;
    }

    public Search mapToSearch(final SearchRequestSpec searchRequestSpec, final SearchUser searchUser) {

        Query query = Query.builder()
                .id(QUERY_ID)
                .searchTypes(Set.of(pivotCreator.apply(searchRequestSpec)))
                .query(searchRequestSpec.queryString() != null ? ElasticsearchQueryString.of(searchRequestSpec.queryString()) : ElasticsearchQueryString.empty())
                .timerange(searchRequestSpec.timerange() != null ? searchRequestSpec.timerange() : RelativeRange.allTime())
                .build();

        if (!searchRequestSpec.streams().isEmpty()) {
            query = query.addStreamsToFilter(new HashSet<>(searchRequestSpec.streams()));
        }

        Search search = Search.builder()
                .queries(ImmutableSet.of(query))
                .build();


        search = search.addStreamsToQueriesWithoutStreams(() -> searchUser.streams().readableOrAllIfEmpty(searchRequestSpec.streams()));
        return search;
    }
}
