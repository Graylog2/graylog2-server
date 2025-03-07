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
package org.graylog.plugins.datanode.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record ClusterState(String status, String clusterName, int numberOfNodes, int activeShards, int relocatingShards,
                           int initializingShards, int unassignedShards, int activePrimaryShards,
                           int delayedUnassignedShards, ShardReplication shardReplication, ManagerNode managerNode,
                           List<Node> opensearchNodes) {

    @JsonIgnore
    public Optional<Node> findByHostname(String hostname) {
        return opensearchNodes.stream().
                filter(n -> n.host().equals(hostname))
                .findFirst();
    }
}
