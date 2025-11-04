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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.opensearch.client.opensearch.generic.Body;
import org.opensearch.client.opensearch.generic.Request;
import org.opensearch.client.opensearch.generic.Response;

import static org.graylog.storage.opensearch3.OfficialOpensearchClient.mapException;

public class PlainJsonApi {
    private final ObjectMapper objectMapper;
    private final OfficialOpensearchClient client;
    private final OpenSearchClient deprecatedClient;

    @Inject
    public PlainJsonApi(ObjectMapper objectMapper,
                        OpenSearchClient deprecatedClient,
                        OfficialOpensearchClient client) {
        this.objectMapper = objectMapper;
        this.client = client;
        this.deprecatedClient = deprecatedClient;
    }

    @Deprecated
    public PlainJsonApi(ObjectMapper objectMapper, OpenSearchClient client) {
        this(objectMapper, client, null);
    }

    public JsonNode performRequest(Request request, String errorMessage) {
        try {
            Response response = client.sync().generic().execute(request);
            String rawJson = response.getBody().map(Body::bodyAsString).orElse("");
            return objectMapper.readTree(rawJson);
        } catch (Exception e) {
            throw mapException(e, errorMessage);
        }
    }

    @Deprecated
    public JsonNode perform(org.graylog.shaded.opensearch2.org.opensearch.client.Request request, String errorMessage) {
        return deprecatedClient.execute((c, requestOptions) -> {
            request.setOptions(requestOptions);
            org.graylog.shaded.opensearch2.org.opensearch.client.Response response = c.getLowLevelClient().performRequest(request);
            return objectMapper.readTree(response.getEntity().getContent());
        }, errorMessage);
    }
}
