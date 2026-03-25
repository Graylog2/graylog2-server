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
package org.graylog2.inputs.diagnosis;

import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineInputsMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbInputsMetadataService;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.streams.responses.StreamPipelineRulesResponse;
import org.graylog2.rest.resources.system.inputs.responses.InputStreamRulesResponse;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InputRoutingRulesServiceTest {

    @Mock
    private MongoDbInputsMetadataService metadataService;

    @Mock
    private PipelineService pipelineService;

    @Mock
    private RuleService ruleService;

    @Mock
    private StreamRuleService streamRuleService;

    @Mock
    private StreamService streamService;

    private InputRoutingRulesService inputRoutingRulesService;

    @BeforeEach
    void setUp() {
        inputRoutingRulesService = new InputRoutingRulesService(
                metadataService,
                pipelineService,
                ruleService,
                streamRuleService,
                streamService);
    }

    @Test
    void getPipelineRulesPageUsesCompositeIdsWhenRuleAppearsInMultiplePipelines() throws Exception {
        when(metadataService.getByInputId("input-id")).thenReturn(PipelineInputsMetadataDao.builder()
                .inputId("input-id")
                .mentionedIn(List.of(
                        new PipelineInputsMetadataDao.MentionedInEntry("pipeline-id-1", "rule-id-1", Set.of()),
                        new PipelineInputsMetadataDao.MentionedInEntry("pipeline-id-2", "rule-id-1", Set.of())))
                .build());
        when(pipelineService.load("pipeline-id-1")).thenReturn(PipelineDao.builder()
                .id("pipeline-id-1")
                .title("Pipeline A")
                .source("pipeline \"Pipeline A\"")
                .build());
        when(pipelineService.load("pipeline-id-2")).thenReturn(PipelineDao.builder()
                .id("pipeline-id-2")
                .title("Pipeline B")
                .source("pipeline \"Pipeline B\"")
                .build());
        when(ruleService.load("rule-id-1")).thenReturn(RuleDao.builder()
                .id("rule-id-1")
                .title("Rule A")
                .source("rule \"Rule A\"")
                .build());
        PageListResponse<StreamPipelineRulesResponse> response = inputRoutingRulesService.getPipelineRulesPage(
                "input-id",
                pipelineId -> true,
                streamId -> true,
                1,
                50,
                "",
                "rule",
                SortOrder.ASCENDING);

        assertThat(response.elements()).hasSize(2);
        assertThat(response.elements())
                .extracting(StreamPipelineRulesResponse::id)
                .containsExactlyInAnyOrder("pipeline-id-1:rule-id-1", "pipeline-id-2:rule-id-1");
        assertThat(response.elements())
                .extracting(StreamPipelineRulesResponse::ruleId)
                .containsOnly("rule-id-1");
    }

    @Test
    void getStreamRulesPageKeepsStreamRuleIdsUnchanged() {
        StreamRule streamRule = mock(StreamRule.class);
        when(streamRule.getId()).thenReturn("stream-rule-id-1");
        when(streamRule.getStreamId()).thenReturn("stream-id-1");
        when(streamRule.getField()).thenReturn("gl2_source_input");
        when(streamRule.getType()).thenReturn(StreamRuleType.EXACT);
        when(streamRule.getValue()).thenReturn("input-id");
        when(streamRule.getInverted()).thenReturn(false);
        when(streamRule.getDescription()).thenReturn("Route from input");
        when(streamRuleService.loadForInput("input-id")).thenReturn(List.of(streamRule));
        when(streamService.streamTitleFromCache("stream-id-1")).thenReturn("Test Stream");

        PageListResponse<InputStreamRulesResponse> response = inputRoutingRulesService.getStreamRulesPage(
                "input-id",
                streamId -> true,
                1,
                50,
                "",
                "stream",
                SortOrder.ASCENDING);

        assertThat(response.elements()).singleElement().satisfies(rule -> {
            assertThat(rule.id()).isEqualTo("stream-rule-id-1");
            assertThat(rule.streamId()).isEqualTo("stream-id-1");
            assertThat(rule.stream()).isEqualTo("Test Stream");
        });
    }
}
