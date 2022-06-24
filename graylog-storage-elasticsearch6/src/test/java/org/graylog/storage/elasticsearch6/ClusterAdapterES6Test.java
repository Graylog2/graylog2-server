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
package org.graylog.storage.elasticsearch6;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import io.searchbox.client.JestClient;
import io.searchbox.core.CatResult;
import org.graylog2.indexer.cluster.health.NodeDiskUsageStats;
import org.graylog2.indexer.cluster.health.NodeFileDescriptorStats;
import org.graylog2.indexer.cluster.health.NodeRole;
import org.graylog2.indexer.cluster.health.SIUnitParser;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class ClusterAdapterES6Test {

    private final static String SAMPLE_CAT_NODES_RESPONSE = "{" +
            "\"result\" : " +
            "[" +
            "{" +
            "\"id\":\"nodeWithCorrectInfo\"," +
            "\"name\":\"nodeWithCorrectInfo\"," +
            "\"role\":\"dimr\"," +
            "\"ip\":\"182.88.0.2\"," +
            "\"fileDescriptorMax\":1048576," +
            "\"diskUsed\":\"45gb\"," +
            "\"diskTotal\":\"411.5gb\"," +
            "\"diskUsedPercent\":\"10.95\"" +
            "}," +
            "{" +
            "\"id\":\"nodeWithMissingDiskStatistics\"," +
            "\"name\":\"nodeWithMissingDiskStatistics\"," +
            "\"role\":\"dimr\"," +
            "\"ip\":\"182.88.0.1\"" +
            "}" +
            "]" +
            "}";

    private ClusterAdapterES6 clusterAdapter;

    @BeforeEach
    void setUp() throws Exception {
        final JestClient jestClient = mock(JestClient.class);
        final CatResult catResult = mock(CatResult.class);
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final JsonNode catApiJsonResponse = objectMapper.readTree(SAMPLE_CAT_NODES_RESPONSE);

        doReturn(catResult).when(jestClient).execute(any());
        doReturn(catApiJsonResponse).when(catResult).getJsonObject();
        doReturn(true).when(catResult).isSucceeded();
        
        this.clusterAdapter = new ClusterAdapterES6(jestClient, Duration.minutes(1));
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
}
