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
import jakarta.inject.Named;
import org.graylog.storage.search.SearchCommand;
import org.graylog2.indexer.results.ChunkedResult;
import org.graylog2.indexer.results.MultiChunkResultRetriever;
import org.graylog2.indexer.results.ResultMessageFactory;
import org.graylog2.indexer.searches.ChunkCommand;
import org.graylog2.indexer.searches.Sorting;
import org.opensearch.client.opensearch._types.ExpandWildcard;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class PaginationOS implements MultiChunkResultRetriever {
    private static final Sorting DEFAULT_SORTING = new Sorting("_doc", Sorting.Direction.ASC);

    private final ResultMessageFactory resultMessageFactory;
    private final OfficialOpensearchClient opensearchClient;
    private final SearchRequestFactoryOS searchRequestFactory;
    private final boolean allowHighlighting;

    @Inject
    public PaginationOS(final ResultMessageFactory resultMessageFactory,
                        final OfficialOpensearchClient opensearchClient,
                        final SearchRequestFactoryOS searchRequestFactory,
                        @Named("allow_highlighting") final boolean allowHighlighting) {
        this.resultMessageFactory = resultMessageFactory;
        this.opensearchClient = opensearchClient;
        this.searchRequestFactory = searchRequestFactory;
        this.allowHighlighting = allowHighlighting;
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
        final Query query = createQuery(chunkCommand);
        final Set<String> indices = chunkCommand.indices();

        return SearchRequest.of(builder -> {
            builder.index(new LinkedList<>(indices));
            builder.query(query);
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

            // Set pagination - apply limit first, then let batchSize override if present
            final SearchCommand searchCommand = SearchCommand.from(chunkCommand);
            // IMPORTANT: Don't use 'from' (offset) with slice parameters - they're incompatible in OpenSearch/Elasticsearch
            // Combining them can cause incorrect results and duplicate hit counts
            if (chunkCommand.sliceParams().isEmpty()) {
                searchCommand.offset().ifPresent(builder::from);
            }
            searchCommand.limit().ifPresent(builder::size);
            chunkCommand.batchSize().ifPresent(batchSize -> builder.size(Math.toIntExact(batchSize)));

            // Set sorting
            final Sorting sorting = searchCommand.sorting().orElse(DEFAULT_SORTING);
            final SortOrder sortOrder = sorting.getDirection() == Sorting.Direction.ASC
                    ? SortOrder.Asc
                    : SortOrder.Desc;
            builder.sort(sort -> sort.field(field -> field
                    .field(sorting.getField())
                    .order(sortOrder)
            ));

            // Set highlighting
            applyHighlighting(builder, searchCommand);

            return builder;
        });
    }

    Query createQuery(final ChunkCommand chunkCommand) {
        final SearchCommand searchCommand = SearchCommand.from(chunkCommand);
        return searchRequestFactory.createQuery(searchCommand);
    }

    private void applyHighlighting(final SearchRequest.Builder builder, final SearchCommand searchCommand) {
        if (allowHighlighting && searchCommand.highlight()) {
            builder.highlight(h -> h
                    .requireFieldMatch(false)
                    .fields("*", f -> f.fragmentSize(0).numberOfFragments(0))
            );
        }
    }
}
