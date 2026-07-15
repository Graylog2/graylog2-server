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
package org.graylog2.datanode.restart;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.plugins.datanode.DatanodeClusterAdminAdapter;
import org.graylog.plugins.datanode.dto.ClusterState;
import org.graylog.plugins.datanode.dto.Node;
import org.graylog.plugins.datanode.dto.ShardReplication;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.indexer.indices.HealthStatus;
import org.graylog2.rest.resources.datanodes.DatanodeRestApiProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.http.POST;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Singleton
public class RollingRestartActions {
    private static final Logger LOG = LoggerFactory.getLogger(RollingRestartActions.class);

    private final DatanodeClusterAdminAdapter clusterAdmin;
    private final DatanodeRestApiProxy datanodeProxy;
    private final NodeService<DataNodeDto> nodeService;

    @Inject
    public RollingRestartActions(DatanodeClusterAdminAdapter clusterAdmin,
                                 DatanodeRestApiProxy datanodeProxy,
                                 NodeService<DataNodeDto> nodeService) {
        this.clusterAdmin = clusterAdmin;
        this.datanodeProxy = datanodeProxy;
        this.nodeService = nodeService;
    }

    public void prepareCluster() {
        LOG.info("Preparing cluster for rolling restart: disabling shard replication and flushing");
        clusterAdmin.disableShardReplication();
    }

    public void enableAllocation() {
        LOG.info("Re-enabling shard allocation");
        clusterAdmin.enableShardReplication();
    }

    public void upgradeNode(String hostname) {
        LOG.info("Sending UPGRADE trigger to DataNode {}", hostname);
        datanodeProxy.remoteInterface(hostname, DataNodeManagementClient.class, DataNodeManagementClient::upgrade);
    }

    public ClusterState getClusterState() {
        return clusterAdmin.getClusterState();
    }

    public boolean isClusterGreen() {
        try {
            final ClusterState s = clusterAdmin.getClusterState();
            return s.status() == HealthStatus.Green && s.relocatingShards() == 0;
        } catch (Exception e) {
            LOG.debug("Cluster health check failed", e);
            return false;
        }
    }

    public boolean isAllocationEnabled() {
        try {
            return clusterAdmin.getClusterState().shardReplication() == ShardReplication.ALL;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isNodeInCluster(String hostname, String expectedOpensearchVersion) {
        try {
            return clusterAdmin.getClusterState().findByHostname(hostname)
                    .filter(n -> Objects.equals(n.version(), expectedOpensearchVersion))
                    .isPresent();
        } catch (Exception e) {
            LOG.debug("Failed to check node presence for {}", hostname, e);
            return false;
        }
    }

    public Optional<String> electedManagerHostname() {
        try {
            final ClusterState state = clusterAdmin.getClusterState();
            final String managerName = state.managerNode().name();
            return state.opensearchNodes().stream()
                    .filter(n -> n.name().equals(managerName))
                    .map(Node::host)
                    .findFirst();
        } catch (Exception e) {
            LOG.debug("Failed to determine elected manager", e);
            return Optional.empty();
        }
    }

    public List<DataNodeDto> liveDataNodes() {
        return new ArrayList<>(nodeService.allActive().values());
    }

    interface DataNodeManagementClient {
        @POST("management/upgrade")
        Call<Void> upgrade();
    }
}
