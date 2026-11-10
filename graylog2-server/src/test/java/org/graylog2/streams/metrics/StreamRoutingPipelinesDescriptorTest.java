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
package org.graylog2.streams.metrics;

import org.graylog.plugins.pipelineprocessor.db.RoutingRuleDao;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService;
import org.graylog.plugins.pipelineprocessor.rest.PipelineRestPermissions;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.metrics.entity.EntityMetric;
import org.graylog2.rest.resources.streams.responses.StreamReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StreamRoutingPipelinesDescriptorTest {

    private MongoDbPipelineMetadataService metadataService;
    private SearchUser searchUser;
    private StreamRoutingPipelinesDescriptor descriptor;

    @BeforeEach
    void setUp() {
        metadataService = mock(MongoDbPipelineMetadataService.class);
        searchUser = mock(SearchUser.class);
        descriptor = new StreamRoutingPipelinesDescriptor(metadataService);
    }

    @Test
    void compute_returnsDistinctPipelineIdsRoutingToStream() {
        // Two rules from the same pipeline route to stream-1 — should deduplicate
        when(metadataService.getRoutingToStreams(any())).thenReturn(List.of(
                buildRoutingRule("p1", "r1", List.of("stream-1")),
                buildRoutingRule("p1", "r2", List.of("stream-1")),
                buildRoutingRule("p2", "r3", List.of("stream-1"))
        ));
        when(searchUser.isPermitted(PipelineRestPermissions.PIPELINE_READ, "p1")).thenReturn(true);
        when(searchUser.isPermitted(PipelineRestPermissions.PIPELINE_READ, "p2")).thenReturn(true);

        final List<EntityMetric<List<String>>> result = descriptor.compute(List.of("stream-1"), searchUser);

        assertThat(result.getFirst().value()).containsExactlyInAnyOrder("p1", "p2");
    }

    @Test
    void compute_filtersByPipelineReadPermission() {
        when(metadataService.getRoutingToStreams(any())).thenReturn(List.of(
                buildRoutingRule("p1", "r1", List.of("stream-1")),
                buildRoutingRule("p2", "r2", List.of("stream-1"))
        ));
        when(searchUser.isPermitted(PipelineRestPermissions.PIPELINE_READ, "p1")).thenReturn(true);
        when(searchUser.isPermitted(PipelineRestPermissions.PIPELINE_READ, "p2")).thenReturn(false);

        final List<EntityMetric<List<String>>> result = descriptor.compute(List.of("stream-1"), searchUser);

        assertThat(result.getFirst().value()).containsExactly("p1");
    }

    @Test
    void compute_returnsEmptyListForStreamWithNoRouting() {
        when(metadataService.getRoutingToStreams(any())).thenReturn(List.of());

        final List<EntityMetric<List<String>>> result = descriptor.compute(List.of("stream-1"), searchUser);

        assertThat(result.getFirst().value()).isEmpty();
    }

    private static RoutingRuleDao buildRoutingRule(String pipelineId, String ruleId, List<String> routedStreamIds) {
        return RoutingRuleDao.builder()
                .pipelineId(pipelineId)
                .pipelineTitle("Pipeline " + pipelineId)
                .ruleId(ruleId)
                .ruleTitle("Rule " + ruleId)
                .routedStreamIds(routedStreamIds)
                .connectedStreams(List.of(new StreamReference("src", "Source")))
                .build();
    }
}
