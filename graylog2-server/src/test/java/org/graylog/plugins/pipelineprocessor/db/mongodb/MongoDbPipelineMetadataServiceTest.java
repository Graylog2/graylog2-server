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

import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.RoutingRuleDao;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.resources.streams.responses.StreamPipelineRulesResponse;
import org.graylog2.rest.resources.streams.responses.StreamReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.plugins.pipelineprocessor.db.RoutingRuleDao.FIELD_PIPELINE_TITLE;
import static org.graylog.plugins.pipelineprocessor.db.RoutingRuleDao.FIELD_RULE_TITLE;

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
                .ruleTitlesById(Map.of("rule3", "Network Monitor"))
                .connectedStreamTitlesById(Map.of("source_stream_2", "Source Stream 2"))
                .hasInputReferences(false)
                .build();

        final List<RoutingRuleDao> routingRules = List.of(
                RoutingRuleDao.builder()
                        .pipelineId("pipeline1").pipelineTitle("Firewall Pipeline")
                        .ruleId("rule1").ruleTitle("Block Malicious IPs")
                        .routedStreamIds(List.of("s1"))
                        .connectedStreams(List.of(new StreamReference("source_stream_1", "Source Stream 1")))
                        .build(),
                RoutingRuleDao.builder()
                        .pipelineId("pipeline1").pipelineTitle("Firewall Pipeline")
                        .ruleId("rule2").ruleTitle("Log Firewall Events")
                        .routedStreamIds(List.of("s1"))
                        .connectedStreams(List.of(new StreamReference("source_stream_1", "Source Stream 1")))
                        .build(),
                RoutingRuleDao.builder()
                        .pipelineId("pipeline2").pipelineTitle("Network Pipeline")
                        .ruleId("rule3").ruleTitle("Network Monitor")
                        .routedStreamIds(List.of("s1"))
                        .connectedStreams(List.of(new StreamReference("source_stream_2", "Source Stream 2")))
                        .build()
        );

        service.save(List.of(pipeline1, pipeline2), routingRules, false);

        // Page 1 with perPage=2: should return 2 results, total=3
        final PaginatedList<StreamPipelineRulesResponse> page1 = service.getRoutingRulesPaginated(
                "s1", null, FIELD_RULE_TITLE, SortOrder.ASCENDING, 1, 2);
        assertThat(page1).hasSize(2);
        assertThat(page1.pagination().total()).isEqualTo(3);

        // Page 2 with perPage=2: should return 1 result, total=3
        final PaginatedList<StreamPipelineRulesResponse> page2 = service.getRoutingRulesPaginated(
                "s1", null, FIELD_RULE_TITLE, SortOrder.ASCENDING, 2, 2);
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
                .ruleTitlesById(Map.of("rule1", "Zebra Rule", "rule2", "Alpha Rule", "rule3", "Middle Rule"))
                .connectedStreamTitlesById(Map.of("source_stream_1", "Source Stream 1"))
                .hasInputReferences(false)
                .build();

        final List<StreamReference> connected = List.of(new StreamReference("source_stream_1", "Source Stream 1"));
        final List<RoutingRuleDao> routingRules = List.of(
                RoutingRuleDao.builder()
                        .pipelineId("pipeline1").pipelineTitle("Test Pipeline")
                        .ruleId("rule1").ruleTitle("Zebra Rule")
                        .routedStreamIds(List.of("s1")).connectedStreams(connected).build(),
                RoutingRuleDao.builder()
                        .pipelineId("pipeline1").pipelineTitle("Test Pipeline")
                        .ruleId("rule2").ruleTitle("Alpha Rule")
                        .routedStreamIds(List.of("s1")).connectedStreams(connected).build(),
                RoutingRuleDao.builder()
                        .pipelineId("pipeline1").pipelineTitle("Test Pipeline")
                        .ruleId("rule3").ruleTitle("Middle Rule")
                        .routedStreamIds(List.of("s1")).connectedStreams(connected).build()
        );

        service.save(List.of(pipeline), routingRules, false);

        final PaginatedList<StreamPipelineRulesResponse> result = service.getRoutingRulesPaginated(
                "s1", null, FIELD_RULE_TITLE, SortOrder.ASCENDING, 1, 10);

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
                .ruleTitlesById(Map.of("rule2", "Rule B"))
                .connectedStreamTitlesById(Map.of("source_stream_2", "Source Stream 2"))
                .hasInputReferences(false)
                .build();

        final List<RoutingRuleDao> routingRules = List.of(
                RoutingRuleDao.builder()
                        .pipelineId("pipeline1").pipelineTitle("Zebra Pipeline")
                        .ruleId("rule1").ruleTitle("Rule A")
                        .routedStreamIds(List.of("s1"))
                        .connectedStreams(List.of(new StreamReference("source_stream_1", "Source Stream 1")))
                        .build(),
                RoutingRuleDao.builder()
                        .pipelineId("pipeline2").pipelineTitle("Alpha Pipeline")
                        .ruleId("rule2").ruleTitle("Rule B")
                        .routedStreamIds(List.of("s1"))
                        .connectedStreams(List.of(new StreamReference("source_stream_2", "Source Stream 2")))
                        .build()
        );

        service.save(List.of(pipeline1, pipeline2), routingRules, false);

        final PaginatedList<StreamPipelineRulesResponse> result = service.getRoutingRulesPaginated(
                "s1", null, FIELD_PIPELINE_TITLE, SortOrder.ASCENDING, 1, 10);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).pipeline()).isEqualTo("Alpha Pipeline");
        assertThat(result.get(1).pipeline()).isEqualTo("Zebra Pipeline");
    }

    @Test
    void testAdditionalFilter() {
        final PipelineRulesMetadataDao pipeline1 = PipelineRulesMetadataDao.builder()
                .pipelineId("pipeline1")
                .pipelineTitle("Security Pipeline")
                .rules(Set.of("rule1"))
                .streams(Set.of("source_stream_1"))
                .functions(Set.of())
                .deprecatedFunctions(Set.of())
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
                .ruleTitlesById(Map.of("rule2", "Log Rotation"))
                .connectedStreamTitlesById(Map.of("source_stream_2", "Source Stream 2"))
                .hasInputReferences(false)
                .build();

        final List<RoutingRuleDao> routingRules = List.of(
                RoutingRuleDao.builder()
                        .pipelineId("pipeline1").pipelineTitle("Security Pipeline")
                        .ruleId("rule1").ruleTitle("Firewall Block")
                        .routedStreamIds(List.of("s1"))
                        .connectedStreams(List.of(new StreamReference("source_stream_1", "Source Stream 1")))
                        .build(),
                RoutingRuleDao.builder()
                        .pipelineId("pipeline2").pipelineTitle("Ops Pipeline")
                        .ruleId("rule2").ruleTitle("Log Rotation")
                        .routedStreamIds(List.of("s1"))
                        .connectedStreams(List.of(new StreamReference("source_stream_2", "Source Stream 2")))
                        .build()
        );

        service.save(List.of(pipeline1, pipeline2), routingRules, false);

        // Filter by pipeline_id should return only that pipeline's rules
        final Bson pipelineFilter = Filters.eq(RoutingRuleDao.FIELD_PIPELINE_ID, "pipeline1");
        final PaginatedList<StreamPipelineRulesResponse> filtered = service.getRoutingRulesPaginated(
                "s1", pipelineFilter, FIELD_RULE_TITLE, SortOrder.ASCENDING, 1, 10);
        assertThat(filtered).hasSize(1);
        assertThat(filtered.getFirst().rule()).isEqualTo("Firewall Block");
        assertThat(filtered.pagination().total()).isEqualTo(1);

        // Filter by rule_id should return only that specific rule
        final Bson ruleFilter = Filters.eq(RoutingRuleDao.FIELD_RULE_ID, "rule2");
        final PaginatedList<StreamPipelineRulesResponse> ruleFiltered = service.getRoutingRulesPaginated(
                "s1", ruleFilter, FIELD_RULE_TITLE, SortOrder.ASCENDING, 1, 10);
        assertThat(ruleFiltered).hasSize(1);
        assertThat(ruleFiltered.getFirst().rule()).isEqualTo("Log Rotation");

        // No filter should return all rules for the stream
        final PaginatedList<StreamPipelineRulesResponse> unfiltered = service.getRoutingRulesPaginated(
                "s1", null, FIELD_RULE_TITLE, SortOrder.ASCENDING, 1, 10);
        assertThat(unfiltered).hasSize(2);
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
                .ruleTitlesById(Map.of("rule1", "Some Rule"))
                .connectedStreamTitlesById(Map.of("source_stream_1", "Source Stream 1"))
                .hasInputReferences(false)
                .build();

        final List<RoutingRuleDao> routingRules = List.of(
                RoutingRuleDao.builder()
                        .pipelineId("pipeline1").pipelineTitle("Test Pipeline")
                        .ruleId("rule1").ruleTitle("Some Rule")
                        .routedStreamIds(List.of("s1"))
                        .connectedStreams(List.of(new StreamReference("source_stream_1", "Source Stream 1")))
                        .build()
        );

        service.save(List.of(pipeline), routingRules, false);

        final PaginatedList<StreamPipelineRulesResponse> result = service.getRoutingRulesPaginated(
                "nonexistent", null, FIELD_RULE_TITLE, SortOrder.ASCENDING, 1, 10);

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
                .ruleTitlesById(Map.of("rule1", "Routing Rule"))
                .connectedStreamTitlesById(Map.of(
                        "source_stream_1", "Source Stream Alpha",
                        "source_stream_2", "Source Stream Beta"))
                .hasInputReferences(false)
                .build();

        final List<RoutingRuleDao> routingRules = List.of(
                RoutingRuleDao.builder()
                        .pipelineId("pipeline1").pipelineTitle("Multi-Stream Pipeline")
                        .ruleId("rule1").ruleTitle("Routing Rule")
                        .routedStreamIds(List.of("s1"))
                        .connectedStreams(List.of(
                                new StreamReference("source_stream_1", "Source Stream Alpha"),
                                new StreamReference("source_stream_2", "Source Stream Beta")))
                        .build()
        );

        service.save(List.of(pipeline), routingRules, false);

        final PaginatedList<StreamPipelineRulesResponse> result = service.getRoutingRulesPaginated(
                "s1", null, FIELD_RULE_TITLE, SortOrder.ASCENDING, 1, 10);

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

    @Test
    void testRuleRoutingToMultipleStreams() {
        // A single rule routes to s1, s2, and s3
        final PipelineRulesMetadataDao pipeline = PipelineRulesMetadataDao.builder()
                .pipelineId("pipeline1")
                .pipelineTitle("Multi-Route Pipeline")
                .rules(Set.of("rule1"))
                .streams(Set.of("source_stream_1"))
                .functions(Set.of())
                .deprecatedFunctions(Set.of())
                .ruleTitlesById(Map.of("rule1", "Fan-Out Rule"))
                .connectedStreamTitlesById(Map.of("source_stream_1", "Source Stream 1"))
                .hasInputReferences(false)
                .build();

        final List<RoutingRuleDao> routingRules = List.of(
                RoutingRuleDao.builder()
                        .pipelineId("pipeline1").pipelineTitle("Multi-Route Pipeline")
                        .ruleId("rule1").ruleTitle("Fan-Out Rule")
                        .routedStreamIds(List.of("s1", "s2", "s3"))
                        .connectedStreams(List.of(new StreamReference("source_stream_1", "Source Stream 1")))
                        .build()
        );

        service.save(List.of(pipeline), routingRules, false);

        // Querying for each target stream should return the rule
        for (final String streamId : List.of("s1", "s2", "s3")) {
            final PaginatedList<StreamPipelineRulesResponse> result = service.getRoutingRulesPaginated(
                    streamId, null, FIELD_RULE_TITLE, SortOrder.ASCENDING, 1, 10);
            assertThat(result)
                    .as("Query for stream '%s'", streamId)
                    .hasSize(1);
            assertThat(result.getFirst().rule()).isEqualTo("Fan-Out Rule");
            assertThat(result.getFirst().ruleId()).isEqualTo("rule1");
            assertThat(result.getFirst().pipeline()).isEqualTo("Multi-Route Pipeline");
        }

        // Querying for a stream NOT in the set should return nothing
        final PaginatedList<StreamPipelineRulesResponse> noResult = service.getRoutingRulesPaginated(
                "s4", null, FIELD_RULE_TITLE, SortOrder.ASCENDING, 1, 10);
        assertThat(noResult).isEmpty();
    }

    @Test
    void testGetPipelinesByRules() {
        final PipelineRulesMetadataDao pipeline1 = PipelineRulesMetadataDao.builder()
                .pipelineId("pipeline1")
                .pipelineTitle("Pipeline 1")
                .rules(Set.of("rule1", "rule2"))
                .streams(Set.of())
                .functions(Set.of())
                .deprecatedFunctions(Set.of())
                .ruleTitlesById(Map.of())
                .connectedStreamTitlesById(Map.of())
                .hasInputReferences(false)
                .build();

        final PipelineRulesMetadataDao pipeline2 = PipelineRulesMetadataDao.builder()
                .pipelineId("pipeline2")
                .pipelineTitle("Pipeline 2")
                .rules(Set.of("rule2", "rule3"))
                .streams(Set.of())
                .functions(Set.of())
                .deprecatedFunctions(Set.of())
                .ruleTitlesById(Map.of())
                .connectedStreamTitlesById(Map.of())
                .hasInputReferences(false)
                .build();

        service.save(List.of(pipeline1, pipeline2), List.of(), false);

        // Single rule query
        assertThat(service.getPipelinesByRule("rule1")).containsExactly("pipeline1");
        assertThat(service.getPipelinesByRule("rule3")).containsExactly("pipeline2");
        assertThat(service.getPipelinesByRule("rule2")).containsExactlyInAnyOrder("pipeline1", "pipeline2");

        // Batch query (fixed N+1)
        assertThat(service.getPipelinesByRules(Set.of("rule1", "rule3")))
                .containsExactlyInAnyOrder("pipeline1", "pipeline2");
    }

    @Test
    void testUpsertReplacesRoutingRules() {
        // Initial save
        final PipelineRulesMetadataDao pipeline = PipelineRulesMetadataDao.builder()
                .pipelineId("pipeline1")
                .pipelineTitle("Test Pipeline")
                .rules(Set.of("rule1"))
                .streams(Set.of())
                .functions(Set.of())
                .deprecatedFunctions(Set.of())
                .ruleTitlesById(Map.of())
                .connectedStreamTitlesById(Map.of())
                .hasInputReferences(false)
                .build();

        final List<RoutingRuleDao> initialRouting = List.of(
                RoutingRuleDao.builder()
                        .pipelineId("pipeline1").pipelineTitle("Test Pipeline")
                        .ruleId("rule1").ruleTitle("Old Rule")
                        .routedStreamIds(List.of("s1"))
                        .connectedStreams(List.of())
                        .build()
        );

        service.save(List.of(pipeline), initialRouting, false);

        // Verify initial state
        assertThat(service.getRoutingRulesPaginated("s1", null, FIELD_RULE_TITLE, SortOrder.ASCENDING, 1, 10))
                .hasSize(1);

        // Upsert with different routing rules (rule1 removed, rule2 added)
        final PipelineRulesMetadataDao updatedPipeline = PipelineRulesMetadataDao.builder()
                .pipelineId("pipeline1")
                .pipelineTitle("Test Pipeline Updated")
                .rules(Set.of("rule2"))
                .streams(Set.of())
                .functions(Set.of())
                .deprecatedFunctions(Set.of())
                .ruleTitlesById(Map.of())
                .connectedStreamTitlesById(Map.of())
                .hasInputReferences(false)
                .build();

        final List<RoutingRuleDao> updatedRouting = List.of(
                RoutingRuleDao.builder()
                        .pipelineId("pipeline1").pipelineTitle("Test Pipeline Updated")
                        .ruleId("rule2").ruleTitle("New Rule")
                        .routedStreamIds(List.of("s2"))
                        .connectedStreams(List.of())
                        .build()
        );

        service.save(List.of(updatedPipeline), updatedRouting, true);

        // Old routing rule should be gone
        assertThat(service.getRoutingRulesPaginated("s1", null, FIELD_RULE_TITLE, SortOrder.ASCENDING, 1, 10))
                .isEmpty();

        // New routing rule should exist
        final PaginatedList<StreamPipelineRulesResponse> result =
                service.getRoutingRulesPaginated("s2", null, FIELD_RULE_TITLE, SortOrder.ASCENDING, 1, 10);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().rule()).isEqualTo("New Rule");
        assertThat(result.getFirst().pipeline()).isEqualTo("Test Pipeline Updated");
    }
}
