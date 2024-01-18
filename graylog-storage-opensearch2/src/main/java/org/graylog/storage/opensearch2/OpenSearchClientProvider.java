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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientOptions;
import org.opensearch.client.transport.rest_client.RestClientTransport;

public class OpenSearchClientProvider implements Provider<OpenSearchClient> {
    private final RestClientTransport transport;

    @Inject
    public OpenSearchClientProvider(RestClient restClient, ObjectMapper objectMapper) {
        this.transport = new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper));
    }

    @Override
    public OpenSearchClient get() {
        return new OpenSearchClient(transport);
    }

    public OpenSearchClient getWithRequestOptions(RequestOptions requestOptions) {
        return new OpenSearchClient(transport.withRequestOptions(new RestClientOptions(requestOptions)));
    }
}
