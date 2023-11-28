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

import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.plugin.system.NodeId;

import java.util.Collection;
import java.util.Map;

public interface NodeService<T extends NodeDto> {
    boolean registerServer(NodeDto dto);

    T byNodeId(String nodeId) throws NodeNotFoundException;

    T byNodeId(NodeId nodeId) throws NodeNotFoundException;

    Map<String, T> byNodeIds(Collection<String> nodeIds);

    Map<String, T> allActive();

    void dropOutdated();

    void markAsAlive(NodeDto dto) throws NodeNotFoundException;

    boolean isOnlyLeader(NodeId nodeIde);

    boolean isAnyLeaderPresent();

    /**
     * Sets the last seen date of the node, additionally updating the settings.
     * If the node hasn't been registered yet, it will be registered.
     *
     * @param dto Dto of the node to be marked as alive
     */
    void ping(NodeDto dto);
}
