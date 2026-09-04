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
package org.graylog.storage.opensearch2.sniffer.impl;

import jakarta.inject.Inject;
import org.graylog.shaded.opensearch2.org.apache.http.HttpHost;
import org.graylog.shaded.opensearch2.org.opensearch.client.Node;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestClient;
import org.graylog.shaded.opensearch2.org.opensearch.client.sniff.NodesSniffer;
import org.graylog.storage.opensearch2.sniffer.SnifferBuilder;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.NodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.configuration.RunsWithDataNode;

import java.util.stream.Collectors;

/**
 * This is an implementation of a {@link NodesSniffer} which provides hostnames of datanodes available in the
 * datanode {@link NodeService} collection. By using this sniffer, opensearch clients can recover and start using
 * opensearch inside datanode even if we heavily reconfigure the cluster topology - by stopping and starting nodes,
 * changing their hostnames(and certificates), by adding and removing nodes. Opensearch clients will always get
 * all available nodes through this sniffer. This also helps to spread the load in the cluster, as all of those
 * nodes can be used by the client, not just those initially configured.
 */
public class DatanodesSniffer implements SnifferBuilder {

    private final NodeService<DataNodeDto> nodeService;
    private final boolean runsWithDataNode;

    @Inject
    public DatanodesSniffer(NodeService<DataNodeDto> nodeService, @RunsWithDataNode boolean runsWithDataNode) {
        this.nodeService = nodeService;
        this.runsWithDataNode = runsWithDataNode;
    }

    @Override
    public boolean enabled() {
        return runsWithDataNode;
    }

    @Override
    public NodesSniffer create(RestClient restClient) {
        return () -> nodeService.allActive().values().stream()
                .filter(n -> n.getDataNodeStatus() == DataNodeStatus.AVAILABLE)
                .map(NodeDto::getTransportAddress)
                .map(host -> new Node(HttpHost.create(host)))
                .collect(Collectors.toList());
    }
}
