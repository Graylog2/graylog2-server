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

import java.io.IOException;
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
    public CountResult count(Set<String> affectedIndices, String query, TimeRange range, String filter) {
        return null;//TODO
//        final SearchesConfig config = SearchesConfig.builder()
//                .query(query)
//                .range(range)
//                .filter(filter)
//                .limit(0)
//                .offset(0)
//                .build();
//        final SearchSourceBuilder searchSourceBuilder = searchRequestFactory.create(config);
//        final SearchRequest searchRequest = new SearchRequest(affectedIndices.toArray(new String[0]))
//                .source(searchSourceBuilder);
//
//        final SearchResponse result = client.search(searchRequest, "Fetching message count failed for indices ");
//
//        return CountResult.create(result.getHits().getTotalHits().value, result.getTook().getMillis());
    }
}
