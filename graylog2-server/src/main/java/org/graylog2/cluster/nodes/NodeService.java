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
import java.util.Optional;

public interface NodeService<T extends NodeDto> {
    boolean registerServer(T dto);

    /**
     * Returns the node identified by {@code nodeId} if it is currently online.
     * Offline (historical) rows are filtered out; use {@link #byNodeIdAnyState(String)} to include them.
     */
    T byNodeId(String nodeId) throws NodeNotFoundException;

    T byNodeId(NodeId nodeId) throws NodeNotFoundException;

    /**
     * Returns the node identified by {@code nodeId} regardless of online state.
     * Used for inventory/history queries (e.g. upgrade tracking).
     */
    Optional<T> byNodeIdAnyState(String nodeId);

    Map<String, T> byNodeIds(Collection<String> nodeIds);

    Map<String, T> allActive();

    /**
     * Returns every known node, including offline ones. Useful for inventory queries
     * (e.g. "which nodes haven't been upgraded yet").
     */
    Map<String, T> allKnown();

    void dropOutdated();

    boolean isOnlyLeader(NodeId nodeIde);

    boolean isAnyLeaderPresent();

    /**
     * Sets the last seen date of the node, additionally updating the settings.
     * If the node hasn't been registered yet, it will be registered.
     *
     * @param dto Dto of the node to be marked as alive
     */
    void ping(T dto);

    void update(T dto);
}
