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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.io.Resources;
import org.graylog2.indexer.cluster.health.ClusterShardAllocation;
import org.graylog2.indexer.cluster.health.NodeDiskUsageStats;
import org.graylog2.indexer.cluster.health.NodeFileDescriptorStats;
import org.graylog2.indexer.cluster.health.NodeRole;
import org.graylog2.indexer.cluster.health.NodeShardAllocation;
import org.graylog2.indexer.cluster.health.SIUnitParser;
import org.graylog2.indexer.indices.HealthStatus;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.cat.AliasesResponse;
import org.opensearch.client.opensearch.cat.AllocationResponse;
import org.opensearch.client.opensearch.cat.IndicesResponse;
import org.opensearch.client.opensearch.cat.NodesRequest;
import org.opensearch.client.opensearch.cat.NodesResponse;
import org.opensearch.client.opensearch.cat.OpenSearchCatClient;
import org.opensearch.client.opensearch.cat.aliases.AliasesRecord;
import org.opensearch.client.opensearch.cat.allocation.AllocationRecord;
import org.opensearch.client.opensearch.cat.indices.IndicesRecord;
import org.opensearch.client.opensearch.cat.nodes.NodesRecord;
import org.opensearch.client.opensearch.cluster.GetClusterSettingsRequest;
import org.opensearch.client.opensearch.cluster.GetClusterSettingsResponse;
import org.opensearch.client.opensearch.cluster.HealthRequest;
import org.opensearch.client.opensearch.cluster.OpenSearchClusterClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClusterAdapterOSTest {
    private static final String nodeId = "I-sZn-HKQhCtdf1JYPcx1A";

    @Mock
    private OfficialOpensearchClient client;
    @Mock
    private PlainJsonApi jsonApi;
    @Mock
    private OpenSearchCatClient catClient;
    @Mock
    private OpenSearchClusterClient clusterClient;
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private ClusterAdapterOS clusterAdapter;

    private final NodesRecord NODE_WITH_CORRECT_INFO = NodesRecord.builder()
            .id("nodeWithCorrectInfo")
            .name("nodeWithCorrectInfo")
            .nodeRole("dimr")
            .ip("182.88.0.2")
            .diskUsed("45gb")
            .diskTotal("411.5gb")
            .diskUsedPercent("10.95")
            .fileDescMax("1048576")
            .build();

    private final NodesRecord NODE_WITH_MISSING_DISK_STATISTICS = NodesRecord.builder()
            .id("nodeWithMissingDiskStatistics")
            .name("nodeWithMissingDiskStatistics")
            .nodeRole("dimr")
            .ip("182.88.0.1")
            .build();

    @BeforeEach
    void setUp() {
        lenient().when(client.execute(any(), anyString())).thenCallRealMethod();
        OpenSearchClient sync = mock(OpenSearchClient.class);
        when(client.sync()).thenReturn(sync);
        when(sync.cat()).thenReturn(catClient);
        when(sync.cluster()).thenReturn(clusterClient);
        this.clusterAdapter = new ClusterAdapterOS(client, Duration.seconds(1), jsonApi);
    }

    @Test
    void handlesMissingHostField() throws Exception {
        mockNodesResponse();

        assertThat(this.clusterAdapter.nodeIdToHostName(nodeId)).isEmpty();
    }

    @Test
    void returnsNameForNodeId() throws Exception {
        mockNodesResponse();

        assertThat(this.clusterAdapter.nodeIdToName(nodeId)).isNotEmpty()
                .contains("es02");
    }

    @Test
    void returnsEmptyOptionalForMissingNodeId() throws Exception {
        mockNodesResponse();

        assertThat(this.clusterAdapter.nodeIdToName("foobar")).isEmpty();
    }

    @Test
    void returnsEmptyOptionalForHealthWhenElasticsearchExceptionThrown() throws IOException {
        when(clusterClient.health(any(HealthRequest.class))).thenThrow(new IOException("Exception"));
        final Optional<HealthStatus> healthStatus = clusterAdapter.health();
        assertThat(healthStatus).isEmpty();
    }

    @Test
    void testFileDescriptorStats() throws IOException {
        NodesResponse nodes = mock(NodesResponse.class);
        when(catClient.nodes(any(NodesRequest.class))).thenReturn(nodes);
        when(nodes.valueBody()).thenReturn(List.of(NODE_WITH_CORRECT_INFO, NODE_WITH_MISSING_DISK_STATISTICS));
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
    void testDiskUsageStats() throws IOException {
        NodesResponse nodes = mock(NodesResponse.class);
        when(catClient.nodes(any(NodesRequest.class))).thenReturn(nodes);
        when(nodes.valueBody()).thenReturn(List.of(NODE_WITH_CORRECT_INFO, NODE_WITH_MISSING_DISK_STATISTICS));
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
    void testDeflectorHealth() throws IOException {

        AliasesResponse aliasesResponse = mock(AliasesResponse.class);
        when(aliasesResponse.valueBody()).thenReturn(List.of(
                AliasesRecord.builder().alias("foo_deflector").index("foo_42").build(),
                AliasesRecord.builder().alias("bar_deflector").index("bar_17").build(),
                AliasesRecord.builder().alias("baz_deflector").index("baz_23").build()
        ));
        when(catClient.aliases()).thenReturn(aliasesResponse);

        IndicesResponse indicesResponse = mock(IndicesResponse.class);
        when(indicesResponse.valueBody()).thenReturn(List.of(
                IndicesRecord.builder().index("foo_42").health("RED").build(),
                IndicesRecord.builder().index("bar_17").health("YELLOW").build(),
                IndicesRecord.builder().index("baz_23").health("GREEN").build()
        ));
        when(catClient.indices()).thenReturn(indicesResponse);

        assertThat(clusterAdapter.deflectorHealth(Set.of("foo_deflector", "bar_deflector", "baz_deflector"))).contains(HealthStatus.Red);
    }

    @Test
    void testClusterShardAllocation() throws IOException {

        GetClusterSettingsResponse settings = GetClusterSettingsResponse.builder()
                .defaults(Map.of())
                .transient_(Map.of())
                .persistent("cluster.max_shards_per_node", JsonData.of("42"))
                .build();
        when(clusterClient.getSettings(any(GetClusterSettingsRequest.class))).thenReturn(settings);


        AllocationResponse allocation = mock(AllocationResponse.class);
        when(allocation.valueBody()).thenReturn(List.of(
                AllocationRecord.builder().node("node1").shards("1").build(),
                AllocationRecord.builder().node("node2").shards("2").build()
        ));
        when(catClient.allocation()).thenReturn(allocation);

        final ClusterShardAllocation clusterShardAllocation = clusterAdapter.clusterShardAllocation();

        assertThat(clusterShardAllocation.maxShardsPerNode()).isEqualTo(42);
        assertThat(clusterShardAllocation.nodeShardAllocations()).hasSize(2);
        assertThat(clusterShardAllocation.nodeShardAllocations())
                .extracting(NodeShardAllocation::node)
                .containsExactly("node1", "node2");
        assertThat(clusterShardAllocation.nodeShardAllocations())
                .extracting(NodeShardAllocation::shards)
                .containsExactly(1, 2);
    }


    private void mockNodesResponse() throws IOException {
        when(jsonApi.performRequest(any(), anyString()))
                .thenReturn(objectMapper.readTree(Resources.getResource("nodes-response-without-host-field.json")));
    }
}
