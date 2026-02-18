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
package org.graylog.plugins.pipelineprocessor.db.mongodb;

import org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.resources.streams.responses.StreamPipelineRulesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MockitoExtension.class)
class MongoDbPipelineMetadataServiceTest {

    private MongoDbPipelineMetadataService service;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        service = new MongoDbPipelineMetadataService(mongoCollections);
    }

    @Test
    void testBasicPagination() {
        // pipeline1 has 2 rules routing to "s1"
        final PipelineRulesMetadataDao pipeline1 = PipelineRulesMetadataDao.builder()
                .pipelineId("pipeline1")
                .pipelineTitle("Firewall Pipeline")
                .rules(Set.of("rule1", "rule2"))
                .streams(Set.of("source_stream_1"))
                .functions(Set.of())
                .deprecatedFunctions(Set.of())
                .streamsByRuleId(Map.of(
                        "rule1", Set.of("s1"),
                        "rule2", Set.of("s1")))
                .routedStreamTitleById(Map.of("s1", "Target Stream 1"))
                .ruleTitlesById(Map.of("rule1", "Block Malicious IPs", "rule2", "Log Firewall Events"))
                .connectedStreamTitlesById(Map.of("source_stream_1", "Source Stream 1"))
                .hasInputReferences(false)
                .build();

        // pipeline2 has 1 rule routing to "s1"
        final PipelineRulesMetadataDao pipeline2 = PipelineRulesMetadataDao.builder()
                .pipelineId("pipeline2")
                .pipelineTitle("Network Pipeline")
                .rules(Set.of("rule3"))
                .streams(Set.of("source_stream_2"))
                .functions(Set.of())
                .deprecatedFunctions(Set.of())
                .streamsByRuleId(Map.of("rule3", Set.of("s1")))
                .routedStreamTitleById(Map.of("s1", "Target Stream 1"))
                .ruleTitlesById(Map.of("rule3", "Network Monitor"))
                .connectedStreamTitlesById(Map.of("source_stream_2", "Source Stream 2"))
                .hasInputReferences(false)
                .build();

        service.save(List.of(pipeline1, pipeline2), false);

        // Page 1 with perPage=2: should return 2 results, total=3
        final PaginatedList<StreamPipelineRulesResponse> page1 = service.getRoutingRulesPaginated(
                "s1", null, "rule", SortOrder.ASCENDING, 1, 2);
        assertThat(page1).hasSize(2);
        assertThat(page1.pagination().total()).isEqualTo(3);

        // Page 2 with perPage=2: should return 1 result, total=3
        final PaginatedList<StreamPipelineRulesResponse> page2 = service.getRoutingRulesPaginated(
                "s1", null, "rule", SortOrder.ASCENDING, 2, 2);
        assertThat(page2).hasSize(1);
        assertThat(page2.pagination().total()).isEqualTo(3);
    }

    @Test
    void testSortByRuleTitle() {
        final PipelineRulesMetadataDao pipeline = PipelineRulesMetadataDao.builder()
                .pipelineId("pipeline1")
                .pipelineTitle("Test Pipeline")
                .rules(Set.of("rule1", "rule2", "rule3"))
                .streams(Set.of("source_stream_1"))
                .functions(Set.of())
                .deprecatedFunctions(Set.of())
                .streamsByRuleId(Map.of(
                        "rule1", Set.of("s1"),
                        "rule2", Set.of("s1"),
                        "rule3", Set.of("s1")))
                .routedStreamTitleById(Map.of("s1", "Target Stream 1"))
                .ruleTitlesById(Map.of("rule1", "Zebra Rule", "rule2", "Alpha Rule", "rule3", "Middle Rule"))
                .connectedStreamTitlesById(Map.of("source_stream_1", "Source Stream 1"))
                .hasInputReferences(false)
                .build();

        service.save(List.of(pipeline), false);

        final PaginatedList<StreamPipelineRulesResponse> result = service.getRoutingRulesPaginated(
                "s1", null, "rule", SortOrder.ASCENDING, 1, 10);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).rule()).isEqualTo("Alpha Rule");
        assertThat(result.get(1).rule()).isEqualTo("Middle Rule");
        assertThat(result.get(2).rule()).isEqualTo("Zebra Rule");
    }

    @Test
    void testSortByPipelineTitle() {
        final PipelineRulesMetadataDao pipeline1 = PipelineRulesMetadataDao.builder()
                .pipelineId("pipeline1")
                .pipelineTitle("Zebra Pipeline")
                .rules(Set.of("rule1"))
                .streams(Set.of("source_stream_1"))
                .functions(Set.of())
                .deprecatedFunctions(Set.of())
                .streamsByRuleId(Map.of("rule1", Set.of("s1")))
                .routedStreamTitleById(Map.of("s1", "Target Stream 1"))
                .ruleTitlesById(Map.of("rule1", "Rule A"))
                .connectedStreamTitlesById(Map.of("source_stream_1", "Source Stream 1"))
                .hasInputReferences(false)
                .build();

        final PipelineRulesMetadataDao pipeline2 = PipelineRulesMetadataDao.builder()
                .pipelineId("pipeline2")
                .pipelineTitle("Alpha Pipeline")
                .rules(Set.of("rule2"))
                .streams(Set.of("source_stream_2"))
                .functions(Set.of())
                .deprecatedFunctions(Set.of())
                .streamsByRuleId(Map.of("rule2", Set.of("s1")))
                .routedStreamTitleById(Map.of("s1", "Target Stream 1"))
                .ruleTitlesById(Map.of("rule2", "Rule B"))
                .connectedStreamTitlesById(Map.of("source_stream_2", "Source Stream 2"))
                .hasInputReferences(false)
                .build();

        service.save(List.of(pipeline1, pipeline2), false);

        final PaginatedList<StreamPipelineRulesResponse> result = service.getRoutingRulesPaginated(
                "s1", null, "pipeline", SortOrder.ASCENDING, 1, 10);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).pipeline()).isEqualTo("Alpha Pipeline");
        assertThat(result.get(1).pipeline()).isEqualTo("Zebra Pipeline");
    }

    @Test
    void testFreeTextSearch() {
        final PipelineRulesMetadataDao pipeline1 = PipelineRulesMetadataDao.builder()
                .pipelineId("pipeline1")
                .pipelineTitle("Security Pipeline")
                .rules(Set.of("rule1"))
                .streams(Set.of("source_stream_1"))
                .functions(Set.of())
                .deprecatedFunctions(Set.of())
                .streamsByRuleId(Map.of("rule1", Set.of("s1")))
                .routedStreamTitleById(Map.of("s1", "Target Stream 1"))
                .ruleTitlesById(Map.of("rule1", "Firewall Block"))
                .connectedStreamTitlesById(Map.of("source_stream_1", "Source Stream 1"))
                .hasInputReferences(false)
                .build();

        final PipelineRulesMetadataDao pipeline2 = PipelineRulesMetadataDao.builder()
                .pipelineId("pipeline2")
                .pipelineTitle("Ops Pipeline")
                .rules(Set.of("rule2"))
                .streams(Set.of("source_stream_2"))
                .functions(Set.of())
                .deprecatedFunctions(Set.of())
                .streamsByRuleId(Map.of("rule2", Set.of("s1")))
                .routedStreamTitleById(Map.of("s1", "Target Stream 1"))
                .ruleTitlesById(Map.of("rule2", "Log Rotation"))
                .connectedStreamTitlesById(Map.of("source_stream_2", "Source Stream 2"))
                .hasInputReferences(false)
                .build();

        service.save(List.of(pipeline1, pipeline2), false);

        // Search "fire" should match only the firewall rule
        final PaginatedList<StreamPipelineRulesResponse> fireResult = service.getRoutingRulesPaginated(
                "s1", "fire", "rule", SortOrder.ASCENDING, 1, 10);
        assertThat(fireResult).hasSize(1);
        assertThat(fireResult.getFirst().rule()).isEqualTo("Firewall Block");
        assertThat(fireResult.pagination().total()).isEqualTo(1);

        // Search "pipeline" should match both (both have "Pipeline" in pipeline title)
        final PaginatedList<StreamPipelineRulesResponse> pipelineResult = service.getRoutingRulesPaginated(
                "s1", "pipeline", "rule", SortOrder.ASCENDING, 1, 10);
        assertThat(pipelineResult).hasSize(2);
        assertThat(pipelineResult.pagination().total()).isEqualTo(2);
    }

    @Test
    void testEmptyResults() {
        final PipelineRulesMetadataDao pipeline = PipelineRulesMetadataDao.builder()
                .pipelineId("pipeline1")
                .pipelineTitle("Test Pipeline")
                .rules(Set.of("rule1"))
                .streams(Set.of("source_stream_1"))
                .functions(Set.of())
                .deprecatedFunctions(Set.of())
                .streamsByRuleId(Map.of("rule1", Set.of("s1")))
                .routedStreamTitleById(Map.of("s1", "Target Stream 1"))
                .ruleTitlesById(Map.of("rule1", "Some Rule"))
                .connectedStreamTitlesById(Map.of("source_stream_1", "Source Stream 1"))
                .hasInputReferences(false)
                .build();

        service.save(List.of(pipeline), false);

        final PaginatedList<StreamPipelineRulesResponse> result = service.getRoutingRulesPaginated(
                "nonexistent", null, "rule", SortOrder.ASCENDING, 1, 10);

        assertThat(result).isEmpty();
        assertThat(result.pagination().total()).isEqualTo(0);
    }

    @Test
    void testConnectedStreamsIncluded() {
        final PipelineRulesMetadataDao pipeline = PipelineRulesMetadataDao.builder()
                .pipelineId("pipeline1")
                .pipelineTitle("Multi-Stream Pipeline")
                .rules(Set.of("rule1"))
                .streams(Set.of("source_stream_1", "source_stream_2"))
                .functions(Set.of())
                .deprecatedFunctions(Set.of())
                .streamsByRuleId(Map.of("rule1", Set.of("s1")))
                .routedStreamTitleById(Map.of("s1", "Target Stream 1"))
                .ruleTitlesById(Map.of("rule1", "Routing Rule"))
                .connectedStreamTitlesById(Map.of(
                        "source_stream_1", "Source Stream Alpha",
                        "source_stream_2", "Source Stream Beta"))
                .hasInputReferences(false)
                .build();

        service.save(List.of(pipeline), false);

        final PaginatedList<StreamPipelineRulesResponse> result = service.getRoutingRulesPaginated(
                "s1", null, "rule", SortOrder.ASCENDING, 1, 10);

        assertThat(result).hasSize(1);
        final StreamPipelineRulesResponse response = result.getFirst();
        assertThat(response.connectedStreams()).hasSize(2);
        assertThat(response.connectedStreams())
                .extracting("id")
                .containsExactlyInAnyOrder("source_stream_1", "source_stream_2");
        assertThat(response.connectedStreams())
                .extracting("title")
                .containsExactlyInAnyOrder("Source Stream Alpha", "Source Stream Beta");
    }
}
