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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import org.graylog.storage.opensearch3.testing.OpenSearchInstance;
import org.graylog.testing.elasticsearch.SearchInstance;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.cluster.ClusterIT;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.opensearch.client.opensearch.cat.nodes.NodesRecord;

public class ClusterOSIT extends ClusterIT {

    ClusterAdapterOS clusterAdapterOS;

    @SearchInstance
    public final OpenSearchInstance openSearchInstance = OpenSearchInstance.create();

    @Override
    protected SearchServerInstance searchServer() {
        return this.openSearchInstance;
    }

    @Override
    protected ClusterAdapter clusterAdapter(Duration timeout) {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        clusterAdapterOS = new ClusterAdapterOS(openSearchInstance.getOfficialOpensearchClient(),
                timeout,
                new PlainJsonApi(objectMapper, openSearchInstance.openSearchClient(), openSearchInstance.getOfficialOpensearchClient()));
        return clusterAdapterOS;
    }

    @Override
    protected String currentNodeId() {
        return currentNode().id();
    }

    private NodesRecord currentNode() {
        return clusterAdapterOS.nodes().getFirst();
    }

    @Override
    protected String currentNodeName() {
        return currentNode().name();
    }

    @Override
    protected String currentHostnameOrIp() {
        final NodesRecord currentNode = currentNode();
        return clusterAdapterOS.nodeIdToHostName(currentNode.id()).orElse(currentNode.ip());
    }

}
