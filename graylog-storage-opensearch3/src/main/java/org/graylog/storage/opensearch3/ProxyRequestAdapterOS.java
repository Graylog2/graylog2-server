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

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.indexer.datanode.ProxyRequestAdapter;
import org.graylog2.rest.resources.datanodes.DatanodeResolver;
import org.opensearch.client.opensearch.generic.Body;
import org.opensearch.client.opensearch.generic.Request;
import org.opensearch.client.opensearch.generic.Requests;
import org.opensearch.client.opensearch.generic.Response;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class ProxyRequestAdapterOS implements ProxyRequestAdapter {

    public static final String APPLICATION_JSON = "application/json";
    public static final String CONTENT_TYPE_HEADER = "content-type";
    private final OfficialOpensearchClientProvider clientProvider;
    private final DatanodeResolver datanodeResolver;

    @Inject
    public ProxyRequestAdapterOS(OfficialOpensearchClientProvider clientProvider, DatanodeResolver datanodeResolver) {
        this.clientProvider = clientProvider;
        this.datanodeResolver = datanodeResolver;
    }

    @Override
    public ProxyResponse request(ProxyRequest request) throws IOException {
        final Request req = Requests.builder()
                .method(request.method())
                .endpoint(request.path())
                .query(queryParams(request))
                .body(Body.from(request.body(), APPLICATION_JSON))
                .build();
        try (
                // we want to close the client after this call. It's created only for this single call.
                OfficialOpensearchClient client = buildClient(request)
        ) {
            return client.sync(c -> {
                try (final org.opensearch.client.opensearch.generic.Response response = c.generic().execute(req);) {
                    final InputStream body = response.getBody().map(Body::body).orElseThrow(() -> new UnsupportedOperationException("No response body received"));
                    final String contentType = getContentType(response);
                    return new ProxyResponse(response.getStatus(), body, contentType);
                }
            }, "failed to trigger opensearch request");
        }
    }

    @Nonnull
    private static Map<String, String> queryParams(ProxyRequest request) {
        return request.queryParameters()
                .entrySet()
                .stream()
                .map(entry -> Map.entry(entry.getKey(), String.join(",", entry.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Nonnull
    private static String getContentType(Response response) {
        return response.getHeaders()
                .stream()
                .filter(h -> h.getKey().equalsIgnoreCase(CONTENT_TYPE_HEADER))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(APPLICATION_JSON);
    }

    private OfficialOpensearchClient buildClient(ProxyRequest request) {
        final URI opensearchAddress = datanodeResolver.findByHostname(request.hostname()).map(DataNodeDto::getTransportAddress)
                .filter(StringUtils::isNotBlank)
                .map(URI::create)
                .orElseThrow(() -> new IllegalStateException("No datanode found matching name " + request.hostname()));
        return clientProvider.buildClient(Collections.singletonList(opensearchAddress));
    }
}
