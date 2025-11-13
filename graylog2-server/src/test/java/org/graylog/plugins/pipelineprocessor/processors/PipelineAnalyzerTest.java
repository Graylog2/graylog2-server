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
import org.graylog2.inputs.InputService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineAnalyzerTest {
    private PipelineAnalyzer pipelineAnalyzer;
    private List<PipelineRulesMetadataDao> ruleRecords;
    private PipelineTestUtil testUtil;

    @Mock
    private PipelineStreamConnectionsService connectionsService;

    @Mock
    private InputService inputService;

    @BeforeEach
    void setUp() throws Exception {
        org.mockito.MockitoAnnotations.openMocks(this).close();
        pipelineAnalyzer = new PipelineAnalyzer(connectionsService, inputService, new MetricRegistry());
        ruleRecords = new ArrayList<>();
        testUtil = new PipelineTestUtil(connectionsService);
    }

    @Test
    void analyzeEmpty() {
        Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> result = pipelineAnalyzer.analyzePipelines(
                ImmutableMap.of(), ImmutableMap.of(), List.of()
        );
        assertTrue(result.isEmpty());
    }

    @Test
    void analyzeSingleRule() {
        Pipeline pipeline1 = testUtil.createPipelineWithRules("pipeline1", List.of(testUtil.ALWAYS_TRUE));

        Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> result = pipelineAnalyzer.analyzePipelines(
                ImmutableMap.of(pipeline1.id(), pipeline1), ImmutableMap.of(pipeline1.id(), pipeline1), ruleRecords);

        assertTrue(result.isEmpty());
        assertTrue(ruleRecords.stream().anyMatch(dao ->
                dao.pipelineId().equals("pipeline1_id") && dao.rules().contains("always_true_id")));
    }

    @Test
    void analyzeDeprecatedFunctions() {

    }


}

