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
package org.graylog2.cluster;

import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.ServerNodeClusterService;
import org.graylog2.cluster.nodes.ServerNodeEntity;
import org.graylog2.plugin.system.NodeId;

import javax.inject.Inject;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @deprecated This is left for compatibility reasons,
 * delegating calls to org.graylog2.cluster.nodes.NodeService&lt;ServerNodeEntity&gt;
 */
@Deprecated(since = "6.0")
public class NodeServiceImpl implements NodeService {

    private final ServerNodeClusterService delegate;

    @Inject
    public NodeServiceImpl(ServerNodeClusterService delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean registerServer(String nodeId, boolean isLeader, URI httpPublishUri, String clusterUri, String hostname) {
        return delegate.registerServer(nodeId, isLeader, httpPublishUri, clusterUri, hostname);
    }

    @Override
    public Node byNodeId(String nodeId) throws NodeNotFoundException {
        return delegate.byNodeId(nodeId);
    }

    @Override
    public Node byNodeId(NodeId nodeId) throws NodeNotFoundException {
        return delegate.byNodeId(nodeId);
    }

    @Override
    public Map<String, Node> byNodeIds(Collection<String> nodeIds) {
        return transformMap(delegate.byNodeIds(nodeIds));
    }

    @Override
    public Map<String, Node> allActive() {
        return transformMap(delegate.allActive());
    }

    @Override
    public void dropOutdated() {
        delegate.dropOutdated();
    }

    @Override
    public void markAsAlive(NodeId node, boolean isLeader, URI restTransportAddress, String clusterAddress, DataNodeStatus dataNodeStatus) throws NodeNotFoundException {
        delegate.markAsAlive(node, isLeader, restTransportAddress, clusterAddress, dataNodeStatus);
    }

    @Override
    public boolean isOnlyLeader(NodeId nodeIde) {
        return delegate.isOnlyLeader(nodeIde);
    }

    @Override
    public boolean isAnyLeaderPresent() {
        return delegate.isAnyLeaderPresent();
    }

    private Map<String, Node> transformMap(Map<String, ServerNodeEntity> nodes) {
        return nodes.entrySet().stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                n -> (Node) n
                        ));
    }
}
