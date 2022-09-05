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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opensearch.client.Request;
import org.graylog2.indexer.cluster.NodeAdapter;
import org.graylog2.storage.SearchVersion;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Optional;

public class NodeAdapterOS2 implements NodeAdapter {
    private final PlainJsonApi jsonApi;

    @Inject
    public NodeAdapterOS2(OpenSearchClient client, ObjectMapper objectMapper) {
        this.jsonApi = new PlainJsonApi(objectMapper, client);
    }

    NodeAdapterOS2(final PlainJsonApi jsonApi) {
        this.jsonApi = jsonApi;
    }

    @Override
    public Optional<SearchVersion> version() {

        final Request request = new Request("GET", "/?filter_path=version.number,version.distribution");
        final Optional<JsonNode> resp = Optional.of(jsonApi.perform(request, "Unable to retrieve cluster information"));

        final Optional<String> version = resp.map(r -> r.path("version")).map(r -> r.path("number")).map(JsonNode::textValue);

        final SearchVersion.Distribution distribution = resp.map(r -> r.path("version")).map(r -> r.path("distribution")).map(JsonNode::textValue)
                .map(d -> d.toUpperCase(Locale.ROOT))
                .map(SearchVersion.Distribution::valueOf)
                .orElse(SearchVersion.Distribution.ELASTICSEARCH);

        return version
                .map(this::parseVersion)
                .map(v -> SearchVersion.create(distribution, v));
    }


}
