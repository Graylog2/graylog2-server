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
package org.graylog.storage.opensearch3.cl;

import org.graylog.storage.opensearch3.indextemplates.OSSerializationUtils;
import org.opensearch.client.opensearch.OpenSearchAsyncClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.transport.OpenSearchTransport;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class CustomAsyncOpenSearchClient extends OpenSearchAsyncClient {
    private final OSSerializationUtils serializationUtils;

    public CustomAsyncOpenSearchClient(OpenSearchTransport transport) {
        super(transport);
        this.serializationUtils = new OSSerializationUtils();
    }

    /**
     * Our problem with the default search is that it provides index names in request URL. With many indices we quickly
     * exceed supported URL length. That's why we are wrapping the search request as a multisearch. There, index names
     * are part of the body, avoiding any such problem.
     */
    @Override
    public <TDocument> CompletableFuture<SearchResponse<TDocument>> search(SearchRequest request, Class<TDocument> tDocumentClass) throws IOException, OpenSearchException {
        return super.msearch(msearch -> msearch.searches(serializationUtils.toMsearch(request)), tDocumentClass)
                // TODO: error handling?
                .thenApply(result -> result.responses().getFirst().result());
    }
}
