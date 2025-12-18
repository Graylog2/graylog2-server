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
package org.graylog.plugins.pipelineprocessor.processors;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.db.PipelineInputsMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.functions.messages.StreamCacheService;
import org.graylog2.inputs.InputService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.graylog.plugins.pipelineprocessor.processors.PipelineTestUtil.ALWAYS_TRUE_ID;
import static org.graylog.plugins.pipelineprocessor.processors.PipelineTestUtil.FROM_INPUT_ID;
import static org.graylog.plugins.pipelineprocessor.processors.PipelineTestUtil.GL2_SOURCE_INPUT_ID;
import static org.graylog.plugins.pipelineprocessor.processors.PipelineTestUtil.INPUT_ID;
import static org.graylog.plugins.pipelineprocessor.processors.PipelineTestUtil.REMOVE_FIELD_ID;
import static org.graylog.plugins.pipelineprocessor.processors.PipelineTestUtil.STREAM_ID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PipelineAnalyzerTest {
    private PipelineAnalyzer pipelineAnalyzer;
    private List<PipelineRulesMetadataDao> ruleRecords;
    private PipelineTestUtil testUtil;

    @Mock
    private PipelineStreamConnectionsService connectionsService;

    @Mock
    private InputService inputService;

    @Mock
    private StreamCacheService streamCacheService;

    @BeforeEach
    void setUp() throws Exception {
        org.mockito.MockitoAnnotations.openMocks(this).close();
        pipelineAnalyzer = new PipelineAnalyzer(connectionsService, inputService, new MetricRegistry(), streamCacheService);
        ruleRecords = new ArrayList<>();
        testUtil = new PipelineTestUtil(connectionsService, inputService);

        when(inputService.find(INPUT_ID)).thenReturn(mock(org.graylog2.inputs.Input.class));
    }

    @Test
    void empty() {
        Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> result = pipelineAnalyzer.analyzePipelines(
                ImmutableMap.of(), ImmutableMap.of(), List.of()
        );
        assertTrue(result.isEmpty());
    }

    @Test
    void singleRule() {
        Pipeline pipeline1 = testUtil.createPipelineWithRules("pipeline1", List.of(testUtil.ALWAYS_TRUE));

        Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> result = pipelineAnalyzer.analyzePipelines(
                ImmutableMap.of(pipeline1.id(), pipeline1), ImmutableMap.of(pipeline1.id(), pipeline1), ruleRecords);

        assertTrue(result.isEmpty());
        assertTrue(ruleRecords.stream().anyMatch(dao ->
                dao.pipelineId().equals(pipeline1.id())
                        && dao.functions().isEmpty()
                        && dao.rules().contains(ALWAYS_TRUE_ID)));
    }

    @Test
    void deprecatedFunction() {
        Pipeline pipeline1 = testUtil.createPipelineWithRules("pipeline1", List.of(testUtil.REMOVE_FIELD));

        Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> result = pipelineAnalyzer.analyzePipelines(
                ImmutableMap.of(pipeline1.id(), pipeline1), ImmutableMap.of(pipeline1.id(), pipeline1), ruleRecords);

        assertTrue(result.isEmpty());
        assertTrue(ruleRecords.stream().anyMatch(dao ->
                dao.pipelineId().equals(pipeline1.id())
                        && dao.rules().contains(REMOVE_FIELD_ID)
                        && dao.functions().contains("remove_field")
                        && dao.deprecatedFunctions().contains("remove_field")));
    }

    @Test
    void inputMentionFromFunction() {
        Pipeline pipeline1 = testUtil.createPipelineWithRules("pipeline1", List.of(testUtil.FROM_INPUT));

        Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> result = pipelineAnalyzer.analyzePipelines(
                ImmutableMap.of(pipeline1.id(), pipeline1), ImmutableMap.of(pipeline1.id(), pipeline1), ruleRecords);

        assertTrue(result.containsKey(INPUT_ID));
        Set<PipelineInputsMetadataDao.MentionedInEntry> mentions = result.get(INPUT_ID);
        assertTrue(mentions.stream().anyMatch(entry ->
                entry.pipelineId().equals(pipeline1.id())
                        && entry.connectedStreams().contains(STREAM_ID)
                        && entry.ruleId().equals(FROM_INPUT_ID)));

        assertTrue(ruleRecords.stream().anyMatch(dao ->
                dao.pipelineId().equals(pipeline1.id())
                        && dao.rules().contains(FROM_INPUT_ID)
                        && dao.functions().contains("from_input")));
    }

    @Test
    void inputMentionFromVariable() {
        Pipeline pipeline1 = testUtil.createPipelineWithRules("pipeline1", List.of(testUtil.GL2_SOURCE_INPUT));

        Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> result = pipelineAnalyzer.analyzePipelines(
                ImmutableMap.of(pipeline1.id(), pipeline1), ImmutableMap.of(pipeline1.id(), pipeline1), ruleRecords);

        assertTrue(result.containsKey(INPUT_ID));
        Set<PipelineInputsMetadataDao.MentionedInEntry> mentions = result.get(INPUT_ID);
        assertTrue(mentions.stream().anyMatch(entry ->
                entry.pipelineId().equals(pipeline1.id())
                        && entry.connectedStreams().contains(STREAM_ID)
                        && entry.ruleId().equals(GL2_SOURCE_INPUT_ID)));

        assertTrue(ruleRecords.stream().anyMatch(dao ->
                dao.pipelineId().equals(pipeline1.id())
                        && dao.rules().contains(GL2_SOURCE_INPUT_ID)
                        && dao.functions().contains("has_field")
                        && dao.functions().contains("to_string")
        ));
    }
}

