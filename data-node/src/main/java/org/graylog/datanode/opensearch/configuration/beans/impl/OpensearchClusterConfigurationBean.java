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
import org.graylog.datanode.opensearch.configuration.ConfigurationBuildParams;
import org.graylog.datanode.opensearch.configuration.beans.OpensearchConfigurationBean;
import org.graylog.datanode.opensearch.configuration.beans.OpensearchConfigurationPart;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class OpensearchClusterConfigurationBean implements OpensearchConfigurationBean {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchClusterConfigurationBean.class);

    public static final Path UNICAST_HOSTS_FILE = Path.of("unicast_hosts.txt");

    private final Configuration localConfiguration;
    private final NodeService<DataNodeDto> nodeService;

    @Inject
    public OpensearchClusterConfigurationBean(Configuration localConfiguration, NodeService<DataNodeDto> nodeService) {
        this.localConfiguration = localConfiguration;
        this.nodeService = nodeService;
    }

    @Override
    public OpensearchConfigurationPart buildConfigurationPart(ConfigurationBuildParams trustedCertificates) {
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

        return OpensearchConfigurationPart.builder()
                .properties(properties.build())
                .addConfigurationDirModifier(this::writeSeedHostsFile)
                .build();
    }

    private void writeSeedHostsFile(Path configPath) {
            try {
                final Path hostsfile = configPath.resolve(UNICAST_HOSTS_FILE); // TODO: restrict file permissions!
                final Set<String> current = nodeService.allActive().values().stream().map(DataNodeDto::getClusterAddress).filter(Objects::nonNull).collect(Collectors.toSet());
                Files.write(hostsfile, current, Charset.defaultCharset(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
            } catch (IOException iox) {
                LOG.error("Could not write to file: {} - {}", UNICAST_HOSTS_FILE, iox.getMessage());
            }

    }
}
