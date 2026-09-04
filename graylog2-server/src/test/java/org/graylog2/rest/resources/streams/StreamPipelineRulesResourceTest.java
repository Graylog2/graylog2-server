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
import org.graylog.plugins.pipelineprocessor.db.RoutingRuleDao;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService;
import org.graylog.plugins.pipelineprocessor.rest.PipelineRestPermissions;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.streams.responses.StreamPipelineRulesResponse;
import org.graylog2.rest.resources.streams.responses.StreamReference;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.security.RestPermissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    @Captor
    private ArgumentCaptor<Predicate<RoutingRuleDao>> entityFilterCaptor;
    @Captor
    private ArgumentCaptor<Predicate<StreamReference>> streamFilterCaptor;

    private Predicate<String> permissionCheck;
    private StreamPipelineRulesResource resource;

    public StreamPipelineRulesResourceTest() {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    @BeforeEach
    void setUp() {
        permissionCheck = permission -> true;
        resource = new TestResource(mongoDbPipelineMetadataService);
    }

    @Test
    void getPage_throwsForbiddenWhenStreamReadNotPermitted() {
        permissionCheck = permission -> false;

        assertThatThrownBy(() -> resource.getPage(STREAM_ID, 1, 50, "", List.of(), "rule", SortOrder.ASCENDING))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(mongoDbPipelineMetadataService);
    }

    @Test
    void getPage_filtersPipelinesByPipelineReadPermission() {
        // User can read stream and pipeline-1, but NOT pipeline-2
        permissionCheck = permission -> !permission.equals(PipelineRestPermissions.PIPELINE_READ + ":" + PIPELINE_ID_2);

        stubPaginatedResult(List.of(
                new StreamPipelineRulesResponse(RULE_ID_1, PIPELINE_ID_1, "Pipeline One", RULE_ID_1, "Rule One", List.of()),
                new StreamPipelineRulesResponse(RULE_ID_2, PIPELINE_ID_2, "Pipeline Two", RULE_ID_2, "Rule Two", List.of())
        ));

        resource.getPage(STREAM_ID, 1, 50, "", List.of(), "rule", SortOrder.ASCENDING);

        // Verify the entity filter predicate correctly filters by PIPELINE_READ
        final Predicate<RoutingRuleDao> entityFilter = entityFilterCaptor.getValue();
        assertThat(entityFilter.test(routingRuleDao(PIPELINE_ID_1, RULE_ID_1))).isTrue();
        assertThat(entityFilter.test(routingRuleDao(PIPELINE_ID_2, RULE_ID_2))).isFalse();
    }

    @Test
    void getPage_filtersConnectedStreamsByStreamsReadPermission() {
        // User can read STREAM_ID and STREAM_ID_2, but NOT STREAM_ID_3
        permissionCheck = permission -> !permission.equals(RestPermissions.STREAMS_READ + ":" + STREAM_ID_3);

        stubPaginatedResult(List.of(
                new StreamPipelineRulesResponse(RULE_ID_1, PIPELINE_ID_1, "Pipeline One", RULE_ID_1, "Rule One",
                        List.of(new StreamReference(STREAM_ID, "Stream One"),
                                new StreamReference(STREAM_ID_2, "Stream Two"),
                                new StreamReference(STREAM_ID_3, "Stream Three")))
        ));

        resource.getPage(STREAM_ID, 1, 50, "", List.of(), "rule", SortOrder.ASCENDING);

        // Verify the connected stream filter predicate correctly filters by STREAMS_READ
        final Predicate<StreamReference> streamFilter = streamFilterCaptor.getValue();
        assertThat(streamFilter.test(new StreamReference(STREAM_ID, "Stream One"))).isTrue();
        assertThat(streamFilter.test(new StreamReference(STREAM_ID_2, "Stream Two"))).isTrue();
        assertThat(streamFilter.test(new StreamReference(STREAM_ID_3, "Stream Three"))).isFalse();
    }

    @Test
    void getPage_returnsResultsWhenFullyPermitted() {
        final List<StreamPipelineRulesResponse> entries = List.of(
                new StreamPipelineRulesResponse(RULE_ID_1, PIPELINE_ID_1, "Pipeline One", RULE_ID_1, "Rule One",
                        List.of(new StreamReference(STREAM_ID, "Stream One")))
        );
        stubPaginatedResult(entries);

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

    private void stubPaginatedResult(List<StreamPipelineRulesResponse> entries) {
        final PaginatedList<StreamPipelineRulesResponse> paginatedList =
                new PaginatedList<>(entries, entries.size(), 1, 50);
        when(mongoDbPipelineMetadataService.getRoutingRulesPaginated(
                eq(STREAM_ID), any(), entityFilterCaptor.capture(), streamFilterCaptor.capture(),
                anyString(), any(SortOrder.class), anyInt(), anyInt()))
                .thenReturn(paginatedList);
    }

    private RoutingRuleDao routingRuleDao(String pipelineId, String ruleId) {
        return RoutingRuleDao.builder()
                .pipelineId(pipelineId).pipelineTitle("Pipeline")
                .ruleId(ruleId).ruleTitle("Rule")
                .routedStreamIds(List.of(STREAM_ID))
                .connectedStreams(List.of())
                .build();
    }

    private class TestResource extends StreamPipelineRulesResource {

        TestResource(MongoDbPipelineMetadataService mongoDbPipelineMetadataService) {
            super(mongoDbPipelineMetadataService);
        }

        @Override
        protected Subject getSubject() {
            final Subject mockSubject = mock(Subject.class);
            when(mockSubject.isPermitted(anyString())).thenAnswer(
                    invocation -> permissionCheck.test(invocation.getArgument(0)));
            when(mockSubject.getPrincipal()).thenReturn("test-user");
            return mockSubject;
        }
    }
}
