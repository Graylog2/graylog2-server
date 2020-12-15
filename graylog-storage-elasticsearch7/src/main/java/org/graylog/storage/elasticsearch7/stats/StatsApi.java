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
package org.graylog.storage.elasticsearch7.stats;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Request;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Response;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;

public class StatsApi {
    private final ObjectMapper objectMapper;
    private final ElasticsearchClient client;

    @Inject
    public StatsApi(ObjectMapper objectMapper,
                    ElasticsearchClient client) {
        this.objectMapper = objectMapper;
        this.client = client;
    }

    public JsonNode indexStats(String index) {
        return stats(index);
    }

    public JsonNode indexStatsWithShardLevel(String index) {
        return indexStatsWithShardLevel(Collections.singleton(index)).path(index);
    }

    public JsonNode indexStatsWithShardLevel(Collection<String> indices) {
        final JsonNode stats = stats(indices, request -> {
            request.addParameter("level", "shards");
            request.addParameter("ignore_unavailable", "true");
        });

        return stats.path("indices");
    }

    public JsonNode indexStatsWithDocsAndStore(Collection<String> indices) {
        final JsonNode stats = stats(indices, ImmutableSet.of("store", "docs"));

        return stats.path("indices");
    }

    public Optional<Long> storeSizes(String index) {
        final JsonNode stats = stats(Collections.singleton(index), Collections.singleton("store"));
        final JsonNode sizeInBytes = stats.path("indices")
                .path(index)
                .path("primaries")
                .path("store")
                .path("size_in_bytes");
        return Optional.of(sizeInBytes).filter(JsonNode::isNumber).map(JsonNode::asLong);
    }

    private JsonNode stats(String index) {
        return stats(Collections.singleton(index), Collections.emptySet(), (request) -> {});
    }

    private JsonNode stats(Collection<String> indices,
                           Collection<String> metrics) {
        return stats(indices, metrics, (request) -> {});
    }

    private JsonNode stats(Collection<String> indices,
                           Consumer<Request> fn) {
        return stats(indices, Collections.emptySet(), fn);
    }

    private JsonNode stats(Collection<String> indices,
                           Collection<String> metrics,
                           Consumer<Request> prepareRequest) {
        final StringBuilder endpoint = new StringBuilder();
        if (!indices.isEmpty()) {
            final String joinedIndices = String.join(",", indices);
            endpoint.append("/");
            endpoint.append(joinedIndices);
        }
        endpoint.append("/_stats");
        if (!metrics.isEmpty()) {
            final String joinedMetrics = String.join(",", metrics);
            endpoint.append("/");
            endpoint.append(joinedMetrics);
        }

        final Request request = new Request("GET", endpoint.toString());
        prepareRequest.accept(request);
        return client.execute((c, requestOptions) -> {
            request.setOptions(requestOptions);
            final Response response = c.getLowLevelClient().performRequest(request);
            return objectMapper.readTree(response.getEntity().getContent());
        }, "Unable to retrieve index stats for " + String.join(",", indices));
    }
}
