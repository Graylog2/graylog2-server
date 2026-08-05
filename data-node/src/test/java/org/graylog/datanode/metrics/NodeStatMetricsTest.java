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
package org.graylog.datanode.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NodeStatMetricsTest {

    @Test
    void registryNameMatchesRawOpenSearchStatPath() {
        // The metric-registry gauges (served by /rest/metrics/multiple) are named after the raw OpenSearch
        // stat path, so their values must stay in bytes to match the "_in_bytes" suffix.
        assertThat(NodeStatMetrics.MEM_TOTAL.getMetricRegistryName()).isEqualTo("opensearch.os.mem.total_in_bytes");
        assertThat(NodeStatMetrics.DISK_USED.getMetricRegistryName()).isEqualTo("opensearch.fs.total.total_in_bytes");
    }

    @Test
    void mapValueConvertsByteMetricsToGibForTheIndex() {
        // Values written to the metrics index / dashboards are converted to GiB.
        assertThat(NodeStatMetrics.mapValue("mem_total", 34359738368L)).isEqualTo(32.0f);
        assertThat(NodeStatMetrics.mapValue("disk_free", 614376128512L)).isEqualTo(572.1824f);
    }

    @Test
    void mapValueLeavesNonByteMetricsUnchanged() {
        assertThat(NodeStatMetrics.mapValue("cpu_load", 26.4873046875)).isEqualTo(26.4873046875);
        assertThat(NodeStatMetrics.mapValue("index_total", 244589)).isEqualTo(244589);
    }
}
