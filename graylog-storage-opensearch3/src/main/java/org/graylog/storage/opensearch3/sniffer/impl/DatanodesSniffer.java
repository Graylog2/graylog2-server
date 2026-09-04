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
package org.graylog.storage.opensearch3.sniffer.impl;

import jakarta.inject.Inject;
import org.graylog.storage.opensearch3.sniffer.DiscoveredNode;
import org.graylog.storage.opensearch3.sniffer.NodesSniffer;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.NodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.configuration.RunsWithDataNode;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides hostnames of datanodes available in the datanode {@link NodeService} collection.
 * By using this sniffer, opensearch clients can recover and start using opensearch inside
 * datanode even if we heavily reconfigure the cluster topology.
 */
public class DatanodesSniffer implements NodesSniffer {

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
    public List<DiscoveredNode> sniff() {
        return nodeService.allActive().values().stream()
                .filter(n -> n.getDataNodeStatus() == DataNodeStatus.AVAILABLE)
                .map(NodeDto::getTransportAddress)
                .map(DatanodesSniffer::toDiscoveredNode)
                .collect(Collectors.toList());
    }

    private static DiscoveredNode toDiscoveredNode(String address) {
        final URI uri = URI.create(address);
        return new DiscoveredNode(
                uri.getScheme(),
                uri.getHost(),
                uri.getPort(),
                Collections.emptyMap()
        );
    }
}
