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
import jakarta.inject.Inject;
import org.graylog2.indexer.management.IndexManagementAdapter;
import org.opensearch.client.opensearch.generic.Requests;

import java.util.Map;
import javax.annotation.Nullable;

public class IndexManagementAdapterOS implements IndexManagementAdapter {
    private final OfficialOpensearchClient client;

    @Inject
    public IndexManagementAdapterOS(OfficialOpensearchClient client) {
        this.client = client;
    }

    @Override
    public JsonNode request(String method, String endpoint, Map<String, String> parameters, @Nullable String jsonBody, String errorMessage) {
        final Requests.JsonBodyBuilder request = Requests.builder()
                .method(method)
                .endpoint(endpoint)
                .query(parameters);
        if (jsonBody != null) {
            request.json(jsonBody);
        }
        return client.performRequest(request.build(), errorMessage);
    }
}
