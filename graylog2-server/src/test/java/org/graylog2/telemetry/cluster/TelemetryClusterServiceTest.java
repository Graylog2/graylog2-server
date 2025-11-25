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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import org.graylog2.cluster.leader.LeaderElectionService;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugin.lifecycles.LoadBalancerStatus;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.ServerVersion;
import org.graylog2.shared.system.stats.StatsService;
import org.graylog2.telemetry.cluster.db.DBTelemetryClusterInfo;
import org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryClusterServiceTest {
    private static final String NODE_ID = "node-1";
    private static final String CLUSTER_ID = "cluster-1";
    private static final String HOSTNAME = "localhost";
    private static final LoadBalancerStatus LB_STATUS = LoadBalancerStatus.ALIVE;
    private static final Lifecycle LIFECYCLE = Lifecycle.RUNNING;
    private static final DateTime STARTED_AT = DateTime.now(DateTimeZone.UTC);
    private static final DateTimeZone TIMEZONE = DateTimeZone.UTC;
    private static final long HEAP_USED = 145806336L;
    private static final long HEAP_COMMITTED = 204472320L;
    private static final long HEAP_MAX = 1073741824L;
    private static final long MEMORY_TOTAL = 51539607552L;
    private static final int CPU_CORES = 15;
    private static final boolean IS_LEADER = true;
    private static final boolean IS_PROCESSING = false;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ServerStatus serverStatus;

    @Mock
    ClusterConfigService clusterConfigService;

    @Mock
    LeaderElectionService leaderElectionService;

    @Mock
    DBTelemetryClusterInfo dbTelemetryClusterInfo;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    StatsService statsService;

    @Mock
    ClusterId clusterId;

    @Mock
    NodeId nodeId;

    MetricRegistry metricRegistry;
    TelemetryClusterService telemetryClusterService;

    @BeforeEach
    void setUp() {
        when(nodeId.toString()).thenReturn(NODE_ID);
        when(serverStatus.getNodeId()).thenReturn(nodeId);
        when(clusterId.clusterId()).thenReturn(CLUSTER_ID);
        when(clusterConfigService.get(ClusterId.class)).thenReturn(clusterId);
        when(leaderElectionService.isLeader()).thenReturn(IS_LEADER);
        when(serverStatus.isProcessing()).thenReturn(IS_PROCESSING);
        when(serverStatus.getLifecycle().getLoadbalancerStatus()).thenReturn(LB_STATUS);
        when(serverStatus.getLifecycle()).thenReturn(LIFECYCLE);
        when(serverStatus.getStartedAt()).thenReturn(STARTED_AT);
        when(serverStatus.getTimezone()).thenReturn(TIMEZONE);
        when(statsService.systemStats().osStats().memory().total()).thenReturn(MEMORY_TOTAL);
        when(statsService.systemStats().osStats().processor().totalCores()).thenReturn(CPU_CORES);

        metricRegistry = new MetricRegistry();
        metricRegistry.register(TelemetryClusterService.METRIC_JVM_MEMORY_HEAP_USED, (Gauge<Long>) () -> HEAP_USED);
        metricRegistry.register(TelemetryClusterService.METRIC_JVM_MEMORY_HEAP_COMMITTED, (Gauge<Long>) () -> HEAP_COMMITTED);
        metricRegistry.register(TelemetryClusterService.METRIC_JVM_MEMORY_HEAP_MAX, (Gauge<Long>) () -> HEAP_MAX);

        telemetryClusterService = new TelemetryClusterService(
                serverStatus,
                clusterConfigService,
                leaderElectionService,
                dbTelemetryClusterInfo,
                metricRegistry,
                statsService
        );
    }

    @Test
    void updateTelemetryClusterData() {
        try (MockedStatic<Tools> toolsMock = mockStatic(Tools.class)) {
            toolsMock.when(Tools::getLocalCanonicalHostname).thenReturn(HOSTNAME);

            telemetryClusterService.updateTelemetryClusterData();

            ArgumentCaptor<TelemetryClusterInfoDto> captor = ArgumentCaptor.forClass(TelemetryClusterInfoDto.class);
            verify(dbTelemetryClusterInfo).update(captor.capture());
            TelemetryClusterInfoDto dto = captor.getValue();

            final String expectedFacility = TelemetryClusterService.FACILITY;
            final String expectedLifecycle = LIFECYCLE.getDescription().toLowerCase(Locale.ENGLISH);
            final String expectedCodename = ServerVersion.CODENAME;
            final String expectedVersion = ServerVersion.VERSION.toString();
            final String expectedOperatingSystem = System.getProperty("os.name") + " " + System.getProperty("os.version");;
            final String expectedLbStatus = LB_STATUS.toString().toLowerCase(Locale.ENGLISH);
            final String expectedTimezone = TIMEZONE.toString();

            assertEquals(NODE_ID, dto.nodeId());
            assertEquals(CLUSTER_ID, dto.clusterId());
            assertEquals(HOSTNAME, dto.hostname());
            assertEquals(IS_LEADER, dto.isLeader());
            assertEquals(IS_PROCESSING, dto.isProcessing());
            assertEquals(STARTED_AT, dto.startedAt());
            assertEquals(HEAP_USED, dto.jvmHeapUsed());
            assertEquals(HEAP_COMMITTED, dto.jvmHeapCommitted());
            assertEquals(HEAP_MAX, dto.jvmHeapMax());
            assertEquals(CPU_CORES, dto.cpuCores());
            assertEquals(MEMORY_TOTAL, dto.memoryTotal());
            assertEquals(expectedTimezone, dto.timezone());
            assertEquals(expectedLbStatus, dto.lbStatus());
            assertEquals(expectedLifecycle, dto.lifecycle());
            assertEquals(expectedOperatingSystem, dto.operatingSystem());
            assertEquals(expectedFacility, dto.facility());
            assertEquals(expectedCodename, dto.codename());
            assertEquals(expectedVersion, dto.version());
        }
    }
}
