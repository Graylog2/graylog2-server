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
package org.graylog2.rest.resources.datanodes;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeDto;
import org.graylog2.cluster.nodes.NodeService;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Singleton
public class DatanodeResolver {

    public static final String LEADER_KEYWORD = "leader";
    public static final String ANY_NODE_KEYWORD = "any";
    public static final String ALL_NODES_KEYWORD = "all";
    private final NodeService<DataNodeDto> nodeService;

    @Inject
    public DatanodeResolver(NodeService<DataNodeDto> nodeService) {
        this.nodeService = nodeService;
    }

    public Optional<DataNodeDto> findByHostname(String hostname) {
        final Collection<DataNodeDto> datanodes = nodeService.allActive().values();
        return datanodes.stream()
                .filter(node -> Objects.equals(node.getHostname(), hostname))
                .findFirst()
                .or(() -> findByKeyword(datanodes, hostname));
    }

    private Optional<DataNodeDto> findByKeyword(Collection<DataNodeDto> datanodes, String hostname) {
        if (hostname != null && hostname.trim().toLowerCase(Locale.ROOT).equals(LEADER_KEYWORD)) {
            return datanodes.stream().filter(NodeDto::isLeader).findFirst();
        } else if (hostname != null && hostname.trim().toLowerCase(Locale.ROOT).equals(ANY_NODE_KEYWORD)) {
            return datanodes.stream().findFirst();
        } else {
            return Optional.empty();
        }
    }

}
