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
package org.graylog2.indexer.management;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import javax.annotation.Nullable;

/**
 * Plain REST calls to the search backend for Index Management (_cat/indices, _cat/shards, allocation explain, ...)
 * through Graylog's configured client: hosts, credentials, TLS.
 * <p>
 * Implemented by the storage modules. It is an optional binding (declared in {@code VersionAwareStorageModule}):
 * only the opensearch3 module provides it, so with the opensearch2 module active it is absent and the Index
 * Management endpoints answer 503.
 */
public interface IndexManagementAdapter {
    /**
     * @param method       HTTP method
     * @param endpoint     path, e.g. {@code /_cat/indices}
     * @param parameters   query parameters
     * @param jsonBody     request body, or {@code null}
     * @param errorMessage message of the exception thrown when the request fails
     * @return the response body as JSON
     */
    JsonNode request(String method, String endpoint, Map<String, String> parameters, @Nullable String jsonBody, String errorMessage);
}
