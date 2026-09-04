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

import com.codahale.metrics.MetricRegistry;
import org.graylog2.cluster.ClusterConfig;
import org.graylog2.cluster.leader.LeaderElectionService;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.shared.ServerVersion;
import org.graylog2.shared.metrics.MetricUtils;
import org.graylog2.shared.system.stats.StatsService;
import org.graylog2.shared.system.stats.os.OsStats;
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
    public static final String METRIC_JVM_MEMORY_HEAP_USED = "jvm.memory.heap.used";
    public static final String METRIC_JVM_MEMORY_HEAP_COMMITTED = "jvm.memory.heap.committed";
    public static final String METRIC_JVM_MEMORY_HEAP_MAX = "jvm.memory.heap.max";

    private final ServerStatus serverStatus;
    private final String clusterId;
    private final ClusterConfigService clusterConfigService;
    private final LeaderElectionService leaderElectionService;
    private final DBTelemetryClusterInfo dbTelemetryClusterInfo;
    private final MetricRegistry metricRegistry;
    private final StatsService statsService;

    @Inject
    public TelemetryClusterService(ServerStatus serverStatus,
                                   ClusterConfigService clusterConfigService,
                                   LeaderElectionService leaderElectionService,
                                   DBTelemetryClusterInfo dbTelemetryClusterInfo,
                                   MetricRegistry metricRegistry,
                                   StatsService statsService) {
        this.serverStatus = serverStatus;
        this.clusterId = Optional.ofNullable(clusterConfigService.get(ClusterId.class))
                .map(ClusterId::clusterId)
                .orElse(UNKNOWN);
        this.clusterConfigService = clusterConfigService;
        this.leaderElectionService = leaderElectionService;
        this.dbTelemetryClusterInfo = dbTelemetryClusterInfo;
        this.metricRegistry = metricRegistry;
        this.statsService = statsService;
    }

    public void updateTelemetryClusterData() {
        final OsStats osStats = statsService.systemStats().osStats();

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
                .jvmHeapUsed(MetricUtils.getGaugeValue(metricRegistry, METRIC_JVM_MEMORY_HEAP_USED).orElse(-1L))
                .jvmHeapCommitted(MetricUtils.getGaugeValue(metricRegistry, METRIC_JVM_MEMORY_HEAP_COMMITTED).orElse(-1L))
                .jvmHeapMax(MetricUtils.getGaugeValue(metricRegistry, METRIC_JVM_MEMORY_HEAP_MAX).orElse(-1L))
                .memoryTotal(osStats.memory().total())
                .cpuCores(osStats.processor().totalCores())
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
