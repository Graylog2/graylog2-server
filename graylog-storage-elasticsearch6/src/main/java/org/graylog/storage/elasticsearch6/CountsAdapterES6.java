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
package org.graylog.storage.elasticsearch6;

import io.searchbox.client.JestClient;
import io.searchbox.core.MultiSearch;
import io.searchbox.core.MultiSearchResult;
import io.searchbox.core.Search;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.index.query.QueryBuilders;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.elasticsearch6.jest.JestUtils;
import org.graylog2.indexer.counts.CountsAdapter;

import javax.inject.Inject;
import java.util.List;

public class CountsAdapterES6 implements CountsAdapter {
    private final JestClient jestClient;

    @Inject
    public CountsAdapterES6(JestClient jestClient) {
        this.jestClient = jestClient;
    }

    @Override
    public long totalCount(List<String> indices) {
        final String query = new SearchSourceBuilder()
                .query(QueryBuilders.matchAllQuery())
                .size(0)
                .toString();
        final Search request = new Search.Builder(query)
                .addIndex(indices)
                .build();
        final MultiSearch multiSearch = new MultiSearch.Builder(request).build();
        final MultiSearchResult searchResult = JestUtils.execute(jestClient, multiSearch, () -> "Fetching message count failed for indices " + indices);
        final List<MultiSearchResult.MultiSearchResponse> responses = searchResult.getResponses();

        long total = 0L;
        for (MultiSearchResult.MultiSearchResponse response : responses) {
            if (response.isError) {
                throw JestUtils.specificException(() -> "Fetching message count failed for indices " + indices, response.error);
            }
            total += response.searchResult.getTotal();
        }

        return total;
    }
}
