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
package org.graylog.storage.elasticsearch7;

import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog2.indexer.counts.CountsAdapter;

import javax.inject.Inject;
import java.util.List;

public class CountsAdapterES7 implements CountsAdapter {
    private final ElasticsearchClient client;

    @Inject
    public CountsAdapterES7(ElasticsearchClient client) {
        this.client = client;
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
}
