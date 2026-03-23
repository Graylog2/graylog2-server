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

import jakarta.inject.Provider;
import org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService;
import org.graylog.plugins.pipelineprocessor.events.PipelineConnectionsChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.PipelinesChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.rest.resources.system.inputs.InputDeletedEvent;
import org.graylog2.rest.resources.system.inputs.InputRenamedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PipelineMetadataClusterEventHandlerTest {

    private final PipelineInterpreterStateUpdater stateUpdater = mock(PipelineInterpreterStateUpdater.class);
    private final PipelineMetadataUpdater metadataUpdater = mock(PipelineMetadataUpdater.class);
    private final MongoDbPipelineMetadataService pipelineMetadataService = mock(MongoDbPipelineMetadataService.class);
    private final PipelineInterpreter.State state = mock(PipelineInterpreter.State.class);
    private final ClusterEventBus clusterEventBus = mock(ClusterEventBus.class);
    private final ScheduledExecutorService executor = directExecutor();

    private PipelineMetadataClusterEventHandler handler;

    @BeforeEach
    void setUp() {
        Provider<PipelineInterpreterStateUpdater> stateUpdaterProvider = () -> stateUpdater;
        handler = new PipelineMetadataClusterEventHandler(
                clusterEventBus, stateUpdaterProvider, metadataUpdater, pipelineMetadataService, executor);
        when(stateUpdater.getLatestState()).thenReturn(state);
    }

    @Test
    void handleRuleChangesDelegatesToMetadataUpdater() {
        RulesChangedEvent event = new RulesChangedEvent(
                Set.of(new RulesChangedEvent.Reference("rule1", "Rule 1")),
                Set.of());

        handler.handleRuleChanges(event);

        verify(metadataUpdater).handleRuleChanges(event, state);
    }

    @Test
    void handlePipelineChangesDelegatesToMetadataUpdater() {
        PipelinesChangedEvent event = PipelinesChangedEvent.create(Set.of(), Set.of("p1"));

        handler.handlePipelineChanges(event);

        verify(metadataUpdater).handlePipelineChanges(event, state);
    }

    @Test
    void handlePipelineConnectionChangesDelegatesToMetadataUpdater() {
        PipelineConnectionsChangedEvent event = PipelineConnectionsChangedEvent.create("stream1", Set.of("p1"));

        handler.handlePipelineConnectionChanges(event);

        verify(metadataUpdater).handleConnectionChanges(event, state);
    }

    @Test
    void handleInputDeletedDelegatesToMetadataUpdater() {
        InputDeletedEvent event = new InputDeletedEvent("input1", "Input 1");

        handler.handleInputDeleted(event);

        verify(metadataUpdater).handleInputDeleted(event, state);
    }

    @Test
    void handleInputRenamedDiscoversAffectedRulesAndDelegates() {
        InputRenamedEvent event = new InputRenamedEvent("input1", "Old Name", "New Name");

        PipelineRulesMetadataDao dao = PipelineRulesMetadataDao.builder()
                .pipelineId("p1")
                .rules(Set.of("rule1", "rule2"))
                .hasInputReferences(true)
                .build();
        when(pipelineMetadataService.getReferencingPipelines()).thenReturn(Set.of(dao));

        handler.handleInputRenamed(event);

        ArgumentCaptor<RulesChangedEvent> captor = ArgumentCaptor.forClass(RulesChangedEvent.class);
        verify(metadataUpdater).handleRuleChanges(captor.capture(), eq(state));
        RulesChangedEvent syntheticEvent = captor.getValue();
        assertEquals(2, syntheticEvent.updatedRules().size());
        assertTrue(syntheticEvent.deletedRules().isEmpty());
    }

    @Test
    void handleInputRenamedSkipsWhenNoReferencingPipelines() {
        InputRenamedEvent event = new InputRenamedEvent("input1", "Old Name", "New Name");
        when(pipelineMetadataService.getReferencingPipelines()).thenReturn(Set.of());

        handler.handleInputRenamed(event);

        verify(metadataUpdater, never()).handleRuleChanges(any(), any());
    }

    @Test
    void nullStateSkipsMetadataUpdate() {
        when(stateUpdater.getLatestState()).thenReturn(null);

        handler.handleRuleChanges(new RulesChangedEvent(Set.of(), Set.of()));

        verifyNoInteractions(metadataUpdater);
    }

    @Test
    void exceptionFromMetadataUpdaterDoesNotPropagate() {
        RulesChangedEvent event = new RulesChangedEvent(Set.of(), Set.of());
        doThrow(new RuntimeException("test error")).when(metadataUpdater).handleRuleChanges(any(), any());

        // Should not throw
        handler.handleRuleChanges(event);
    }

    private static ScheduledExecutorService directExecutor() {
        final ScheduledExecutorService mock = mock(ScheduledExecutorService.class);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(mock).submit(any(Runnable.class));
        return mock;
    }
}
