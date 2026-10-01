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
import org.graylog.datanode.opensearch.configuration.OpensearchSeedHostsResolver;
import org.graylog.datanode.opensearch.configuration.UnicastHostsFile;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationBean;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class OpensearchClusterConfigurationBean implements DatanodeConfigurationBean<OpensearchConfigurationParams> {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchClusterConfigurationBean.class);

    private final Configuration localConfiguration;
    private final OpensearchSeedHostsResolver seedHostsResolver;

    @Inject
    public OpensearchClusterConfigurationBean(Configuration localConfiguration, OpensearchSeedHostsResolver seedHostsResolver) {
        this.localConfiguration = localConfiguration;
        this.seedHostsResolver = seedHostsResolver;
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

        final String hostname = localConfiguration.getHostname();
        LOG.info("Opensearch networking: bind host: {}, publish host: {}, node name: {}, hostname: {}", bindHost, publishHost, nodeName, hostname);

        // cluster.initial_cluster_manager_nodes is resolved by the InitialClusterManagerNodesResolver, see OpensearchConfigurationService

        final List<String> discoverySeedHosts = localConfiguration.getOpensearchDiscoverySeedHosts();
        if (discoverySeedHosts != null && !discoverySeedHosts.isEmpty()) {
            properties.put("discovery.seed_hosts", String.join(",", discoverySeedHosts));
        } else {
            properties.put("discovery.seed_providers", "file");
        }
        final Set<String> seedHosts = seedHostsResolver.resolve();
        LOG.info("Opensearch discovery seeds hosts: {}", seedHosts);
        if (seedHosts.isEmpty()) {
            LOG.warn("No active data nodes found, opensearch discovery seed hosts are empty. They will be updated as soon as other data nodes register.");
        }

        // set default number of replicas to 0 if only one node is known.
        // this does not affect replicas for Graylog managed indices, but resolves some problems for system managed indices.
        // (see http://github.com/opensearch-project/OpenSearch/issues/9438)
        String replicas = seedHosts.size() <= 1 ? "0" : "1";
        properties.put("cluster.default_number_of_replicas", replicas);

        // TODO: why do we have this configured?
        properties.put("node.max_local_storage_nodes", "3");

        return DatanodeConfigurationPart.builder()
                .properties(properties.build())
                .withConfigFile(UnicastHostsFile.configFile(seedHosts))
                .build();
    }
}
