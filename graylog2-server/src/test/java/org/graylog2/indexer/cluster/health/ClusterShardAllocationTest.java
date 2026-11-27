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
package org.graylog2.indexer.cluster.health;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClusterShardAllocationTest {

    @Test
    void fromCatAllocationMovesUnassignedRowOutOfDataNodes() {
        final ClusterShardAllocation allocation = ClusterShardAllocation.fromCatAllocation(10, ClusterShardAllocation.NO_CLUSTER_LIMIT, List.of(
                new NodeShardAllocation("node1", 7),
                new NodeShardAllocation("UNASSIGNED", 8)
        ), 15);

        assertThat(allocation.nodeShardAllocations()).containsExactly(new NodeShardAllocation("node1", 7));
        assertThat(allocation.unassignedShards()).isEqualTo(8);
        assertThat(allocation.openShards()).isEqualTo(15);
        assertThat(allocation.dataNodes()).isEqualTo(1);
        assertThat(allocation.maxShards()).isEqualTo(10);
    }

    @Test
    void fromCatAllocationWithoutOpenShardsCountsAllRows() {
        final ClusterShardAllocation allocation = ClusterShardAllocation.fromCatAllocation(10, List.of(
                new NodeShardAllocation("node1", 7),
                new NodeShardAllocation("node2", 3),
                new NodeShardAllocation("UNASSIGNED", 8)
        ));

        assertThat(allocation.openShards()).isEqualTo(18);
        assertThat(allocation.maxShards()).isEqualTo(20);
    }

    @Test
    void maxShardsIsCappedByClusterLimit() {
        final List<NodeShardAllocation> dataNodes = List.of(
                new NodeShardAllocation("node1", 1),
                new NodeShardAllocation("node2", 1),
                new NodeShardAllocation("node3", 1));

        assertThat(ClusterShardAllocation.fromCatAllocation(10, 15, dataNodes, 3).maxShards()).isEqualTo(15);
        assertThat(ClusterShardAllocation.fromCatAllocation(10, 50, dataNodes, 3).maxShards()).isEqualTo(30);
    }

    @Test
    void maxShardsDoesNotOverflowForUnboundedMaximum() {
        final ClusterShardAllocation allocation = new ClusterShardAllocation(Integer.MAX_VALUE, List.of(
                new NodeShardAllocation("node1", 1),
                new NodeShardAllocation("node2", 1)
        ));

        assertThat(allocation.maxShards()).isEqualTo(2L * Integer.MAX_VALUE);
        assertThat(allocation.unassignedShards()).isZero();
        assertThat(allocation.openShards()).isEqualTo(2);
    }
}
