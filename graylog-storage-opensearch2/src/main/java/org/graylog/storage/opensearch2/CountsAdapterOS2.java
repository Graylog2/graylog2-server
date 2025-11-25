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
import org.graylog.shaded.opensearch2.org.opensearch.client.core.CountRequest;
import org.graylog.shaded.opensearch2.org.opensearch.client.core.CountResponse;
import org.graylog.storage.search.SearchCommand;
import org.graylog2.indexer.counts.CountsAdapter;
import org.graylog2.indexer.searches.SearchesConfig;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.List;
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
        final CountResponse result = client.execute(
                (restClient, requestOptions) ->
                        restClient.count(
                                new CountRequest(indices.toArray(new String[0])),
                                requestOptions
                        ),
                "Fetching message count failed for indices"
        );
        return result.getCount();
    }

    @Override
    public long count(final Set<String> affectedIndices,
                      final String query,
                      final TimeRange range,
                      final String filter) {
        final SearchesConfig config = SearchesConfig.builder()
                .query(query)
                .filter(filter)
                .range(range)
                .limit(0)
                .offset(0)
                .build();

        final CountResponse result = client.execute(
                (restClient, requestOptions) ->
                        restClient.count(
                                new CountRequest(
                                        affectedIndices.toArray(new String[0]),
                                        searchRequestFactory.createQueryBuilder(SearchCommand.from(config))
                                ),
                                requestOptions
                        ),
                "Fetching message count failed for indices"
        );
        return result.getCount();
    }
}
