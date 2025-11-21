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

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.inject.Inject;
import org.graylog2.telemetry.cluster.db.DBTelemetryClusterInfo;
import org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.graylog2.telemetry.scheduler.TelemetryMetricSupplier;

import java.util.Map;
import java.util.Optional;

public class NodesSystemMetricsSupplier implements TelemetryMetricSupplier {
    private final DBTelemetryClusterInfo dbTelemetryClusterInfo;

    @Inject
    public NodesSystemMetricsSupplier(DBTelemetryClusterInfo dbTelemetryClusterInfo) {
        this.dbTelemetryClusterInfo = dbTelemetryClusterInfo;
    }

    @Override
    public Optional<TelemetryEvent> get() {
        Map<String, Object> metrics = Map.of(
                "nodes", dbTelemetryClusterInfo.findAll().stream()
                        .map(this::toNodeInfo)
                        .toList()
        );

        return Optional.of(TelemetryEvent.of(metrics));
    }

    private NodeInfo toNodeInfo(TelemetryClusterInfoDto dto) {
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

    public record NodeInfo(
            @JsonProperty("node_id") String nodeId,
            @JsonProperty("operating_system") String operatingSystem,
            @JsonProperty("cpu_cores") Integer cpuCores,
            @JsonProperty("memory_total") Long memoryTotal,
            @JsonProperty("jvm_heap_used") Long jvmHeapUsed,
            @JsonProperty("jvm_heap_committed") Long jvmHeapCommitted,
            @JsonProperty("jvm_heap_max") Long jvmHeapMax
    ) {
    }
}
