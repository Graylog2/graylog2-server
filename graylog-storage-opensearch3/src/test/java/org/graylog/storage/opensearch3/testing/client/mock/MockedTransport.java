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
package org.graylog.storage.opensearch3.testing.client.mock;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.json.jackson.JacksonJsonpParser;
import org.opensearch.client.transport.Endpoint;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.TransportOptions;
import org.opensearch.client.transport.endpoints.SimpleEndpoint;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

class MockedTransport implements OpenSearchTransport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final JacksonJsonpMapper jacksonJsonpMapper;
    private final Set<MockedResponse> responses;

    MockedTransport(Set<MockedResponse> responses) {
        jacksonJsonpMapper = new JacksonJsonpMapper(OBJECT_MAPPER);
        this.responses = responses;
    }

    @Override
    public <RequestT, ResponseT, ErrorT> ResponseT performRequest(RequestT request, Endpoint<RequestT, ResponseT, ErrorT> endpoint, @Nullable TransportOptions options) throws IOException {
        final String url = endpoint.requestUrl(request);
        final String method = endpoint.method(request);

        final String rawBody = responses.stream()
                .filter(r -> r.method().equals(method))
                .filter(r -> r.url().equals(url))
                .map(MockedResponse::body)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No response body provided for " + method + " and " + url));

        final JsonParser parser = OBJECT_MAPPER.createParser(rawBody);
        final JacksonJsonpParser jsonpParser = new JacksonJsonpParser(parser);
        final Object response = ((SimpleEndpoint) endpoint).responseDeserializer().deserialize(jsonpParser, jsonpMapper());
        return (ResponseT) response;
    }

    @Override
    public <RequestT, ResponseT, ErrorT> CompletableFuture<ResponseT> performRequestAsync(RequestT request, Endpoint<RequestT, ResponseT, ErrorT> endpoint, @Nullable TransportOptions options) {
        try {
            return CompletableFuture.completedFuture(performRequest(request, endpoint, options));
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public JsonpMapper jsonpMapper() {
        return jacksonJsonpMapper;
    }

    @Override
    public TransportOptions options() {
        return TransportOptions.builder().build();
    }

    @Override
    public void close() {

    }
}
