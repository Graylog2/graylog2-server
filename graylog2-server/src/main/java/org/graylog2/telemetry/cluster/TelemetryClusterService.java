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
package org.graylog2.telemetry.cluster;

import com.google.common.collect.ImmutableMap;
import org.graylog2.cluster.ClusterConfig;
import org.graylog2.cluster.leader.LeaderElectionService;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.shared.ServerVersion;
import org.graylog2.telemetry.cluster.db.DBTelemetryClusterInfo;
import org.joda.time.DateTime;

import jakarta.inject.Inject;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.graylog2.telemetry.cluster.db.DBTelemetryClusterInfo.FIELD_IS_LEADER;
import static org.graylog2.telemetry.cluster.db.DBTelemetryClusterInfo.FIELD_NODE_ID;
import static org.graylog2.telemetry.cluster.db.DBTelemetryClusterInfo.FIELD_VERSION;

public class TelemetryClusterService {

    public static final String UNKNOWN = "unknown";
    private final ServerStatus serverStatus;
    private final String clusterId;
    private final ClusterConfigService clusterConfigService;
    private final LeaderElectionService leaderElectionService;
    private final DBTelemetryClusterInfo dbTelemetryClusterInfo;

    @Inject
    public TelemetryClusterService(ServerStatus serverStatus,
                                   ClusterConfigService clusterConfigService,
                                   LeaderElectionService leaderElectionService,
                                   DBTelemetryClusterInfo dbTelemetryClusterInfo) {
        this.serverStatus = serverStatus;
        this.clusterId = Optional.ofNullable(clusterConfigService.get(ClusterId.class))
                .map(ClusterId::clusterId)
                .orElse(UNKNOWN);
        this.clusterConfigService = clusterConfigService;
        this.leaderElectionService = leaderElectionService;
        this.dbTelemetryClusterInfo = dbTelemetryClusterInfo;
    }

    public void updateTelemetryClusterData() {
        Map<String, Object> nodeInfo = new ImmutableMap.Builder<String, Object>()
                .put("facility", "graylog-server")
                .put("codename", ServerVersion.CODENAME)
                .put(FIELD_NODE_ID, serverStatus.getNodeId().toString())
                .put("cluster_id", clusterId)
                .put(FIELD_VERSION, ServerVersion.VERSION.toString())
                .put("started_at", Tools.getISO8601String(serverStatus.getStartedAt()))
                .put("hostname", Tools.getLocalCanonicalHostname())
                .put("lifecycle", serverStatus.getLifecycle().getDescription().toLowerCase(Locale.ENGLISH))
                .put("lb_status", serverStatus.getLifecycle().getLoadbalancerStatus().toString().toLowerCase(Locale.ENGLISH))
                .put("timezone", serverStatus.getTimezone().getID())
                .put("operating_system", System.getProperty("os.name", UNKNOWN) + " " + System.getProperty("os.version", UNKNOWN))
                .put(FIELD_IS_LEADER, leaderElectionService.isLeader())
                .put("is_processing", serverStatus.isProcessing())
                .build();

        dbTelemetryClusterInfo.update(nodeInfo, serverStatus.getNodeId().toString());
    }

    public String getClusterId() {
        return clusterId;
    }

    public Optional<DateTime> getClusterCreationDate() {
        return Optional.ofNullable(clusterConfigService.getRaw(ClusterId.class)).map(ClusterConfig::lastUpdated);
    }

    public Map<String, Map<String, Object>> nodesTelemetryInfo() {
        return dbTelemetryClusterInfo.findAll();
    }

}
