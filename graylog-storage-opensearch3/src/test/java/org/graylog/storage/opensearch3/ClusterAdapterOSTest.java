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
package org.graylog.storage.opensearch3;

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.io.Resources;
import org.graylog.storage.opensearch3.testing.client.mock.ServerlessOpenSearchClient;
import org.graylog2.indexer.cluster.health.ClusterShardAllocation;
import org.graylog2.indexer.cluster.health.NodeDiskUsageStats;
import org.graylog2.indexer.cluster.health.NodeFileDescriptorStats;
import org.graylog2.indexer.cluster.health.NodeRole;
import org.graylog2.indexer.cluster.health.NodeShardAllocation;
import org.graylog2.indexer.cluster.health.SIUnitParser;
import org.graylog2.indexer.indices.HealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClusterAdapterOSTest {
    private static final String nodeId = "I-sZn-HKQhCtdf1JYPcx1A";

    private ClusterAdapterOS clusterAdapter;

    @BeforeEach
    void setUp() {

        final OfficialOpensearchClient client = ServerlessOpenSearchClient.builder()
                .stubResponse("GET", "/_nodes/*", Resources.getResource("nodes-response-without-host-field.json"))
                .stubResponse("GET", "/_cat/nodes", Resources.getResource("cat_nodes.json"))
                .stubResponse("GET", "/_cluster/settings", Resources.getResource("cluster_settings.json"))
                .stubResponse("GET", "/_cat/allocation", Resources.getResource("cat_allocation.json"))
                .stubResponse("GET", "/_cat/aliases", Resources.getResource("cat_aliases.json"))
                .stubResponse("GET", "/_cat/indices", Resources.getResource("cat_indices.json"))
                .stubError("GET", "/_cluster/health", 500, "Server not responding")
                .build();
        this.clusterAdapter = new ClusterAdapterOS(client, Duration.seconds(1));
    }

    @Test
    void handlesMissingHostField() {
        assertThat(this.clusterAdapter.nodeIdToHostName(nodeId)).isEmpty();
    }

    @Test
    void returnsNameForNodeId() {
        assertThat(this.clusterAdapter.nodeIdToName(nodeId)).isNotEmpty()
                .contains("es02");
    }

    @Test
    void returnsEmptyOptionalForMissingNodeId() {
        assertThat(this.clusterAdapter.nodeIdToName("foobar")).isEmpty();
    }

    @Test
    void returnsEmptyOptionalForHealthWhenElasticsearchExceptionThrown() throws IOException {
        final Optional<HealthStatus> healthStatus = clusterAdapter.health();
        assertThat(healthStatus).isEmpty();
    }

    @Test
    void testFileDescriptorStats() {
        final Set<NodeFileDescriptorStats> nodeFileDescriptorStats = clusterAdapter.fileDescriptorStats();

        assertThat(nodeFileDescriptorStats)
                .hasSize(1)
                .noneSatisfy(
                        nodeDescr -> assertThat(nodeDescr.name()).isEqualTo("nodeWithMissingDiskStatistics")
                )
                .first()
                .satisfies(
                        nodeDescr -> {
                            assertThat(nodeDescr.name()).isEqualTo("nodeWithCorrectInfo");
                            assertThat(nodeDescr.ip()).isEqualTo("182.88.0.2");
                            assertThat(nodeDescr.fileDescriptorMax()).isPresent();
                            assertThat(nodeDescr.fileDescriptorMax().get()).isEqualTo(1048576L);
                        }
                );
    }

    @Test
    void testDiskUsageStats() {
        final Set<NodeDiskUsageStats> diskUsageStats = clusterAdapter.diskUsageStats();

        assertThat(diskUsageStats)
                .hasSize(1)
                .noneSatisfy(
                        diskStats -> assertThat(diskStats.name()).isEqualTo("nodeWithMissingDiskStatistics")
                )
                .first()
                .satisfies(
                        nodeDescr -> {
                            assertThat(nodeDescr.name()).isEqualTo("nodeWithCorrectInfo");
                            assertThat(nodeDescr.ip()).isEqualTo("182.88.0.2");
                            assertThat(nodeDescr.roles()).isEqualTo(NodeRole.parseSymbolString("dimr"));
                            assertThat(nodeDescr.diskUsed().getBytes()).isEqualTo(SIUnitParser.parseBytesSizeValue("45gb").getBytes());
                            assertThat(nodeDescr.diskTotal().getBytes()).isEqualTo(SIUnitParser.parseBytesSizeValue("411.5gb").getBytes());
                            assertThat(nodeDescr.diskUsedPercent()).isEqualTo(10.95d);
                        }
                );

    }

    @Test
    void testDeflectorHealth() {
        assertThat(clusterAdapter.deflectorHealth(Set.of("graylog_0", "gl-system-events_deflector", "gl-events_deflector"))).contains(HealthStatus.Red);
    }

    @Test
    void testClusterShardAllocation() {
        final ClusterShardAllocation clusterShardAllocation = clusterAdapter.clusterShardAllocation();

        assertThat(clusterShardAllocation.maxShardsPerNode()).isEqualTo(42);
        assertThat(clusterShardAllocation.nodeShardAllocations()).hasSize(2);
        assertThat(clusterShardAllocation.nodeShardAllocations())
                .extracting(NodeShardAllocation::node)
                .containsExactly("node1", "node2");
        assertThat(clusterShardAllocation.nodeShardAllocations())
                .extracting(NodeShardAllocation::shards)
                .containsExactly(15, 16);
    }
}
