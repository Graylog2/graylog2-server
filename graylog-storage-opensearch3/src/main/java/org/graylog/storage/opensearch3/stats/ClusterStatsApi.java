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
package org.graylog.storage.opensearch3.stats;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetStats;
import org.opensearch.client.opensearch.generic.Request;
import org.opensearch.client.opensearch.generic.Requests;

import java.util.Map;

public class ClusterStatsApi {
    private final OfficialOpensearchClient officialOpensearchClient;

    @Inject
    public ClusterStatsApi(OfficialOpensearchClient officialOpensearchClient) {
        this.officialOpensearchClient = officialOpensearchClient;
    }

    public IndexSetStats clusterStats() {
        // the new filter-by-request-path of ClusterStatsRequest is only available in OS >= 2.18.
        // so we have to resort to using the undocumented, but working (as of 3.5.0) filter_path request parameter
        // with a generic api request.
        final Request request = Requests.builder()
                .method("GET")
                .endpoint("/_cluster/stats")
                .query(Map.of("filter_path", "indices.count,indices.docs.count,indices.store.size_in_bytes"))
                .build();
        JsonNode stats = officialOpensearchClient.performRequest(request, "Couldn't read OpensSearch cluster stats");

        final long indicesCount = stats.path("indices").path("count").asLong();
        final long docsCount = stats.path("indices").path("docs").path("count").asLong();
        final long sizeBytes = stats.path("indices").path("store").path("size_in_bytes").asLong();

        return IndexSetStats.create(indicesCount, docsCount, sizeBytes);
    }
}
