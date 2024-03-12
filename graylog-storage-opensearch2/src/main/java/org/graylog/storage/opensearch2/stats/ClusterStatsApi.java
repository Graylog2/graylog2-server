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
package org.graylog.storage.opensearch2.stats;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.graylog.storage.opensearch2.PlainJsonApi;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetStats;
import org.opensearch.client.Request;

public class ClusterStatsApi {
    private final ObjectMapper objectMapper;
    private final PlainJsonApi jsonApi;


    @Inject
    public ClusterStatsApi(ObjectMapper objectMapper,
                           PlainJsonApi jsonApi) {
        this.objectMapper = objectMapper;
        this.jsonApi = jsonApi;
    }

    public IndexSetStats clusterStats() {
        final JsonNode indicesCountJson = clusterStats("indices.count");
        final JsonNode docsCountJson = clusterStats("indices.docs.count");
        final JsonNode sizeBytesJson = clusterStats("indices.store.size_in_bytes");

        final long indicesCount = indicesCountJson.path("indices").path("count").asLong();
        final long docsCount = docsCountJson.path("indices").path("docs").path("count").asLong();
        final long sizeBytes = sizeBytesJson.path("indices").path("store").path("size_in_bytes").asLong();

        return IndexSetStats.create(indicesCount, docsCount, sizeBytes);
    }

    private JsonNode clusterStats(String filterPath) {
        final String endpoint = "/_cluster/stats?filter_path=" + filterPath;
        final Request request = new Request("GET", endpoint);
        return jsonApi.perform(request, "Couldn't read Opensearch cluster stats");
    }
}
