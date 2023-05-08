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
import com.google.common.primitives.Ints;
import org.graylog2.cluster.leader.LeaderElectionService;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.shared.ServerVersion;
import org.graylog2.telemetry.cluster.db.DBTelemetryClusterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.graylog2.configuration.TelemetryConfiguration.TELEMETRY_CLUSTER_INFO_TTL;
import static org.graylog2.telemetry.cluster.db.DBTelemetryClusterInfo.FIELD_NODE_ID;

public class TelemetryClusterInfoPeriodical extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(TelemetryClusterInfoPeriodical.class);
    private final ServerStatus serverStatus;
    private final String clusterId;
    private final LeaderElectionService leaderElectionService;
    private final DBTelemetryClusterInfo dbTelemetryClusterInfo;
    private final Duration telemetryClusterInfoTtl;

    @Inject
    public TelemetryClusterInfoPeriodical(@Named(TELEMETRY_CLUSTER_INFO_TTL) Duration telemetryClusterInfoTtl,
                                          ServerStatus serverStatus,
                                          ClusterConfigService clusterConfigService,
                                          LeaderElectionService leaderElectionService,
                                          DBTelemetryClusterInfo dbTelemetryClusterInfo) {
        this.telemetryClusterInfoTtl = telemetryClusterInfoTtl;
        this.serverStatus = serverStatus;
        this.clusterId = Optional.ofNullable(clusterConfigService.get(ClusterId.class))
                .map(ClusterId::clusterId)
                .orElse(null);
        this.leaderElectionService = leaderElectionService;
        this.dbTelemetryClusterInfo = dbTelemetryClusterInfo;
    }

    @Override
    public void doRun() {
        Map<String, Object> nodeInfo = new ImmutableMap.Builder<String, Object>()
                .put("facility", "graylog-server")
                .put("codename", ServerVersion.CODENAME)
                .put(FIELD_NODE_ID, serverStatus.getNodeId().toString())
                .put("cluster_id", clusterId)
                .put("version", ServerVersion.VERSION.toString())
                .put("started_at", Tools.getISO8601String(serverStatus.getStartedAt()))
                .put("hostname", Tools.getLocalCanonicalHostname())
                .put("lifecycle", serverStatus.getLifecycle().getDescription().toLowerCase(Locale.ENGLISH))
                .put("lb_status", serverStatus.getLifecycle().getLoadbalancerStatus().toString().toLowerCase(Locale.ENGLISH))
                .put("timezone", serverStatus.getTimezone().getID())
                .put("operating_system", System.getProperty("os.name", "unknown") + " " + System.getProperty("os.version", "unknown"))
                .put("is_leader", leaderElectionService.isLeader())
                .put("is_processing", serverStatus.isProcessing())
                .build();

        dbTelemetryClusterInfo.update(nodeInfo, serverStatus.getNodeId().toString());

    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return Ints.saturatedCast(telemetryClusterInfoTtl.minusMinutes(1).toSeconds());
    }

    @Nonnull
    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
