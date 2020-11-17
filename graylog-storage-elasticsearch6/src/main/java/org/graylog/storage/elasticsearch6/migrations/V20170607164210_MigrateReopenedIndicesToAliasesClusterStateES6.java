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
package org.graylog.storage.elasticsearch6.migrations;

import com.fasterxml.jackson.databind.JsonNode;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.State;
import org.graylog.storage.elasticsearch6.jest.JestUtils;
import org.graylog2.migrations.V20170607164210_MigrateReopenedIndicesToAliases;

import javax.inject.Inject;
import java.util.Collection;

public class V20170607164210_MigrateReopenedIndicesToAliasesClusterStateES6 implements V20170607164210_MigrateReopenedIndicesToAliases.ClusterState {
    private final JestClient jestClient;

    @Inject
    public V20170607164210_MigrateReopenedIndicesToAliasesClusterStateES6(JestClient jestClient) {
        this.jestClient = jestClient;
    }

    @Override
    public JsonNode getForIndices(Collection<String> indices) {
        final String indexList = String.join(",", indices);

        final State request = new State.Builder().withMetadata().indices(indexList).build();

        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't read cluster state for reopened indices " + indices);

        return jestResult.getJsonObject();
    }
}
