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

import org.graylog.plugins.pipelineprocessor.events.PipelineConnectionsChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.PipelinesChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog.scheduler.system.SystemJobManager;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.rest.resources.system.inputs.InputDeletedEvent;
import org.graylog2.rest.resources.system.inputs.InputRenamedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PipelineMetadataClusterEventHandlerTest {

    private final SystemJobManager systemJobManager = mock(SystemJobManager.class);
    private final ClusterEventBus clusterEventBus = mock(ClusterEventBus.class);

    private PipelineMetadataClusterEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PipelineMetadataClusterEventHandler(clusterEventBus, systemJobManager);
    }

    @Test
    void handleRuleChangesSubmitsJob() {
        RulesChangedEvent event = new RulesChangedEvent(
                Set.of(new RulesChangedEvent.Reference("rule1", "Rule 1")),
                Set.of());

        handler.handleRuleChanges(event);

        ArgumentCaptor<PipelineMetadataUpdateJob.Config> captor = ArgumentCaptor.forClass(PipelineMetadataUpdateJob.Config.class);
        verify(systemJobManager).submit(captor.capture());
        PipelineMetadataUpdateJob.Config config = captor.getValue();
        assertEquals(PipelineMetadataUpdateJob.EventType.RULES_CHANGED, config.eventType());
        assertNotNull(config.rulesEvent());
        assertEquals(event, config.rulesEvent());
    }

    @Test
    void handlePipelineChangesSubmitsJob() {
        PipelinesChangedEvent event = PipelinesChangedEvent.create(Set.of(), Set.of("p1"));

        handler.handlePipelineChanges(event);

        ArgumentCaptor<PipelineMetadataUpdateJob.Config> captor = ArgumentCaptor.forClass(PipelineMetadataUpdateJob.Config.class);
        verify(systemJobManager).submit(captor.capture());
        PipelineMetadataUpdateJob.Config config = captor.getValue();
        assertEquals(PipelineMetadataUpdateJob.EventType.PIPELINES_CHANGED, config.eventType());
        assertEquals(event, config.pipelinesEvent());
    }

    @Test
    void handlePipelineConnectionChangesSubmitsJob() {
        PipelineConnectionsChangedEvent event = PipelineConnectionsChangedEvent.create("stream1", Set.of("p1"));

        handler.handlePipelineConnectionChanges(event);

        ArgumentCaptor<PipelineMetadataUpdateJob.Config> captor = ArgumentCaptor.forClass(PipelineMetadataUpdateJob.Config.class);
        verify(systemJobManager).submit(captor.capture());
        PipelineMetadataUpdateJob.Config config = captor.getValue();
        assertEquals(PipelineMetadataUpdateJob.EventType.PIPELINE_CONNECTIONS_CHANGED, config.eventType());
        assertEquals(event, config.connectionsEvent());
    }

    @Test
    void handleInputDeletedSubmitsJob() {
        InputDeletedEvent event = new InputDeletedEvent("input1", "Input 1");

        handler.handleInputDeleted(event);

        ArgumentCaptor<PipelineMetadataUpdateJob.Config> captor = ArgumentCaptor.forClass(PipelineMetadataUpdateJob.Config.class);
        verify(systemJobManager).submit(captor.capture());
        PipelineMetadataUpdateJob.Config config = captor.getValue();
        assertEquals(PipelineMetadataUpdateJob.EventType.INPUT_DELETED, config.eventType());
        assertEquals(event, config.inputDeletedEvent());
    }

    @Test
    void handleInputRenamedSubmitsJob() {
        InputRenamedEvent event = new InputRenamedEvent("input1", "Old Name", "New Name");

        handler.handleInputRenamed(event);

        ArgumentCaptor<PipelineMetadataUpdateJob.Config> captor = ArgumentCaptor.forClass(PipelineMetadataUpdateJob.Config.class);
        verify(systemJobManager).submit(captor.capture());
        PipelineMetadataUpdateJob.Config config = captor.getValue();
        assertEquals(PipelineMetadataUpdateJob.EventType.INPUT_RENAMED, config.eventType());
        assertEquals(event, config.inputRenamedEvent());
    }

    @Test
    void configFactoryMethodsSetCorrectEventTypeAndNullOthers() {
        RulesChangedEvent rulesEvent = new RulesChangedEvent(Set.of(), Set.of());
        PipelineMetadataUpdateJob.Config config = PipelineMetadataUpdateJob.forRulesChanged(rulesEvent);

        assertEquals(PipelineMetadataUpdateJob.EventType.RULES_CHANGED, config.eventType());
        assertNotNull(config.rulesEvent());
        assertNull(config.pipelinesEvent());
        assertNull(config.connectionsEvent());
        assertNull(config.inputDeletedEvent());
        assertNull(config.inputRenamedEvent());
    }
}
