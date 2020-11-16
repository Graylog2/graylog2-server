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
package org.graylog.storage.elasticsearch7.cat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Request;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Response;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class CatApi {
    private final ObjectMapper objectMapper;
    private final ElasticsearchClient client;

    @Inject
    public CatApi(ObjectMapper objectMapper,
                  ElasticsearchClient client) {
        this.objectMapper = objectMapper;
        this.client = client;
    }

    public List<NodeResponse> nodes() {
        final Request request = request("GET", "nodes");
        request.addParameter("h", "id,name,host,ip,fileDescriptorMax,diskUsed,diskTotal,diskUsedPercent");
        request.addParameter("full_id", "true");
        return perform(request, new TypeReference<List<NodeResponse>>() {}, "Unable to retrieve nodes list");
    }

    public Set<String> indices(String index, Collection<String> status, String errorMessage) {
        return indices(Collections.singleton(index), status, errorMessage);
    }

    public Set<String> indices(Collection<String> indices, Collection<String> status, String errorMessage) {
        final String joinedIndices = String.join(",", indices);
        final JsonNode jsonResponse = requestIndices(joinedIndices, errorMessage);

        //noinspection UnstableApiUsage
        return Streams.stream(jsonResponse.elements())
                .filter(index -> status.isEmpty() || status.contains(index.path("status").asText()))
                .map(index -> index.path("index").asText())
                .collect(Collectors.toSet());
    }

    public Optional<String> indexState(String indexName, String errorMessage) {
        final JsonNode jsonResponse = requestIndices(indexName, errorMessage);

        //noinspection UnstableApiUsage
        return Streams.stream(jsonResponse.elements())
                .filter(index -> index.path("index").asText().equals(indexName))
                .map(index -> index.path("status").asText())
                .findFirst();
    }

    private JsonNode requestIndices(String indexName, String errorMessage) {
        final Request request = request("GET", "indices/" + indexName);
        request.addParameter("h", "index,status");
        request.addParameter("expand_wildcards", "all");
        request.addParameter("s", "index,status");

        return perform(request, new TypeReference<JsonNode>() {}, errorMessage);
    }

    private <R> R perform(Request request, TypeReference<R> responseClass, String errorMessage) {
        return client.execute((c, requestOptions) -> {
            request.setOptions(requestOptions);

            final Response response = c.getLowLevelClient().performRequest(request);
            return returnType(response, responseClass);
        }, errorMessage);
    }

    private <R> R returnType(Response response, TypeReference<R> responseClass) throws IOException {
        return objectMapper.readValue(response.getEntity().getContent(), responseClass);
    }

    private Request request(@SuppressWarnings("SameParameterValue") String method, String endpoint) {
        final Request request = new Request(method, "/_cat/" + endpoint);
        request.addParameter("format", "json");

        return request;
    }
}
