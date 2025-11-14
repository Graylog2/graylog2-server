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

import org.graylog2.cluster.ClusterConfig;
import org.graylog2.cluster.leader.LeaderElectionService;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.shared.ServerVersion;
import org.graylog2.telemetry.cluster.db.DBTelemetryClusterInfo;
import org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto;
import org.joda.time.DateTime;

import jakarta.inject.Inject;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class TelemetryClusterService {

    public static final String UNKNOWN = "unknown";
    public static final String FACILITY = "graylog-server";

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
        TelemetryClusterInfoDto nodeInfo = TelemetryClusterInfoDto.Builder.create()
                .facility(FACILITY)
                .codename(ServerVersion.CODENAME)
                .nodeId(serverStatus.getNodeId().toString())
                .clusterId(clusterId)
                .version(ServerVersion.VERSION.toString())
                .startedAt(Optional.ofNullable(serverStatus.getStartedAt()).orElseGet(Tools::nowUTC))
                .hostname(Tools.getLocalCanonicalHostname())
                .lifecycle(serverStatus.getLifecycle().getDescription().toLowerCase(Locale.ENGLISH))
                .lbStatus(serverStatus.getLifecycle().getLoadbalancerStatus().toString().toLowerCase(Locale.ENGLISH))
                .timezone(serverStatus.getTimezone().getID())
                .operatingSystem(System.getProperty("os.name", UNKNOWN) + " " + System.getProperty("os.version", UNKNOWN))
                .isLeader(leaderElectionService.isLeader())
                .isProcessing(serverStatus.isProcessing())
                .build();

        dbTelemetryClusterInfo.update(nodeInfo);
    }

    public String getClusterId() {
        return clusterId;
    }

    public Optional<DateTime> getClusterCreationDate() {
        return Optional.ofNullable(clusterConfigService.getRaw(ClusterId.class)).map(ClusterConfig::lastUpdated);
    }

    public List<TelemetryClusterInfoDto> nodesTelemetryInfo() {
        return dbTelemetryClusterInfo.findAll();
    }

}
