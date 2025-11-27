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
package org.graylog.storage.opensearch3.client;

import org.graylog.storage.opensearch3.indextemplates.OSSerializationUtils;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.MsearchResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.msearch.MultiSearchResponseItem;
import org.opensearch.client.transport.OpenSearchTransport;

import java.io.IOException;

public class CustomOpenSearchClient extends OpenSearchClient {
    private final OSSerializationUtils serializationUtils;

    public CustomOpenSearchClient(OpenSearchTransport transport) {
        super(transport);
        this.serializationUtils = new OSSerializationUtils();
    }

    /**
     * Our problem with the default search is that it provides index names in request URL. With many indices we quickly
     * exceed supported URL length. That's why we are wrapping the search request as a multisearch. There, index names
     * are part of the body, avoiding any such problem.
     */
    @Override
    public <TDocument> SearchResponse<TDocument> search(SearchRequest request, Class<TDocument> tDocumentClass) throws IOException, OpenSearchException {
        final MsearchResponse<TDocument> multiSearchResponse = super.msearch(req -> req.searches(serializationUtils.toMsearch(request)), tDocumentClass);
        final MultiSearchResponseItem<TDocument> resp = multiSearchResponse.responses().getFirst();
        if (resp.isFailure()) {
            throw new OpenSearchException(resp.failure());
        }
        // if it's not failure, then it has to be a result, right?
        return resp.result();
    }
}
