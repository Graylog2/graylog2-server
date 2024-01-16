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
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog2.indexer.results.ChunkedResult;
import org.graylog2.indexer.results.MultiChunkResultRetriever;
import org.graylog2.indexer.searches.ChunkCommand;

import jakarta.inject.Inject;

import java.util.Set;

public class PaginationES7 implements MultiChunkResultRetriever {
    private final ElasticsearchClient client;
    private final SearchRequestFactory searchRequestFactory;

    @Inject
    public PaginationES7(final ElasticsearchClient client,
                         final SearchRequestFactory searchRequestFactory) {
        this.client = client;
        this.searchRequestFactory = searchRequestFactory;
    }

    @Override
    public ChunkedResult retrieveChunkedResult(final ChunkCommand chunkCommand) {
        final SearchSourceBuilder searchQuery = searchRequestFactory.create(chunkCommand);
        final SearchRequest request = buildSearchRequest(searchQuery, chunkCommand.indices());
        final SearchResponse result = client.search(request, "Unable to perform search-after pagination search");
        return new PaginationResultES7(client, request, result, searchQuery.toString(), chunkCommand.fields(), chunkCommand.limit().orElse(-1));
    }

    private SearchRequest buildSearchRequest(final SearchSourceBuilder query,
                                             final Set<String> indices) {
        return new SearchRequest(indices.toArray(new String[0]))
                .source(query);
    }
}
