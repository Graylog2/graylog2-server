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
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.opensearch.configuration.OpensearchConfigurationParams;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationBean;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;
import org.graylog.datanode.process.configuration.files.TextConfigFile;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class OpensearchClusterConfigurationBean implements DatanodeConfigurationBean<OpensearchConfigurationParams> {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchClusterConfigurationBean.class);

    private static final Path UNICAST_HOSTS_FILE = Path.of("unicast_hosts.txt");

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

        final String bindHost = localConfiguration.getBindAddress();
        properties.put("network.bind_host", bindHost);

        final String publishHost = localConfiguration.getHostname();
        properties.put("network.publish_host", publishHost);

        if (localConfiguration.getClustername() != null && !localConfiguration.getClustername().isBlank()) {
            properties.put("cluster.name", localConfiguration.getClustername());
        }

        if (localConfiguration.getBindAddress() != null && !localConfiguration.getBindAddress().isBlank()) {
            properties.put("network.host", localConfiguration.getBindAddress());
        }
        properties.put("http.port", String.valueOf(localConfiguration.getOpensearchHttpPort()));
        properties.put("transport.port", String.valueOf(localConfiguration.getOpensearchTransportPort()));

        final String nodeName = localConfiguration.getDatanodeNodeName();
        properties.put("node.name", nodeName);

        LOG.info("Opensearch networking: bind host: {}, publish host: {}, node name: {}", bindHost, publishHost, nodeName);

        final String initialClusterManagerNodes = getInitialClusterManagerNodes();
        properties.put("cluster.initial_cluster_manager_nodes", initialClusterManagerNodes);
        LOG.info("Opensearch initial cluster manager nodes: {}", initialClusterManagerNodes);


        final List<String> discoverySeedHosts = localConfiguration.getOpensearchDiscoverySeedHosts();
        if (discoverySeedHosts != null && !discoverySeedHosts.isEmpty()) {
            properties.put("discovery.seed_hosts", String.join(",", discoverySeedHosts));
        } else {
            properties.put("discovery.seed_providers", "file");
        }
        Set<String> seedHosts = resolveDiscoverySeedHosts();
        LOG.info("Opensearch discovery seeds hosts: {}", seedHosts);

        // set default number of replicas to 0 if only one node is known.
        // this does not affect replicas for Graylog managed indices, but resolves some problems for system managed indices.
        // (see http://github.com/opensearch-project/OpenSearch/issues/9438)
        String replicas = (seedHosts == null || seedHosts.isEmpty() || seedHosts.size() == 1) ? "0" : "1";
        properties.put("cluster.default_number_of_replicas", replicas);

        // TODO: why do we have this configured?
        properties.put("node.max_local_storage_nodes", "3");

        return DatanodeConfigurationPart.builder()
                .properties(properties.build())
                .withConfigFile(new TextConfigFile(UNICAST_HOSTS_FILE, String.join("\n", seedHosts)))
                .build();
    }

    private String getInitialClusterManagerNodes() {
        if (localConfiguration.getInitialClusterManagerNodes() != null && !localConfiguration.getInitialClusterManagerNodes().isBlank()) {
            return localConfiguration.getInitialClusterManagerNodes();
        } else {
            return buildInitialManagerNodesList();
        }
    }

    @Nonnull
    private String buildInitialManagerNodesList() {
        // this node itself might not be registered with the node service yet, therefore we always add it to the list.
        return nodeService.allActive().values().stream()
                .filter(this::isManager)
                .map(Node::getHostname)
                .collect(Collectors.collectingAndThen(
                        Collectors.toSet(),
                        hostnames -> {
                            if (localConfiguration.getNodeRoles() == null || localConfiguration.getNodeRoles().isEmpty() ||
                                    localConfiguration.getNodeRoles().contains(OpensearchNodeRole.CLUSTER_MANAGER)) {
                                hostnames.add(localConfiguration.getHostname());
                            }
                            return String.join(",", hostnames);
                        }
                ));
    }

    private boolean isManager(DataNodeDto n) {
        final List<String> roles = n.getOpensearchRoles();
        return roles != null && roles.contains(OpensearchNodeRole.CLUSTER_MANAGER);
    }

    private Set<String> resolveDiscoverySeedHosts() {
        return nodeService.allActive().values().stream()
                .map(DataNodeDto::getClusterAddress)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
