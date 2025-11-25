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
import org.graylog.storage.search.SearchCommand;
import org.graylog2.indexer.counts.CountsAdapter;
import org.graylog2.indexer.searches.SearchesConfig;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.opensearch.client.opensearch.core.CountRequest;
import org.opensearch.client.opensearch.core.CountResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    public long totalCount(List<String> indices) {
        try {
            return client.sync().count(requestBuilder -> requestBuilder.index(indices)).count();
        } catch (IOException e) {
            throw new RuntimeException("Fetching message count failed for indices " + indices, e);
        }
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
        try {
            final CountResponse count = client.sync().count(
                    CountRequest.builder()
                            .index(new ArrayList<>(affectedIndices))
                            .query(searchRequestFactory.createQueryBuilder(SearchCommand.from(config)))
                            .build()
            );
            return count.count();
        } catch (IOException e) {
            throw new RuntimeException("Fetching message count failed", e);
        }
    }
}
