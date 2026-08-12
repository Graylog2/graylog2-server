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

import org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService;
import org.graylog.plugins.pipelineprocessor.rest.PipelineRestPermissions;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.metrics.entity.EntityMetric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StreamPipelinesDescriptorTest {

    private MongoDbPipelineMetadataService metadataService;
    private SearchUser searchUser;
    private StreamPipelinesDescriptor descriptor;

    @BeforeEach
    void setUp() {
        metadataService = mock(MongoDbPipelineMetadataService.class);
        searchUser = mock(SearchUser.class);
        descriptor = new StreamPipelinesDescriptor(metadataService);
    }

    @Test
    void compute_returnsPipelineIdsConnectedToStream() {
        when(metadataService.getConnectedToStreams(any())).thenReturn(List.of(
                buildPipelineDao("p1", Set.of("stream-1", "stream-2")),
                buildPipelineDao("p2", Set.of("stream-1")),
                buildPipelineDao("p3", Set.of("stream-2"))
        ));
        when(searchUser.isPermitted(PipelineRestPermissions.PIPELINE_READ, "p1")).thenReturn(true);
        when(searchUser.isPermitted(PipelineRestPermissions.PIPELINE_READ, "p2")).thenReturn(true);
        when(searchUser.isPermitted(PipelineRestPermissions.PIPELINE_READ, "p3")).thenReturn(true);

        final List<EntityMetric<List<String>>> result = descriptor.compute(List.of("stream-1", "stream-2"), searchUser);

        assertThat(result).filteredOn(m -> m.entityId().equals("stream-1"))
                .first().extracting(EntityMetric::value)
                .satisfies(v -> assertThat(v).containsExactlyInAnyOrder("p1", "p2"));
        assertThat(result).filteredOn(m -> m.entityId().equals("stream-2"))
                .first().extracting(EntityMetric::value)
                .satisfies(v -> assertThat(v).containsExactlyInAnyOrder("p1", "p3"));
    }

    @Test
    void compute_filtersByPipelineReadPermission() {
        when(metadataService.getConnectedToStreams(any())).thenReturn(List.of(
                buildPipelineDao("p1", Set.of("stream-1")),
                buildPipelineDao("p2", Set.of("stream-1"))
        ));
        when(searchUser.isPermitted(PipelineRestPermissions.PIPELINE_READ, "p1")).thenReturn(true);
        when(searchUser.isPermitted(PipelineRestPermissions.PIPELINE_READ, "p2")).thenReturn(false);

        final List<EntityMetric<List<String>>> result = descriptor.compute(List.of("stream-1"), searchUser);

        assertThat(result.getFirst().value()).containsExactly("p1");
    }

    @Test
    void compute_returnsEmptyListForStreamWithNoPipelines() {
        when(metadataService.getConnectedToStreams(any())).thenReturn(List.of());

        final List<EntityMetric<List<String>>> result = descriptor.compute(List.of("stream-1"), searchUser);

        assertThat(result.getFirst().value()).isEmpty();
    }

    private static PipelineRulesMetadataDao buildPipelineDao(String pipelineId, Set<String> streams) {
        return PipelineRulesMetadataDao.builder()
                .pipelineId(pipelineId)
                .pipelineTitle("Pipeline " + pipelineId)
                .rules(Set.of())
                .streams(streams)
                .functions(Set.of())
                .deprecatedFunctions(Set.of())
                .ruleTitlesById(Map.of())
                .connectedStreamTitlesById(Map.of())
                .hasInputReferences(false)
                .build();
    }
}
