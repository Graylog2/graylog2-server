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
import org.graylog.datanode.opensearch.configuration.OpensearchConfigurationParams;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationBean;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;
import org.graylog.datanode.process.configuration.files.TextConfigFile;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OpensearchClusterConfigurationBean implements DatanodeConfigurationBean<OpensearchConfigurationParams> {

    public static final Path UNICAST_HOSTS_FILE = Path.of("unicast_hosts.txt");

    private final Configuration localConfiguration;
    private final NodeService<DataNodeDto> nodeService;

    @Inject
    public OpensearchClusterConfigurationBean(Configuration localConfiguration, NodeService<DataNodeDto> nodeService) {
        this.localConfiguration = localConfiguration;
        this.nodeService = nodeService;
    }

    @Override
    public DatanodeConfigurationPart buildConfigurationPart(OpensearchConfigurationParams trustedCertificates) {
        ImmutableMap.Builder<String, String> properties = ImmutableMap.builder();

        properties.put("network.bind_host", localConfiguration.getBindAddress());
        properties.put("network.publish_host", localConfiguration.getHostname());

        if (localConfiguration.getClustername() != null && !localConfiguration.getClustername().isBlank()) {
            properties.put("cluster.name", localConfiguration.getClustername());
        }

        if (localConfiguration.getBindAddress() != null && !localConfiguration.getBindAddress().isBlank()) {
            properties.put("network.host", localConfiguration.getBindAddress());
        }
        properties.put("http.port", String.valueOf(localConfiguration.getOpensearchHttpPort()));
        properties.put("transport.port", String.valueOf(localConfiguration.getOpensearchTransportPort()));

        properties.put("node.name", localConfiguration.getDatanodeNodeName());

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

        return DatanodeConfigurationPart.builder()
                .properties(properties.build())
                .withConfigFile(seedHostFile())
                .build();
    }

    private TextConfigFile seedHostFile() {
        final String data = nodeService.allActive().values().stream()
                .map(DataNodeDto::getClusterAddress)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
        return new TextConfigFile(UNICAST_HOSTS_FILE, data);
    }
}
