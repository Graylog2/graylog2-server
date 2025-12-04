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
package org.graylog.storage.opensearch2;

import jakarta.inject.Inject;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.builder.SearchSourceBuilder;
import org.graylog2.indexer.counts.CountsAdapter;
import org.graylog2.indexer.results.CountResult;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CountsAdapterOS2 implements CountsAdapter {
    private final OpenSearchClient client;
    private final SearchRequestFactory searchRequestFactory;

    @Inject
    public CountsAdapterOS2(final OpenSearchClient client,
                            final SearchRequestFactory searchRequestFactory) {
        this.client = client;
        this.searchRequestFactory = searchRequestFactory;
    }

    @Override
    public long totalCount(List<String> indices) {
        final SearchSourceBuilder query = new SearchSourceBuilder()
                .query(QueryBuilders.matchAllQuery())
                .size(0)
                .trackTotalHits(true);
        final SearchRequest searchRequest = new SearchRequest(indices.toArray(new String[0]))
                .source(query);

        final SearchResponse result = client.search(searchRequest, "Fetching message count failed for indices ");

        return result.getHits().getTotalHits().value;
    }

    @Override
    public CountResult count(final Set<String> affectedIndices,
                             final String query,
                             final TimeRange range,
                             final String filter) {
        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(searchRequestFactory.createQueryBuilder(query, Optional.ofNullable(range), Optional.ofNullable(filter)))
                .size(0)
                .trackTotalHits(true);
        final SearchRequest searchRequest = new SearchRequest(affectedIndices.toArray(new String[0]))
                .source(searchSourceBuilder);
        final SearchResponse result = client.search(searchRequest, "Fetching message count failed for indices ");

        return CountResult.create(result.getHits().getTotalHits().value, result.getTook().getMillis());
    }
}
