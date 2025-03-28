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
import org.graylog.shaded.opensearch2.org.opensearch.action.search.MultiSearchRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.support.IndicesOptions;
import org.graylog.shaded.opensearch2.org.opensearch.search.builder.SearchSourceBuilder;
import org.graylog2.indexer.results.ChunkedResult;
import org.graylog2.indexer.results.MultiChunkResultRetriever;
import org.graylog2.indexer.results.ResultMessageFactory;
import org.graylog2.indexer.searches.ChunkCommand;

import java.util.List;
import java.util.Set;

public class PaginationOS2 implements MultiChunkResultRetriever {
    private final ResultMessageFactory resultMessageFactory;
    private final OpenSearchClient client;
    private final SearchRequestFactory searchRequestFactory;

    @Inject
    public PaginationOS2(final ResultMessageFactory resultMessageFactory,
                         final OpenSearchClient client,
                         final SearchRequestFactory searchRequestFactory) {
        this.resultMessageFactory = resultMessageFactory;
        this.client = client;
        this.searchRequestFactory = searchRequestFactory;
    }

    @Override
    public ChunkedResult retrieveChunkedResult(final ChunkCommand chunkCommand) {
        final SearchSourceBuilder searchQuery = searchRequestFactory.create(chunkCommand);
        final SearchRequest request = buildSearchRequest(searchQuery, chunkCommand.indices());
        // doing a msearch so that it results in a POST so we don't have to deal with possible errors where the request exceeds HTTP parameter lengths
        final var result = client.msearch(List.of(request), "Unable to perform search-after pagination search");
        if(result.size() != 1) {
            // we put in one request, so we expect exactly one result to come back
            throw new RuntimeException("MSearch with one request should result in exactly one result, but was: " + result.size());
        }
        return new PaginationResultOS2(resultMessageFactory, client, request, result.get(0).getResponse(), searchQuery.toString(), chunkCommand.fields(), chunkCommand.limit().orElse(-1));
    }

    private SearchRequest buildSearchRequest(final SearchSourceBuilder query,
                                             final Set<String> indices) {
        return new SearchRequest(indices.toArray(new String[0]))
                .source(query)
                .indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);
    }
}
