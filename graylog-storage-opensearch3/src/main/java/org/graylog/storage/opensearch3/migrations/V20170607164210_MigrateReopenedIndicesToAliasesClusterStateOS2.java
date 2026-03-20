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
package org.graylog.storage.opensearch3.migrations;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog2.migrations.V20170607164210_MigrateReopenedIndicesToAliases;
import org.opensearch.client.opensearch.generic.Request;
import org.opensearch.client.opensearch.generic.Requests;

import java.util.Collection;

public class V20170607164210_MigrateReopenedIndicesToAliasesClusterStateOS2 implements V20170607164210_MigrateReopenedIndicesToAliases.ClusterState {
    private final OfficialOpensearchClient client;

    @Inject
    public V20170607164210_MigrateReopenedIndicesToAliasesClusterStateOS2(OfficialOpensearchClient client) {
        this.client = client;
    }

    @Override
    public JsonNode getForIndices(Collection<String> indices) {
        return client.performRequest(request(indices),
                "Couldn't read cluster state for reopened indices " + indices);
    }

    private Request request(Collection<String> indices) {
        final StringBuilder apiEndpoint = new StringBuilder("/_cluster/state/metadata");
        if (!indices.isEmpty()) {
            final String joinedIndices = String.join(",", indices);
            apiEndpoint.append("/");
            apiEndpoint.append(joinedIndices);
        }
        return Requests.builder()
                .method("GET")
                .endpoint(apiEndpoint.toString())
                .build();
    }
}
