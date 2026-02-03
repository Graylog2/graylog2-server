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
package org.graylog.storage.opensearch3;

import jakarta.inject.Inject;
import org.graylog2.indexer.counts.CountsAdapter;
import org.graylog2.indexer.results.CountResult;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CountsAdapterOS implements CountsAdapter {

    private final OfficialOpensearchClient client;
    private final SearchRequestFactoryOS searchRequestFactory;

    @Inject
    public CountsAdapterOS(final OfficialOpensearchClient client,
                           final SearchRequestFactoryOS searchRequestFactory) {
        this.client = client;
        this.searchRequestFactory = searchRequestFactory;
    }

    @Override
    public long totalCount(final List<String> indices) {

        return client.sync(c -> {
            final SearchRequest req = SearchRequest.of(sr -> sr
                    .index(indices)
                    .trackTotalHits(t -> t.enabled(true))
                    .size(0));

            final SearchResponse<Void> response = c.search(req, Void.class);
            return response.hits().total().value();
        }, "Fetching message count failed for indices ");
    }

    @Override
    public CountResult count(final Set<String> affectedIndices,
                             final String query,
                             final TimeRange range,
                             final String filter) {

        final Query queryOS = searchRequestFactory.createQuery(query, Optional.ofNullable(range), Optional.ofNullable(filter));
        return client.sync(c -> {
            final SearchRequest req = SearchRequest.of(sr -> sr
                    .index(affectedIndices.stream().toList())
                    .query(queryOS)
                    .trackTotalHits(t -> t.enabled(true))
                    .size(0)
            );
            final SearchResponse<Void> response = c.search(req, Void.class);
            return CountResult.create(response.hits().total().value(), response.took());
        }, "Fetching message count failed for indices ");
    }
}
