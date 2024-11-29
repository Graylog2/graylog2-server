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
package org.graylog.datanode.opensearch.configuration.beans.impl;

import com.google.common.collect.ImmutableMap;
import jakarta.inject.Inject;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.opensearch.configuration.beans.OpensearchConfigurationBean;
import org.graylog.datanode.opensearch.configuration.beans.OpensearchConfigurationPart;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Collectors;

public class OpensearchClusterConfigurationBean implements OpensearchConfigurationBean {

    private final Configuration localConfiguration;
    private final NodeService<DataNodeDto> nodeService;

    @Inject
    public OpensearchClusterConfigurationBean(Configuration localConfiguration, NodeService<DataNodeDto> nodeService) {
        this.localConfiguration = localConfiguration;
        this.nodeService = nodeService;
    }

    @Override
    public OpensearchConfigurationPart buildConfigurationPart(List<X509Certificate> trustedCertificates) {
        ImmutableMap.Builder<String, String> properties = ImmutableMap.builder();

        if (localConfiguration.getInitialClusterManagerNodes() != null && !localConfiguration.getInitialClusterManagerNodes().isBlank()) {
            properties.put("cluster.initial_cluster_manager_nodes", localConfiguration.getInitialClusterManagerNodes());
        } else {
            final var nodeList = String.join(",", nodeService.allActive().values().stream().map(Node::getHostname).collect(Collectors.toSet()));
            properties.put("cluster.initial_cluster_manager_nodes", nodeList);
        }

        final List<String> discoverySeedHosts = localConfiguration.getOpensearchDiscoverySeedHosts();
        if (discoverySeedHosts != null && !discoverySeedHosts.isEmpty()) {
            properties.put("discovery.seed_hosts", String.join(",", discoverySeedHosts));
        }

        properties.put("discovery.seed_providers", "file");

        // TODO: why do we have this configured?
        properties.put("node.max_local_storage_nodes", "3");

        return OpensearchConfigurationPart.builder()
                .properties(properties.build())
                .build();
    }
}
