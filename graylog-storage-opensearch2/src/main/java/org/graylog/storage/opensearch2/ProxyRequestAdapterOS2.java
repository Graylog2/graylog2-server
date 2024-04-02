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
import org.apache.commons.lang3.StringUtils;
import org.graylog.shaded.opensearch2.org.apache.http.entity.ContentType;
import org.graylog.shaded.opensearch2.org.apache.http.entity.InputStreamEntity;
import org.graylog.shaded.opensearch2.org.opensearch.OpenSearchException;
import org.graylog.shaded.opensearch2.org.opensearch.client.Request;
import org.graylog.shaded.opensearch2.org.opensearch.client.Response;
import org.graylog.shaded.opensearch2.org.opensearch.client.ResponseException;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestClient;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.indexer.datanode.ProxyRequestAdapter;
import org.graylog2.rest.resources.datanodes.DatanodeResolver;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;

public class ProxyRequestAdapterOS2 implements ProxyRequestAdapter {

    private final RestClientProvider restClientProvider;
    private final DatanodeResolver datanodeResolver;

    @Inject
    public ProxyRequestAdapterOS2(RestClientProvider restClientProvider, DatanodeResolver datanodeResolver) {
        this.restClientProvider = restClientProvider;
        this.datanodeResolver = datanodeResolver;
    }

    @Override
    public ProxyResponse request(ProxyRequest request) throws IOException {
        final var req = new Request(request.method(), request.path());
        request.queryParameters().forEach((key, values) -> values.forEach(value -> req.addParameter(key, value)));
        req.setEntity(new InputStreamEntity(request.body(), ContentType.APPLICATION_JSON));
        try (
                RestHighLevelClient restClient = buildClient(request)
        ) {
            final var osResponse = restClient.getLowLevelClient().performRequest(req);
            return new ProxyResponse(osResponse.getStatusLine().getStatusCode(), osResponse.getEntity().getContent(), osResponse.getEntity().getContentType().getValue());
        } catch (OpenSearchException openSearchException) {
            final var cause = openSearchException.getCause();
            if (cause instanceof ResponseException responseException) {
                final var response = responseException.getResponse();
                final var status = response.getStatusLine().getStatusCode();
                return getProxyResponse(status, response);
            }
            throw openSearchException;
        }
    }

    @NotNull
    private static ProxyResponse getProxyResponse(int status, Response response) throws IOException {
        return new ProxyResponse(status, response.getEntity().getContent(), response.getEntity().getContentType().getValue());
    }

    private RestHighLevelClient buildClient(ProxyRequest request) {
        final URI opensearchAddress = datanodeResolver.findByHostname(request.hostname()).map(DataNodeDto::getTransportAddress)
                .filter(StringUtils::isNotBlank)
                .map(URI::create)
                .orElseThrow(() -> new IllegalStateException("No datanode found matching name " + request.hostname()));
        return restClientProvider.buildBasicRestClient(Collections.singletonList(opensearchAddress));
    }
}
