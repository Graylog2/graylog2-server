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
import org.graylog2.indexer.results.ChunkedResult;
import org.graylog2.indexer.results.MultiChunkResultRetriever;
import org.graylog2.indexer.searches.ChunkCommand;
import org.opensearch.client.opensearch._types.ExpandWildcard;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

@Deprecated
public class Scroll implements MultiChunkResultRetriever {
    private static final String DEFAULT_SCROLLTIME = "1m";
    private final OfficialOpensearchClient opensearchClient;
    private final ScrollResultOS3.Factory scrollResultFactory;
    private final SearchRequestFactoryOS searchRequestFactory;

    @Inject
    public Scroll(OfficialOpensearchClient opensearchClient,
                  ScrollResultOS3.Factory scrollResultFactory,
                  SearchRequestFactoryOS searchRequestFactory) {
        this.opensearchClient = opensearchClient;
        this.scrollResultFactory = scrollResultFactory;
        this.searchRequestFactory = searchRequestFactory;
    }

    @Override
    public ChunkedResult retrieveChunkedResult(ChunkCommand chunkCommand) {
        final Query query = createQuery(chunkCommand);
        final SearchRequest request = buildScrollRequest(query, chunkCommand);
        final SearchResponse<Map> result = opensearchClient.sync(
                c -> c.search(request, Map.class),
                "Unable to perform scroll search"
        );
        return scrollResultFactory.create(result, request.toString(), DEFAULT_SCROLLTIME, chunkCommand.fields(), chunkCommand.limit().orElse(-1));
    }

    Query createQuery(final ChunkCommand chunkCommand) {
        final SearchCommand searchCommand = SearchCommand.from(chunkCommand);
        return searchRequestFactory.createQuery(searchCommand);
    }

    SearchRequest buildScrollRequest(Query query, ChunkCommand chunkCommand) {
        final Set<String> indices = chunkCommand.indices();

        final Time scrollTime = new Time.Builder().time(DEFAULT_SCROLLTIME).build();

        return SearchRequest.of(builder -> {
            builder.index(new LinkedList<>(indices));
            builder.query(query);
            builder.scroll(scrollTime);
            builder.ignoreUnavailable(true);
            builder.allowNoIndices(true);
            builder.expandWildcards(ExpandWildcard.Open);
            builder.trackTotalHits(th -> th.enabled(true));

            // Set source fields
            if (!chunkCommand.fields().isEmpty()) {
                builder.source(s -> s.filter(sf -> sf.includes(new LinkedList<>(chunkCommand.fields()))));
            }

            // Set slice parameters
            chunkCommand.sliceParams().ifPresent(sliceParams ->
                    builder.slice(slice -> slice.id(sliceParams.id()).max(sliceParams.max()))
            );

            // Set pagination parameters
            // IMPORTANT: Don't use 'from' (offset) with slice parameters - they're incompatible in OpenSearch/Elasticsearch
            if (chunkCommand.sliceParams().isEmpty()) {
                chunkCommand.offset().ifPresent(offset -> builder.from(offset));
            }

            // Set batch size, or use limit as fallback if batchSize is absent
            if (chunkCommand.batchSize().isPresent()) {
                builder.size(Math.toIntExact(chunkCommand.batchSize().getAsLong()));
            } else {
                chunkCommand.limit().ifPresent(builder::size);
            }

            return builder;
        });
    }
}
