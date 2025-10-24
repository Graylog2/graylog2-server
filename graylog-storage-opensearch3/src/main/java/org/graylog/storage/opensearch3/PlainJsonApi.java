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

    @Inject
    public PlainJsonApi(ObjectMapper objectMapper,
                        OfficialOpensearchClient client) {
        this.objectMapper = objectMapper;
        this.client = client;
    }

    public JsonNode perform(Request request, String errorMessage) {
        try {
            Response response = client.sync().generic().execute(request);
            String rawJson = response.getBody().map(Body::bodyAsString).orElse("");
            return objectMapper.readTree(rawJson);
        } catch (Exception e) {
            throw mapException(e, errorMessage);
        }
    }
}
