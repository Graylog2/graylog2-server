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
package org.graylog2.cluster.nodes.opensearch;

import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.cluster.health.NodeDiskUsageStats;
import org.graylog2.system.stats.elasticsearch.NodeInfo;
import org.graylog2.system.stats.elasticsearch.NodeUtilization;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class OpensearchNodesProviderTest {

    @Test
    void cachesResultAcrossRepeatedCalls() {
        final AtomicInteger fetchCount = new AtomicInteger();
        final OpensearchNodesProvider provider = new OpensearchNodesProvider(countingCluster(fetchCount));

        provider.get();
        provider.get();
        provider.get();

        assertThat(fetchCount.get()).isEqualTo(1);
    }

    private Cluster countingCluster(AtomicInteger fetchCount) {
        return new Cluster(null, null, null, null) {
            @Override
            public Map<String, NodeInfo> getNodesInfo() {
                fetchCount.incrementAndGet();
                return Map.of();
            }

            @Override
            public Map<String, NodeUtilization> getNodesUtilization() {
                return Map.of();
            }

            @Override
            public Set<NodeDiskUsageStats> getDiskUsageStats() {
                return Set.of();
            }
        };
    }
}
