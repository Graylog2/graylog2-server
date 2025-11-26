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
package org.graylog2.telemetry.suppliers;

import org.graylog2.telemetry.cluster.db.DBTelemetryClusterInfo;
import org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto;
import org.graylog2.telemetry.fixtures.TelemetryFixtures;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SystemMetricsSupplierTest {
    @Mock
    private DBTelemetryClusterInfo dbTelemetryClusterInfo;

    @InjectMocks
    private NodesSystemMetricsSupplier supplier;

    @Test
    public void shouldReturnSystemMetrics() {
        TelemetryClusterInfoDto nodeInfo1 = TelemetryFixtures.nodeInfo("node-1", true);
        TelemetryClusterInfoDto nodeInfo2 = TelemetryFixtures.nodeInfo("node-1", false);

        when(dbTelemetryClusterInfo.findAll()).thenReturn(List.of(nodeInfo1, nodeInfo2));

        Optional<TelemetryEvent> event = supplier.get();
        assertTrue(event.isPresent());

        var nodes = (List<NodesSystemMetricsSupplier.NodeInfo>) event.get().metrics().get("nodes");
        assertThat(nodes)
                .hasSize(2)
                .containsExactlyInAnyOrder(
                        toNodeInfo(nodeInfo1),
                        toNodeInfo(nodeInfo2)
                );
    }

    private NodesSystemMetricsSupplier.NodeInfo toNodeInfo(TelemetryClusterInfoDto dto) {
        return new NodesSystemMetricsSupplier.NodeInfo(
                dto.nodeId(),
                dto.operatingSystem(),
                dto.cpuCores(),
                dto.memoryTotal(),
                dto.jvmHeapUsed(),
                dto.jvmHeapCommitted(),
                dto.jvmHeapMax()
        );
    }
}
