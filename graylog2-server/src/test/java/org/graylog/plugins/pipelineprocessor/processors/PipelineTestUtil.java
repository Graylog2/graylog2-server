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

import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.functions.FromInput;
import org.graylog.plugins.pipelineprocessor.functions.messages.RemoveField;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.inputs.InputService;
import org.graylog2.shared.inputs.InputRegistry;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PipelineTestUtil {

    private final FunctionRegistry functionRegistry = new FunctionRegistry(Map.of(
            RemoveField.NAME, new RemoveField(),
            FromInput.NAME, new FromInput(mock(InputRegistry.class))));
    private final PipelineRuleParser parser = new PipelineRuleParser(functionRegistry);
    private final PipelineStreamConnectionsService connectionsService;
    private final InputService inputService;

    public static final String ALWAYS_TRUE_ID = "always_true_id";
    public static final String REMOVE_FIELD_ID = "remove_field_id";
    public static final String FROM_INPUT_ID = "from_input_id";
    public static final String CONNECTION_ID = "connection1_id";
    public static final String STREAM_ID = "stream1_id";
    public static final String INPUT_NAME = "input1";
    public static final String INPUT_ID = INPUT_NAME + "_id";

    public final Rule ALWAYS_TRUE = parser.parseRule("always_true_id", "rule \"alwaysTrue\" when true then end", true);
    public final Rule REMOVE_FIELD = parser.parseRule("remove_field_id", "rule \"removeField\" when true then remove_field(\"dummy\"); end", true);
    public final Rule FROM_INPUT = parser.parseRule("from_input_id", "rule \"fromInput\" when from_input(name: \"input1\") then end", true);

    public PipelineTestUtil(PipelineStreamConnectionsService connectionsService, InputService inputService) {
        this.connectionsService = connectionsService;
        this.inputService = inputService;
        when(inputService.findIdsByTitle(INPUT_NAME)).thenReturn(List.of(INPUT_ID));
    }

    public Pipeline createPipelineWithRules(String pipelineName, List<Rule> rules) {
        String pipelineId = pipelineName + "_id";
        Pipeline pipeline = Pipeline.builder()
                .id(pipelineId)
                .name(pipelineName)
                .stages(createStagesWithRules(rules))
                .build();
        PipelineConnections connections = PipelineConnections.create(
                CONNECTION_ID, STREAM_ID, Set.of(pipelineId));
        when(connectionsService.loadByPipelineId(pipelineId)).thenReturn(Set.of(connections));

        return pipeline;
    }

    private SortedSet<Stage> createStagesWithRules(List<Rule> rules) {
        Stage stage = Stage.builder()
                .stage(0)
                .match(Stage.Match.ALL)
                .ruleReferences(rules.stream().map(Rule::name).toList())
                .build();
        stage.setRules(rules);

        SortedSet<Stage> stages = new java.util.TreeSet<>((s1, s2) -> 0);
        stages.add(stage);
        return stages;
    }
}
