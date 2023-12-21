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

import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.graylog2.indexer.datanode.ProxyRequestAdapter;
import org.opensearch.client.Request;
import org.opensearch.client.ResponseException;
import org.opensearch.client.RestClient;
import org.opensearch.client.opensearch._types.OpenSearchException;

import jakarta.inject.Inject;

import java.io.IOException;

public class ProxyRequestAdapterOS2 implements ProxyRequestAdapter {
    private final OpenSearchClient client;
    private final RestClient restClient;

    @Inject
    public ProxyRequestAdapterOS2(OpenSearchClient openSearchClient, RestClient restClient) {
        this.client = openSearchClient;
        this.restClient = restClient;
    }

    @Override
    public ProxyResponse request(ProxyRequest request) throws IOException {
        final var osRequest = new Request(request.method(), request.path());
        osRequest.setEntity(new InputStreamEntity(request.body(), ContentType.APPLICATION_JSON));

        try {
            final var osResponse = client.execute(() -> restClient.performRequest(osRequest), "Unable to proxy request to data node");

            return new ProxyResponse(osResponse.getStatusLine().getStatusCode(), osResponse.getEntity().getContent());
        } catch (OpenSearchException openSearchException) {
            final var cause = openSearchException.getCause();
            if (cause instanceof ResponseException responseException) {
                final var response = responseException.getResponse();
                final var status = response.getStatusLine().getStatusCode();
                return new ProxyResponse(status, response.getEntity().getContent());
            }
            throw openSearchException;
        }
    }
}
