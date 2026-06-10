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
package org.graylog2.telemetry.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.system.traffic.TrafficCounterService;
import org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto;
import org.graylog2.telemetry.fixtures.TelemetryFixtures;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_CLUSTER_ID;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_CODENAME;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_CPU_CORES;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_FACILITY;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_HOSTNAME;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_IS_LEADER;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_IS_PROCESSING;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_LB_STATUS;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_LIFECYCLE;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_JVM_HEAP_COMMITTED;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_JVM_HEAP_MAX;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_JVM_HEAP_USED;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_MEMORY_TOTAL;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_NODE_ID;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_OPERATING_SYSTEM;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_TIMEZONE;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_VERSION;
import static org.graylog2.telemetry.rest.TelemetryResponseFactory.CLUSTER_ID;
import static org.graylog2.telemetry.rest.TelemetryResponseFactory.INPUT_TRAFFIC_LAST_MONTH;
import static org.graylog2.telemetry.rest.TelemetryResponseFactory.INSTALLATION_SOURCE;
import static org.graylog2.telemetry.rest.TelemetryResponseFactory.LICENSE_COUNT;
import static org.graylog2.telemetry.rest.TelemetryResponseFactory.NODES;
import static org.graylog2.telemetry.rest.TelemetryResponseFactory.NODES_COUNT;
import static org.graylog2.telemetry.rest.TelemetryResponseFactory.NODE_LEADER_APP_VERSION;
import static org.graylog2.telemetry.rest.TelemetryResponseFactory.OUTPUT_TRAFFIC_LAST_MONTH;
import static org.graylog2.telemetry.rest.TelemetryResponseFactory.UNKNOWN_VERSION;
import static org.graylog2.telemetry.rest.TelemetryResponseFactory.USERS_COUNT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TelemetryResponseFactoryTest {
    private final ObjectMapper mapper = new ObjectMapperProvider().get();
    private final TelemetryResponseFactory factory = new TelemetryResponseFactory(mapper);

    @Test
    void createClusterInfo() {
        final long userCount = 42L;
        final String installationSource = "test-src";
        final TelemetryClusterInfoDto leader = TelemetryFixtures.nodeInfo("node-1", true);
        final TelemetryClusterInfoDto nonLeader = TelemetryFixtures.nodeInfo("node-2", false);
        final DateTime date = DateTime.now(DateTimeZone.UTC);
        final TrafficCounterService.TrafficHistogram histogram = traffic(
                Map.of(
                        date.minusDays(1), 10L,
                        date.minusDays(2), 5L
                ),
                Map.of(
                        date.minusDays(1), 3L,
                        date.minusDays(2), 7L
                )
        );

        final ObjectNode clusterInfo = factory.createClusterInfo(
                leader.clusterId(),
                date.minusDays(10),
                List.of(leader, nonLeader),
                histogram,
                userCount,
                installationSource
        );

        assertThat(clusterInfo.get(CLUSTER_ID).asText()).isEqualTo(leader.clusterId());
        assertThat(clusterInfo.get(NODES_COUNT).asInt()).isEqualTo(2);
        assertThat(clusterInfo.get(USERS_COUNT).asLong()).isEqualTo(userCount);
        assertThat(clusterInfo.get(INSTALLATION_SOURCE).asText()).isEqualTo(installationSource);
        assertThat(clusterInfo.get(LICENSE_COUNT).asInt()).isZero();
        assertThat(clusterInfo.get(OUTPUT_TRAFFIC_LAST_MONTH).asLong()).isEqualTo(15L);
        assertThat(clusterInfo.get(INPUT_TRAFFIC_LAST_MONTH).asLong()).isEqualTo(10L);
        assertThat(clusterInfo.get(NODE_LEADER_APP_VERSION).asText()).isEqualTo(leader.version());

        ObjectNode nodes = (ObjectNode) clusterInfo.get(NODES);

        for (TelemetryClusterInfoDto dto : List.of(leader, nonLeader)) {
            ObjectNode node = (ObjectNode) nodes.get(dto.nodeId());
            assertThat(node.get(FIELD_NODE_ID).asText()).isEqualTo(dto.nodeId());
            assertThat(node.get(FIELD_CLUSTER_ID).asText()).isEqualTo(dto.clusterId());
            assertThat(node.get(FIELD_CODENAME).asText()).isEqualTo(dto.codename());
            assertThat(node.get(FIELD_FACILITY).asText()).isEqualTo(dto.facility());
            assertThat(node.get(FIELD_HOSTNAME).asText()).isEqualTo(dto.hostname());
            assertThat(node.get(FIELD_IS_LEADER).asBoolean()).isEqualTo(dto.isLeader());
            assertThat(node.get(FIELD_IS_PROCESSING).asBoolean()).isEqualTo(dto.isProcessing());
            assertThat(node.get(FIELD_LB_STATUS).asText()).isEqualTo(dto.lbStatus());
            assertThat(node.get(FIELD_LIFECYCLE).asText()).isEqualTo(dto.lifecycle());
            assertThat(node.get(FIELD_OPERATING_SYSTEM).asText()).isEqualTo(dto.operatingSystem());
            assertThat(node.get(FIELD_TIMEZONE).asText()).isEqualTo(dto.timezone());
            assertThat(node.get(FIELD_VERSION).asText()).isEqualTo(dto.version());
            assertThat(node.get(FIELD_JVM_HEAP_USED).asLong()).isEqualTo(dto.jvmHeapUsed());
            assertThat(node.get(FIELD_JVM_HEAP_COMMITTED).asLong()).isEqualTo(dto.jvmHeapCommitted());
            assertThat(node.get(FIELD_JVM_HEAP_MAX).asLong()).isEqualTo(dto.jvmHeapMax());
            assertThat(node.get(FIELD_MEMORY_TOTAL).asLong()).isEqualTo(dto.memoryTotal());
            assertThat(node.get(FIELD_CPU_CORES).asInt()).isEqualTo(dto.cpuCores());
        }
    }

    @Test
    void createClusterInfoWithNoLeaderAndEmptyTraffic() {
        final TelemetryClusterInfoDto nonLeader1 = TelemetryFixtures.nodeInfo("node-1", false);
        final TelemetryClusterInfoDto nonLeader2 = TelemetryFixtures.nodeInfo("node-2", false);
        final TrafficCounterService.TrafficHistogram histogram = traffic(Map.of(), Map.of());

        final ObjectNode clusterInfo = factory.createClusterInfo(
                nonLeader1.clusterId(),
                DateTime.now(DateTimeZone.UTC),
                List.of(nonLeader1, nonLeader2),
                histogram,
                0L,
                "test-src"
        );

        assertThat(clusterInfo.get(NODE_LEADER_APP_VERSION).asText()).isEqualTo(UNKNOWN_VERSION);
        assertThat(clusterInfo.get(OUTPUT_TRAFFIC_LAST_MONTH).asLong()).isZero();
        assertThat(clusterInfo.get(INPUT_TRAFFIC_LAST_MONTH).asLong()).isZero();
    }

    private TrafficCounterService.TrafficHistogram traffic(Map<DateTime, Long> output, Map<DateTime, Long> input) {
        TrafficCounterService.TrafficHistogram h = mock(TrafficCounterService.TrafficHistogram.class);
        when(h.output()).thenReturn(output);
        when(h.input()).thenReturn(input);
        return h;
    }
}
