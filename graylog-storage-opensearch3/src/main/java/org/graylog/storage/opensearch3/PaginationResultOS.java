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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.indexer.results.ResultMessageFactory;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class PaginationResultOS extends ChunkedQueryResultOS {
    private final SearchRequest initialSearchRequest;

    @AssistedInject
    public PaginationResultOS(ResultMessageFactory resultMessageFactory,
                              OfficialOpensearchClient client,
                              SearchRequest initialSearchRequest,
                              @Assisted SearchResponse<Map> initialResult,
                              @Assisted("query") String query,
                              @Assisted List<String> fields,
                              @Assisted int limit) {
        super(resultMessageFactory, client, initialResult, query, fields, limit);
        this.initialSearchRequest = initialSearchRequest;
    }

    @Override
    @Nullable
    protected SearchResponse<Map> nextSearchResult() {
        final List<Hit<Map>> hits = lastSearchResponse.hits().hits();
        if (hits.isEmpty()) {
            return null;
        }

        final Hit<Map> lastHit = hits.get(hits.size() - 1);
        final List<FieldValue> sortValues = lastHit.sort();

        if (sortValues.isEmpty()) {
            return null;
        }

        final SearchRequest nextRequest = SearchRequest.of(builder -> {
            // Copy all properties from the initial request
            builder.index(initialSearchRequest.index());
            builder.query(initialSearchRequest.query());
            builder.ignoreUnavailable(initialSearchRequest.ignoreUnavailable());
            builder.allowNoIndices(initialSearchRequest.allowNoIndices());
            builder.expandWildcards(initialSearchRequest.expandWildcards());
            builder.trackTotalHits(initialSearchRequest.trackTotalHits());

            if (initialSearchRequest.source() != null) {
                builder.source(initialSearchRequest.source());
            }
            if (initialSearchRequest.size() != null) {
                builder.size(initialSearchRequest.size());
            }
            if (initialSearchRequest.sort() != null) {
                builder.sort(initialSearchRequest.sort());
            }
            if (initialSearchRequest.highlight() != null) {
                builder.highlight(initialSearchRequest.highlight());
            }

            // Set search_after with the sort values from the last hit
            builder.searchAfter(sortValues);

            return builder;
        });

        return client.sync(c -> c.search(nextRequest, Map.class), "Unable to retrieve next chunk from search: ");
    }

    @Override
    protected String getChunkingMethodName() {
        return "search-after pagination";
    }

    @Override
    public void cancel() {
        //not needed for pagination
    }

}
