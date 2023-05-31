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

import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TestNodeService implements NodeService {

    private final Node.Type type;
    private final List<Node> nodes = new LinkedList<>();

    public TestNodeService(Node.Type type) {
        this.type = type;
    }

    @Override
    public Node.Type type() {
        return type;
    }

    @Override
    public boolean registerServer(String nodeId, boolean isLeader, URI httpPublishUri, String hostname) {
        return nodes.add(new TestNode(type, nodeId, isLeader, httpPublishUri.toString(), hostname, new DateTime()));
    }

    @Override
    public Node byNodeId(String nodeId) throws NodeNotFoundException {
        return nodes.stream().filter(n -> n.getNodeId().equals(nodeId)).findFirst().orElseThrow(() -> new NodeNotFoundException("Not found"));
    }

    @Override
    public Node byNodeId(NodeId nodeId) throws NodeNotFoundException {
        return byNodeId(nodeId.getNodeId());
    }

    @Override
    public Map<String, Node> allActive(Node.Type type) {
        return nodes.stream().filter(n -> n.getType().equals(type)).collect(Collectors.toMap(Node::getNodeId, Function.identity()));
    }

    @Deprecated
    @Override
    public Map<String, Node> allActive() {
        return allActive(type);
    }

    @Override
    public boolean isAnyLeaderPresent() {
        return nodes.stream().anyMatch(Node::isLeader);
    }

    @Override
    public boolean isOnlyLeader(NodeId nodeId) {
        return nodes.stream().filter(n -> !Objects.equals(n.getNodeId(), nodeId.getNodeId())).noneMatch(Node::isLeader);
    }

    @Override
    public void dropOutdated() {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    @Override
    public void markAsAlive(NodeId node, boolean isLeader, URI restTransportAddress) {
        throw new UnsupportedOperationException("Unsupported operation");
    }
}
