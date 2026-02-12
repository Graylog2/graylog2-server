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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.eventbus.EventBus;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineInputsMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbInputsMetadataService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService;
import org.graylog.plugins.pipelineprocessor.events.PipelineConnectionsChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.PipelinesChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.entities.DefaultEntityScope;
import org.graylog2.rest.resources.system.inputs.InputDeletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PipelineMetadataUpdaterTest {

    private final MongoDbPipelineMetadataService pipelineMetadataService = mock(MongoDbPipelineMetadataService.class);
    private final MongoDbInputsMetadataService inputsMetadataService = mock(MongoDbInputsMetadataService.class);
    private final PipelineService pipelineService = mock(PipelineService.class);
    private PipelineMetadataUpdater updater;

    private final PipelineInterpreter.State state = mock(PipelineInterpreter.State.class);
    private final PipelineAnalyzer pipelineAnalyzer = mock(PipelineAnalyzer.class);
    private final EventBus eventBus = mock(EventBus.class);

    @BeforeEach
    void setUp() throws NotFoundException {
        updater = spy(new PipelineMetadataUpdater(
                pipelineMetadataService,
                inputsMetadataService,
                pipelineAnalyzer,
                pipelineService,
                eventBus
        ));

        when(pipelineService.load("id1")).thenReturn(
                PipelineDao.create("id1", DefaultEntityScope.NAME,
                        "title1", "description1", "source1", null, null));
        when(pipelineService.load("id2")).thenReturn(
                PipelineDao.create("id2", DefaultEntityScope.NAME,
                        "title2", "description2", "source2", null, null));

        // stub lower level methods - we are only verifying that they are called with correct parameters
        doReturn(ImmutableMap.of()).when(updater).affectedPipelinesAsMap(any(), any());
    }

    @Test
    void testHandlePipelineChanges() {
        PipelinesChangedEvent event = PipelinesChangedEvent.create(Set.of("id1"), Set.of("id2"));

        updater.handlePipelineChanges(event, state);

        verify(pipelineMetadataService).delete(Set.of("id1"));
        verify(inputsMetadataService).deleteInputMentionsByPipelineId("id1");

        ArgumentCaptor<Set<PipelineDao>> pipelineCaptor = ArgumentCaptor.forClass(Set.class);
        verify(updater).handleUpdates(pipelineCaptor.capture(), any());
        assertTrue(pipelineCaptor.getValue().stream().anyMatch(p -> p.id().equals("id2")));
    }

    @Test
    void testHandleConnectionChanges() {
        PipelineConnectionsChangedEvent event = PipelineConnectionsChangedEvent.create("stream_id", Set.of("id1"));

        updater.handleConnectionChanges(event, state);

        ArgumentCaptor<Set<PipelineDao>> pipelineCaptor = ArgumentCaptor.forClass(Set.class);
        verify(updater).handleUpdates(pipelineCaptor.capture(), any());
        assertTrue(pipelineCaptor.getValue().stream().anyMatch(p -> p.id().equals("id1")));
    }

    @Test
    void testHandleRuleChanges() {
        RulesChangedEvent event = new RulesChangedEvent(
                Set.of(new RulesChangedEvent.Reference("rule1", "Rule 1")),
                Set.of(new RulesChangedEvent.Reference("rule2", "Rule 2")));
        when(pipelineMetadataService.getPipelinesByRules(Set.of("rule2"))).thenReturn(Set.of("pipeline1"));
        doReturn(Set.of(PipelineDao.create("pipeline1", DefaultEntityScope.NAME,
                "title1", "description1", "source1", null, null)))
                .when(updater).affectedPipelines(event);

        updater.handleRuleChanges(event, state);

        verify(inputsMetadataService).deleteInputMentionsByRuleId("rule1");

        ArgumentCaptor<Set<PipelineDao>> pipelineCaptor = ArgumentCaptor.forClass(Set.class);
        verify(updater).handleUpdates(pipelineCaptor.capture(), any());
        assertTrue(pipelineCaptor.getValue().stream().anyMatch(p -> p.id().equals("pipeline1")));
    }

    @Test
    void testHandleInputDeletedDeletesInput() throws NotFoundException {
        InputDeletedEvent event = new InputDeletedEvent("input1", "input1_title");

        when(inputsMetadataService.getByInputId("input1")).thenReturn(
                PipelineInputsMetadataDao.builder()
                        .inputId("input1")
                        .mentionedIn(List.of(
                                new PipelineInputsMetadataDao.MentionedInEntry(
                                        "pipeline1", "stage1", Set.of("stream1"))
                        ))
                        .build()
        );
        updater.handleInputDeleted(event, state);

        verify(inputsMetadataService).deleteInput("input1");
    }

    @Test
    void handleUpdatesPassesResolvedPipelinesFromStateToAnalyzer() throws NotFoundException {
        // Verify that handleUpdates extracts already-resolved pipelines from state.getCurrentPipelines()
        // and passes them to the analyzer — without needing a PipelineResolver.

        // Build a resolved pipeline with a rule already set on its stage
        final Rule rule = new PipelineRuleParser(new FunctionRegistry(Map.of()))
                .parseRule("rule-1", """
                        rule "test-rule"
                        when true
                        then
                        end
                        """, true);
        final Stage stage = Stage.builder()
                .stage(0)
                .match(Stage.Match.EITHER)
                .ruleReferences(List.of("test-rule"))
                .build();
        stage.setRules(List.of(rule));

        final Pipeline resolvedPipeline = Pipeline.builder()
                .id("id2")
                .name("test-pipeline")
                .stages(ImmutableSortedSet.of(stage))
                .build();

        // Set up state to return this resolved pipeline
        when(state.getCurrentPipelines()).thenReturn(ImmutableMap.of("id2", resolvedPipeline));

        // Let the real affectedPipelinesAsMap execute (override the setUp stub)
        doCallRealMethod().when(updater).affectedPipelinesAsMap(any(), any());

        final PipelinesChangedEvent event = PipelinesChangedEvent.create(Set.of(), Set.of("id2"));
        updater.handlePipelineChanges(event, state);

        // Capture the pipelines map passed to the analyzer
        @SuppressWarnings("unchecked")
        final ArgumentCaptor<ImmutableMap<String, Pipeline>> pipelinesCaptor = ArgumentCaptor.forClass(ImmutableMap.class);
        verify(pipelineAnalyzer).analyzePipelines(pipelinesCaptor.capture(), anyList());

        final ImmutableMap<String, Pipeline> passedPipelines = pipelinesCaptor.getValue();
        assertThat(passedPipelines).containsKey("id2");
        assertThat(passedPipelines.get("id2")).isSameAs(resolvedPipeline);
        assertThat(passedPipelines.get("id2").stages().first().getRules())
                .as("Pipeline passed to analyzer should have rules already resolved on its stages")
                .hasSize(1);
        assertThat(passedPipelines.get("id2").stages().first().getRules().get(0).name())
                .isEqualTo("test-rule");
    }
}
