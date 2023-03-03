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
package org.graylog.storage.opensearch2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.io.Resources;
import org.graylog.shaded.opensearch2.org.opensearch.OpenSearchException;
import org.graylog.storage.opensearch2.cat.CatApi;
import org.graylog.storage.opensearch2.cat.IndexSummaryResponse;
import org.graylog.storage.opensearch2.cat.NodeResponse;
import org.graylog2.indexer.cluster.health.NodeDiskUsageStats;
import org.graylog2.indexer.cluster.health.NodeFileDescriptorStats;
import org.graylog2.indexer.cluster.health.NodeRole;
import org.graylog2.indexer.cluster.health.SIUnitParser;
import org.graylog2.indexer.indices.HealthStatus;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClusterAdapterOS2Test {
    private static final String nodeId = "I-sZn-HKQhCtdf1JYPcx1A";

    private OpenSearchClient client;
    private CatApi catApi;
    private PlainJsonApi jsonApi;
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private ClusterAdapterOS2 clusterAdapter;

    private final static NodeResponse NODE_WITH_CORRECT_INFO = NodeResponse.create("nodeWithCorrectInfo",
            "nodeWithCorrectInfo",
            "dimr",
            null,
            "182.88.0.2",
            "45gb",
            "411.5gb",
            10.95d,
            1048576L);
    private final static NodeResponse NODE_WITH_MISSING_DISK_STATISTICS = NodeResponse.create("nodeWithMissingDiskStatistics",
            "nodeWithMissingDiskStatistics",
            "dimr",
            null,
            "182.88.0.1",
            null,
            null,
            null,
            null);

    @BeforeEach
    void setUp() {
        this.client = mock(OpenSearchClient.class);
        this.catApi = mock(CatApi.class);
        this.jsonApi = mock(PlainJsonApi.class);

        this.clusterAdapter = new ClusterAdapterOS2(client, Duration.seconds(1), catApi, jsonApi);
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
    void returnsEmptyOptionalForHealthWhenElasticsearchExceptionThrown() {
        when(client.execute(any())).thenThrow(new OpenSearchException("Exception"));
        final Optional<HealthStatus> healthStatus = clusterAdapter.health();
        assertThat(healthStatus).isEmpty();
    }

    @Test
    void testFileDescriptorStats() {
        doReturn(ImmutableList.of(NODE_WITH_CORRECT_INFO, NODE_WITH_MISSING_DISK_STATISTICS)).when(catApi).nodes();
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
        doReturn(ImmutableList.of(NODE_WITH_CORRECT_INFO, NODE_WITH_MISSING_DISK_STATISTICS)).when(catApi).nodes();
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
        when(catApi.aliases()).thenReturn(Map.of(
                "foo_deflector", "foo_42",
                "bar_deflector", "bar_17",
                "baz_deflector", "baz_23"
        ));

        when(catApi.indices()).thenReturn(List.of(
                new IndexSummaryResponse("foo_42", "", "RED"),
                new IndexSummaryResponse("bar_17", "", "YELLOW"),
                new IndexSummaryResponse("baz_23", "", "GREEN")
        ));

        assertThat(clusterAdapter.deflectorHealth(Set.of("foo_deflector", "bar_deflector", "baz_deflector"))).contains(HealthStatus.Red);
    }

    private void mockNodesResponse() throws IOException {
        when(jsonApi.perform(any(), anyString()))
                .thenReturn(objectMapper.readTree(Resources.getResource("nodes-response-without-host-field.json")));
    }
}
