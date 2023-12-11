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
package org.graylog2.cluster.nodes;

import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.plugin.system.NodeId;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TestDataNodeNodeClusterService implements NodeService<DataNodeDto> {

    private final List<DataNodeDto> nodes = new LinkedList<>();


    @Override
    public boolean registerServer(NodeDto dto) {
        return nodes.add((DataNodeDto) dto);
    }

    @Override
    public DataNodeDto byNodeId(String nodeId) throws NodeNotFoundException {
        return nodes.stream().filter(n -> n.getNodeId().equals(nodeId)).findFirst().orElseThrow(() -> new NodeNotFoundException("Not found"));
    }

    @Override
    public DataNodeDto byNodeId(NodeId nodeId) throws NodeNotFoundException {
        return byNodeId(nodeId.getNodeId());
    }

    @Override
    public Map<String, DataNodeDto> byNodeIds(Collection<String> nodeIds) {
        return nodes.stream().filter(n -> nodeIds.contains(n.getNodeId())).collect(Collectors.toMap(Node::getNodeId, Function.identity()));
    }

    @Override
    public Map<String, DataNodeDto> allActive() {
        return nodes.stream().collect(Collectors.toMap(Node::getNodeId, Function.identity()));
    }

    @Override
    public boolean isOnlyLeader(NodeId nodeId) {
        return nodes.stream().filter(n -> !Objects.equals(n.getNodeId(), nodeId.getNodeId())).noneMatch(Node::isLeader);
    }

    @Override
    public boolean isAnyLeaderPresent() {
        return nodes.stream().anyMatch(Node::isLeader);
    }

    @Override
    public void ping(NodeDto dto) {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    @Override
    public void dropOutdated() {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    @Override
    public void markAsAlive(NodeDto dto) throws NodeNotFoundException {
        throw new UnsupportedOperationException("Unsupported operation");
    }
}
