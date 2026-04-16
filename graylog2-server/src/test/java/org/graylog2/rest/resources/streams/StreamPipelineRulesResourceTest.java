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
package org.graylog2.rest.resources.streams;

import jakarta.ws.rs.ForbiddenException;
import org.apache.shiro.subject.Subject;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog.plugins.pipelineprocessor.rest.PipelineRestPermissions;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.streams.responses.StreamPipelineRulesResponse;
import org.graylog2.rest.resources.streams.responses.StreamReference;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
class StreamPipelineRulesResourceTest {

    private static final String STREAM_ID = "stream-1";
    private static final String STREAM_ID_2 = "stream-2";
    private static final String STREAM_ID_3 = "stream-3";
    private static final String PIPELINE_ID_1 = "pipeline-1";
    private static final String PIPELINE_ID_2 = "pipeline-2";
    private static final String RULE_ID_1 = "rule-1";
    private static final String RULE_ID_2 = "rule-2";

    @Mock
    private MongoDbPipelineMetadataService mongoDbPipelineMetadataService;
    @Mock
    private PipelineService pipelineService;
    @Mock
    private RuleService ruleService;
    @Mock
    private PipelineStreamConnectionsService connectionsService;
    @Mock
    private StreamService streamService;

    private Predicate<String> permissionCheck;
    private StreamPipelineRulesResource resource;

    public StreamPipelineRulesResourceTest() {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    @BeforeEach
    void setUp() throws Exception {
        permissionCheck = permission -> true;
        resource = new TestResource(mongoDbPipelineMetadataService, pipelineService,
                ruleService, connectionsService, streamService);

        // Create stream mocks before stubbing to avoid Mockito's UnfinishedStubbing detection
        final Stream stream1 = mockStream(STREAM_ID, "Stream One");
        final Stream stream2 = mockStream(STREAM_ID_2, "Stream Two");
        final Stream stream3 = mockStream(STREAM_ID_3, "Stream Three");
        lenient().when(streamService.load(STREAM_ID)).thenReturn(stream1);
        lenient().when(streamService.load(STREAM_ID_2)).thenReturn(stream2);
        lenient().when(streamService.load(STREAM_ID_3)).thenReturn(stream3);
    }

    @Test
    void getPage_throwsForbiddenWhenStreamReadNotPermitted() {
        permissionCheck = permission -> false;

        assertThatThrownBy(() -> resource.getPage(STREAM_ID, 1, 50, "", List.of(), "rule", SortOrder.ASCENDING))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(mongoDbPipelineMetadataService);
    }

    @Test
    void getPage_filtersPipelinesByPipelineReadPermission() throws Exception {
        // User can read the target stream and pipeline-1, but NOT pipeline-2
        permissionCheck = permission -> !permission.equals(PipelineRestPermissions.PIPELINE_READ + ":" + PIPELINE_ID_2);

        final PipelineRulesMetadataDao dao1 = buildMetadataDao(PIPELINE_ID_1, RULE_ID_1, STREAM_ID);
        final PipelineRulesMetadataDao dao2 = buildMetadataDao(PIPELINE_ID_2, RULE_ID_2, STREAM_ID);
        when(mongoDbPipelineMetadataService.getRoutingPipelines(STREAM_ID)).thenReturn(Set.of(dao1, dao2));

        stubPipeline(PIPELINE_ID_1, "Pipeline One");
        stubRule(RULE_ID_1, "Rule One");
        stubConnections(PIPELINE_ID_1, STREAM_ID);

        final PageListResponse<StreamPipelineRulesResponse> response =
                resource.getPage(STREAM_ID, 1, 50, "", List.of(), "rule", SortOrder.ASCENDING);

        assertThat(response.elements()).hasSize(1);
        assertThat(response.elements().getFirst().pipelineId()).isEqualTo(PIPELINE_ID_1);
    }

    @Test
    void getPage_filtersConnectedStreamsByStreamsReadPermission() throws Exception {
        // User can read STREAM_ID and STREAM_ID_2, but NOT STREAM_ID_3
        permissionCheck = permission -> !permission.equals(RestPermissions.STREAMS_READ + ":" + STREAM_ID_3);

        final PipelineRulesMetadataDao dao = buildMetadataDao(PIPELINE_ID_1, RULE_ID_1, STREAM_ID);
        when(mongoDbPipelineMetadataService.getRoutingPipelines(STREAM_ID)).thenReturn(Set.of(dao));

        stubPipeline(PIPELINE_ID_1, "Pipeline One");
        stubRule(RULE_ID_1, "Rule One");
        // Pipeline is connected to three streams — user can only see two
        stubConnections(PIPELINE_ID_1, STREAM_ID, STREAM_ID_2, STREAM_ID_3);

        final PageListResponse<StreamPipelineRulesResponse> response =
                resource.getPage(STREAM_ID, 1, 50, "", List.of(), "rule", SortOrder.ASCENDING);

        assertThat(response.elements()).hasSize(1);
        final List<String> connectedStreamIds = response.elements().getFirst().connectedStreams().stream()
                .map(StreamReference::id)
                .toList();
        assertThat(connectedStreamIds).containsExactlyInAnyOrder(STREAM_ID, STREAM_ID_2);
        assertThat(connectedStreamIds).doesNotContain(STREAM_ID_3);
    }

    @Test
    void getPage_excludesRulesNotRoutingToQueriedStream() throws Exception {
        // Pipeline has two rules: RULE_ID_1 routes to STREAM_ID, RULE_ID_2 routes to STREAM_ID_2
        final PipelineRulesMetadataDao dao = PipelineRulesMetadataDao.builder()
                .pipelineId(PIPELINE_ID_1)
                .rules(Set.of(RULE_ID_1, RULE_ID_2))
                .streams(Set.of(STREAM_ID, STREAM_ID_2))
                .streamsByRuleId(Map.of(
                        RULE_ID_1, Set.of(STREAM_ID),
                        RULE_ID_2, Set.of(STREAM_ID_2)))
                .build();
        when(mongoDbPipelineMetadataService.getRoutingPipelines(STREAM_ID)).thenReturn(Set.of(dao));

        stubPipeline(PIPELINE_ID_1, "Pipeline One");
        stubRule(RULE_ID_1, "Rule One");
        stubConnections(PIPELINE_ID_1, STREAM_ID, STREAM_ID_2);

        final PageListResponse<StreamPipelineRulesResponse> response =
                resource.getPage(STREAM_ID, 1, 50, "", List.of(), "rule", SortOrder.ASCENDING);

        assertThat(response.elements()).hasSize(1);
        assertThat(response.elements().getFirst().ruleId()).isEqualTo(RULE_ID_1);
        assertThat(response.elements().getFirst().rule()).isEqualTo("Rule One");
    }

    @Test
    void getPage_returnsResultsWhenFullyPermitted() throws Exception {
        final PipelineRulesMetadataDao dao = buildMetadataDao(PIPELINE_ID_1, RULE_ID_1, STREAM_ID);
        when(mongoDbPipelineMetadataService.getRoutingPipelines(STREAM_ID)).thenReturn(Set.of(dao));

        stubPipeline(PIPELINE_ID_1, "Pipeline One");
        stubRule(RULE_ID_1, "Rule One");
        stubConnections(PIPELINE_ID_1, STREAM_ID);

        final PageListResponse<StreamPipelineRulesResponse> response =
                resource.getPage(STREAM_ID, 1, 50, "", List.of(), "rule", SortOrder.ASCENDING);

        assertThat(response.elements()).hasSize(1);
        final StreamPipelineRulesResponse entry = response.elements().getFirst();
        assertThat(entry.pipelineId()).isEqualTo(PIPELINE_ID_1);
        assertThat(entry.pipeline()).isEqualTo("Pipeline One");
        assertThat(entry.ruleId()).isEqualTo(RULE_ID_1);
        assertThat(entry.rule()).isEqualTo("Rule One");
        assertThat(entry.connectedStreams()).hasSize(1);
        assertThat(entry.connectedStreams().getFirst().id()).isEqualTo(STREAM_ID);
    }

    // --- helpers ---

    private PipelineRulesMetadataDao buildMetadataDao(String pipelineId, String ruleId, String streamId) {
        return PipelineRulesMetadataDao.builder()
                .pipelineId(pipelineId)
                .rules(Set.of(ruleId))
                .streams(Set.of(streamId))
                .streamsByRuleId(Map.of(ruleId, Set.of(streamId)))
                .build();
    }

    private void stubPipeline(String pipelineId, String title) throws Exception {
        final PipelineDao pipelineDao = PipelineDao.builder()
                .id(pipelineId)
                .title(title)
                .source("pipeline \"" + title + "\"\nstage 0 match either\nend")
                .build();
        when(pipelineService.load(pipelineId)).thenReturn(pipelineDao);
    }

    private void stubRule(String ruleId, String title) throws Exception {
        final RuleDao ruleDao = RuleDao.builder()
                .id(ruleId)
                .title(title)
                .source("rule \"" + title + "\"\nwhen true\nthen\nend")
                .build();
        when(ruleService.load(ruleId)).thenReturn(ruleDao);
    }

    private void stubConnections(String pipelineId, String... streamIds) {
        final List<PipelineConnections> connections = Arrays.stream(streamIds)
                .map(sid -> PipelineConnections.create(null, sid, Set.of(pipelineId)))
                .toList();
        when(connectionsService.loadByPipelineId(pipelineId)).thenReturn(Set.copyOf(connections));
    }

    private Stream mockStream(String streamId, String title) {
        final Stream stream = mock(Stream.class);
        lenient().when(stream.getTitle()).thenReturn(title);
        lenient().when(stream.getId()).thenReturn(streamId);
        return stream;
    }

    private class TestResource extends StreamPipelineRulesResource {

        TestResource(MongoDbPipelineMetadataService mongoDbPipelineMetadataService,
                     PipelineService pipelineService,
                     RuleService ruleService,
                     PipelineStreamConnectionsService connectionsService,
                     StreamService streamService) {
            super(mongoDbPipelineMetadataService, pipelineService, ruleService, connectionsService, streamService);
        }

        @Override
        protected Subject getSubject() {
            final Subject mockSubject = mock(Subject.class);
            when(mockSubject.isPermitted(anyString())).thenAnswer(
                    invocation -> permissionCheck.test(invocation.getArgument(0)));
            lenient().when(mockSubject.getPrincipal()).thenReturn("test-user");
            return mockSubject;
        }
    }
}
