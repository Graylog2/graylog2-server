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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.search.SearchHit;
import org.graylog.shaded.opensearch2.org.opensearch.search.builder.SearchSourceBuilder;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

public class PaginationResultOS2 extends ChunkedQueryResultOS2 {
    private final SearchRequest initialSearchRequest;

    @AssistedInject
    public PaginationResultOS2(OpenSearchClient client,
                               SearchRequest initialSearchRequest,
                               @Assisted SearchResponse initialResult,
                               @Assisted("query") String query,
                               @Assisted List<String> fields,
                               @Assisted int limit) {
        super(client, initialResult, query, fields, limit);
        this.initialSearchRequest = initialSearchRequest;
    }

    @Override
    @Nullable
    protected SearchResponse nextSearchResult() throws IOException {
        final SearchSourceBuilder initialQuery = initialSearchRequest.source();
        final SearchHit[] hits = lastSearchResponse.getHits().getHits();
        if (hits == null || hits.length == 0) {
            return null;
        }
        initialQuery.searchAfter(hits[hits.length - 1].getSortValues());
        initialSearchRequest.source(initialQuery);
        return client.executeWithIOException((c, requestOptions) -> c.search(initialSearchRequest, requestOptions),
                "Unable to retrieve next chunk from search: ");
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


