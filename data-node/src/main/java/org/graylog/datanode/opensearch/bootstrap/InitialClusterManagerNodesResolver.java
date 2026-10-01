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
package org.graylog.datanode.opensearch.bootstrap;

import jakarta.inject.Inject;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.opensearch.configuration.beans.impl.OpensearchCommonConfigurationBean;
import org.graylog.datanode.opensearch.configuration.beans.impl.OpensearchNodeRole;

import java.util.Optional;

/**
 * Resolves the value of {@code cluster.initial_cluster_manager_nodes}. The setting is used by opensearch only
 * when a new cluster is bootstrapped. Unless configured explicitly, it's set only on the one node that claimed the
 * bootstrap, all other nodes leave it empty and join the cluster via discovery.
 * <p>
 * The local opensearch data directory doesn't tell if the node has ever been part of a cluster, opensearch writes
 * its node metadata there on the very first start. Whether a cluster has been formed is decided by the recorded
 * cluster UUID instead. A node that has already joined a cluster ignores the setting anyway.
 */
public class InitialClusterManagerNodesResolver {

    private final Configuration localConfiguration;
    private final ClusterBootstrapService clusterBootstrapService;

    @Inject
    public InitialClusterManagerNodesResolver(Configuration localConfiguration, ClusterBootstrapService clusterBootstrapService) {
        this.localConfiguration = localConfiguration;
        this.clusterBootstrapService = clusterBootstrapService;
    }

    public Optional<String> resolve() {
        final Optional<String> configured = explicitConfiguration();

        if (!isManagerEligible()) {
            return configured;
        }

        if (configured.isPresent()) {
            // keeps nodes without explicit configuration from bootstrapping a separate cluster
            clusterBootstrapService.registerExplicitBootstrap(configured.get());
            return configured;
        }

        if (clusterBootstrapService.claimBootstrap()) {
            // opensearch matches the initial cluster manager nodes against node names (or IP addresses), not hostnames
            return Optional.of(localConfiguration.getDatanodeNodeName());
        }
        return Optional.empty();
    }

    /**
     * @return true if this node took over the expired bootstrap claim of another node and has to restart opensearch,
     * so that {@link #resolve()} configures it to bootstrap the cluster
     */
    public boolean takeOverExpiredBootstrap() {
        return isManagerEligible() && explicitConfiguration().isEmpty() && clusterBootstrapService.takeOverExpiredClaim();
    }

    private Optional<String> explicitConfiguration() {
        return Optional.ofNullable(localConfiguration.getInitialClusterManagerNodes()).filter(nodes -> !nodes.isBlank());
    }

    private boolean isManagerEligible() {
        return OpensearchCommonConfigurationBean.getNodeRoles(localConfiguration).contains(OpensearchNodeRole.CLUSTER_MANAGER);
    }
}
