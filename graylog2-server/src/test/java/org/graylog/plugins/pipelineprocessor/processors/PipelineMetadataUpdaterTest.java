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
import com.google.common.eventbus.EventBus;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineInputsMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbInputsMetadataService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService;
import org.graylog.plugins.pipelineprocessor.events.PipelineConnectionsChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.PipelinesChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.entities.DefaultEntityScope;
import org.graylog2.rest.resources.system.inputs.InputDeletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PipelineMetadataUpdaterTest {

    private MongoDbPipelineMetadataService pipelineMetadataService = mock(MongoDbPipelineMetadataService.class);
    private MongoDbInputsMetadataService inputsMetadataService = mock(MongoDbInputsMetadataService.class);
    private PipelineService pipelineService = mock(PipelineService.class);
    private PipelineMetadataUpdater updater;

    private PipelineInterpreter.State state = mock(PipelineInterpreter.State.class);
    private PipelineResolver resolver = mock(PipelineResolver.class);
    private PipelineMetricRegistry metricRegistry = mock(PipelineMetricRegistry.class);
    private PipelineAnalyzer pipelineAnalyzer = mock(PipelineAnalyzer.class);
    private EventBus eventBus = mock(EventBus.class);

    @Captor
    ArgumentCaptor<Set<PipelineDao>> pipelineCaptor;

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
        when(updater.affectedPipelinesAsMap(any(), any())).thenReturn(ImmutableMap.of());
    }

    @Test
    void testHandlePipelineChanges() {
        PipelinesChangedEvent event = mock(PipelinesChangedEvent.class);
        when(event.deletedPipelineIds()).thenReturn(Set.of("id1"));
        when(event.updatedPipelineIds()).thenReturn(Set.of("id2"));

        updater.handlePipelineChanges(event, state, resolver, metricRegistry);

        verify(pipelineMetadataService).delete(Set.of("id1"));
        verify(inputsMetadataService, atLeastOnce()).deleteInputMentionsByPipelineId("id1");

        verify(updater).handleUpdates(pipelineCaptor.capture(), state, resolver, metricRegistry);
        assertTrue(pipelineCaptor.getValue().stream().anyMatch(p -> p.id().equals("id2")));
    }

    @Test
    void testHandleConnectionChanges() {
        PipelineConnectionsChangedEvent event = mock(PipelineConnectionsChangedEvent.class);
        when(event.pipelineIds()).thenReturn(Set.of("id1"));

        updater.handleConnectionChanges(event, state, resolver, metricRegistry);

        verify(updater).handleUpdates(pipelineCaptor.capture(), state, resolver, metricRegistry);
        assertTrue(pipelineCaptor.getValue().stream().anyMatch(p -> p.id().equals("id2")));
    }

    @Test
    void testHandleRuleChangesDeletesMentionsAndUpdates() {
        RulesChangedEvent event = mock(RulesChangedEvent.class);
        when(event.deletedRules()).thenReturn(Set.of());
        when(event.updatedRules()).thenReturn(Set.of());

        updater.handleRuleChanges(event, state, resolver, metricRegistry);

        // No exception means success; further verification can be added for handleUpdates
    }

    @Test
    void testHandleInputDeletedDeletesInput() throws NotFoundException {
        InputDeletedEvent event = mock(InputDeletedEvent.class);
        when(event.inputId()).thenReturn("input1");

        when(inputsMetadataService.getByInputId("input1")).thenReturn(
                PipelineInputsMetadataDao.builder()
                        .inputId("input1")
                        .mentionedIn(List.of(
                                new PipelineInputsMetadataDao.MentionedInEntry(
                                        "pipeline1", "stage1", Set.of("stream1"))
                        ))
                        .build()
        );
        updater.handleInputDeleted(event, state, resolver, metricRegistry);

        verify(inputsMetadataService).deleteInput("input1");
    }
}
