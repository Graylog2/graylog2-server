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
package org.graylog.storage.elasticsearch6;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Ping;
import org.graylog2.indexer.cluster.NodeAdapter;
import org.graylog.storage.elasticsearch6.jest.JestUtils;

import javax.inject.Inject;
import java.util.Optional;

public class NodeAdapterES6 implements NodeAdapter {
    private final JestClient jestClient;

    @Inject
    public NodeAdapterES6(JestClient jestClient) {
        this.jestClient = jestClient;
    }

    @Override
    public Optional<String> version() {
        final Ping request = new Ping.Builder().build();
        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Unable to retrieve Elasticsearch version");
        return Optional.ofNullable(jestResult.getJsonObject().path("version").path("number").asText(null));
    }
}
