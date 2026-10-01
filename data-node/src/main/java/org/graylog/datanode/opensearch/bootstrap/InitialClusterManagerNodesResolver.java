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
import org.graylog.datanode.filesystem.index.IndicesDirectoryParser;
import org.graylog.datanode.opensearch.configuration.beans.impl.OpensearchCommonConfigurationBean;
import org.graylog.datanode.opensearch.configuration.beans.impl.OpensearchNodeRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Resolves the value of {@code cluster.initial_cluster_manager_nodes}. The setting is used by opensearch only
 * when a new cluster is bootstrapped. It's set only on the one node that claimed the bootstrap, all other nodes
 * leave it empty and join the cluster via discovery.
 */
public class InitialClusterManagerNodesResolver {

    private static final Logger LOG = LoggerFactory.getLogger(InitialClusterManagerNodesResolver.class);

    private final Configuration localConfiguration;
    private final ClusterBootstrapService clusterBootstrapService;

    @Inject
    public InitialClusterManagerNodesResolver(Configuration localConfiguration, ClusterBootstrapService clusterBootstrapService) {
        this.localConfiguration = localConfiguration;
        this.clusterBootstrapService = clusterBootstrapService;
    }

    public Optional<String> resolve(Path opensearchDataDir) {
        final String configured = localConfiguration.getInitialClusterManagerNodes();
        if (configured != null && !configured.isBlank()) {
            return Optional.of(configured);
        }

        if (!OpensearchCommonConfigurationBean.getNodeRoles(localConfiguration).contains(OpensearchNodeRole.CLUSTER_MANAGER)) {
            return Optional.empty();
        }

        if (hasClusterState(opensearchDataDir)) {
            LOG.debug("Opensearch data directory {} contains cluster state, no cluster bootstrap needed", opensearchDataDir);
            return Optional.empty();
        }

        if (clusterBootstrapService.claimBootstrap()) {
            // opensearch matches the initial cluster manager nodes against node names (or IP addresses), not hostnames
            return Optional.of(localConfiguration.getDatanodeNodeName());
        }
        return Optional.empty();
    }

    /**
     * A node that has been part of a cluster keeps its state in {@code nodes/<n>/_state} and never bootstraps again.
     */
    static boolean hasClusterState(Path opensearchDataDir) {
        final Path nodesDir = opensearchDataDir.resolve("nodes");
        if (!Files.isDirectory(nodesDir)) {
            return false;
        }
        try (Stream<Path> nodeDirs = Files.list(nodesDir)) {
            return nodeDirs.anyMatch(dir -> Files.isDirectory(dir.resolve(IndicesDirectoryParser.STATE_DIR_NAME)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
