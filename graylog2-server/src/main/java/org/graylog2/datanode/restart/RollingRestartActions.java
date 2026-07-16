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
import org.graylog2.security.AccessToken;
import org.graylog2.security.AccessTokenService;
import org.graylog2.system.processing.control.ClusterProcessingControl;
import org.graylog2.system.processing.control.ClusterProcessingControlFactory;
import org.graylog2.system.processing.control.RemoteProcessingControlResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.PeriodDuration;
import retrofit2.Call;
import retrofit2.http.POST;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Singleton
public class RollingRestartActions {
    private static final Logger LOG = LoggerFactory.getLogger(RollingRestartActions.class);

    private static final String TOKEN_NAME = "graylog-datanode-rolling-restart";
    /**
     * TTL for the ephemeral access token minted for the message-processing pause/resume fan-out. The token is
     * used and revoked within a single tick (seconds); the TTL is only a safety net in case revocation fails.
     */
    private static final PeriodDuration TOKEN_TTL = PeriodDuration.of(Duration.ofMinutes(2));

    private final DatanodeClusterAdminAdapter clusterAdmin;
    private final DatanodeRestApiProxy datanodeProxy;
    private final NodeService<DataNodeDto> nodeService;
    private final ClusterProcessingControlFactory clusterProcessingControlFactory;
    private final AccessTokenService accessTokenService;

    @Inject
    public RollingRestartActions(DatanodeClusterAdminAdapter clusterAdmin,
                                 DatanodeRestApiProxy datanodeProxy,
                                 NodeService<DataNodeDto> nodeService,
                                 ClusterProcessingControlFactory clusterProcessingControlFactory,
                                 AccessTokenService accessTokenService) {
        this.clusterAdmin = clusterAdmin;
        this.datanodeProxy = datanodeProxy;
        this.nodeService = nodeService;
        this.clusterProcessingControlFactory = clusterProcessingControlFactory;
        this.accessTokenService = accessTokenService;
    }

    public void prepareCluster() {
        LOG.info("Preparing cluster for rolling restart: disabling shard replication and flushing");
        clusterAdmin.disableShardReplication();
    }

    public void enableAllocation() {
        LOG.info("Re-enabling shard allocation");
        clusterAdmin.enableShardReplication();
    }

    /**
     * Pauses message processing on all Graylog server nodes and waits for their output buffers to drain, so that
     * no in-flight messages are lost while a DataNode's OpenSearch process is restarted. Used for small clusters
     * (1-2 DataNodes) that have no shard redundancy to fall back on.
     *
     * <p>The cluster-wide fan-out is authenticated with a short-lived access token minted on the fly for the
     * triggering user and revoked immediately afterwards, so no long-lived credential is ever persisted.</p>
     */
    public void pauseProcessing(String username) {
        final AccessToken token = accessTokenService.create(username, TOKEN_NAME, TOKEN_TTL);
        try {
            final ClusterProcessingControl<RemoteProcessingControlResource> control =
                    clusterProcessingControlFactory.create(authHeader(token));
            LOG.info("Pausing message processing on all Graylog nodes for rolling restart");
            control.pauseProcessing();
            LOG.info("Waiting for output buffers to drain on all Graylog nodes");
            control.waitForEmptyBuffers();
            LOG.info("Message processing paused and output buffers drained");
        } finally {
            revokeQuietly(token);
        }
    }

    public void resumeProcessing(String username) {
        final AccessToken token = accessTokenService.create(username, TOKEN_NAME, TOKEN_TTL);
        try {
            final ClusterProcessingControl<RemoteProcessingControlResource> control =
                    clusterProcessingControlFactory.create(authHeader(token));
            control.resumeGraylogMessageProcessing();
        } finally {
            revokeQuietly(token);
        }
    }

    private static String authHeader(AccessToken token) {
        // Access tokens authenticate via HTTP Basic with the token as the username and the literal "token" as the
        // password (see AccessTokenAuthenticator).
        final String credentials = token.getToken() + ":token";
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private void revokeQuietly(AccessToken token) {
        try {
            accessTokenService.deleteById(token.getId());
        } catch (Exception e) {
            LOG.warn("Failed to revoke ephemeral rolling-restart access token {}; it will expire within {}",
                    token.getId(), TOKEN_TTL, e);
        }
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
