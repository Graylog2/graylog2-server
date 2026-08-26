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
import org.graylog2.indexer.results.ChunkedResult;
import org.graylog2.indexer.results.MultiChunkResultRetriever;
import org.graylog2.indexer.results.ResultMessageFactory;
import org.graylog2.indexer.searches.ChunkCommand;
import org.graylog2.indexer.searches.Sorting;
import org.opensearch.client.opensearch._types.ExpandWildcard;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import java.util.LinkedList;
import java.util.Map;

public class PaginationOS implements MultiChunkResultRetriever {
    private static final Sorting DEFAULT_SORTING = new Sorting("_doc", Sorting.Direction.ASC);

    private final ResultMessageFactory resultMessageFactory;
    private final OfficialOpensearchClient opensearchClient;
    private final SearchRequestFactoryOS searchRequestFactory;

    @Inject
    public PaginationOS(final ResultMessageFactory resultMessageFactory,
                        final OfficialOpensearchClient opensearchClient,
                        final SearchRequestFactoryOS searchRequestFactory) {
        this.resultMessageFactory = resultMessageFactory;
        this.opensearchClient = opensearchClient;
        this.searchRequestFactory = searchRequestFactory;
    }

    @Override
    public ChunkedResult retrieveChunkedResult(final ChunkCommand chunkCommand) {
        final SearchRequest request = buildSearchRequest(chunkCommand);
        final SearchResponse<Map> result = opensearchClient.sync(
                c -> c.search(request, Map.class),
                "Unable to perform search-after pagination search"
        );
        return new PaginationResultOS(resultMessageFactory, opensearchClient, request, result, request.toString(), chunkCommand.fields(), chunkCommand.limit().orElse(-1));
    }

    SearchRequest buildSearchRequest(final ChunkCommand chunkCommand) {
        final SearchRequest.Builder builder = searchRequestFactory.create(chunkCommand);

        builder.index(new LinkedList<>(chunkCommand.indices()));
        builder.ignoreUnavailable(true);
        builder.allowNoIndices(true);
        builder.expandWildcards(ExpandWildcard.Open);

        return builder.build();
    }
}
