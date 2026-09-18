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
package org.graylog.datanode.opensearch;

import com.google.common.eventbus.EventBus;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.bootstrap.preflight.DatanodeDirectoriesLockfileCheck;
import org.graylog.datanode.configuration.DatanodeKeystore;
import org.graylog.datanode.opensearch.statemachine.OpensearchStateMachine;
import org.graylog2.bootstrap.preflight.PreflightConfigService;
import org.graylog2.datanode.DataNodeLifecycleEvent;
import org.graylog2.datanode.DataNodeLifecycleTrigger;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
class OpensearchProcessServiceTest {

    private final NodeId nodeId = new SimpleNodeId("test-node");

    @Mock
    private EventBus eventBus;
    @Mock
    private Configuration configuration;
    @Mock
    private DatanodeDirectoriesLockfileCheck lockfileCheck;
    @Mock
    private PreflightConfigService preflightConfigService;
    @Mock
    private OpensearchProcess process;
    @Mock
    private DatanodeKeystore datanodeKeystore;
    @Mock
    private CsrRequester csrRequester;
    @Mock
    private OpensearchStateMachine stateMachine;

    private OpensearchProcessService service;

    @BeforeEach
    void setUp() {
        service = new OpensearchProcessService(eventBus, configuration, nodeId, lockfileCheck, preflightConfigService,
                process, datanodeKeystore, csrRequester, stateMachine);
    }

    @Test
    void startPostsConfigurationRebuildRequestInsteadOfReusingCachedConfiguration() {
        service.handleNodeLifecycleEvent(DataNodeLifecycleEvent.create(nodeId.getNodeId(), DataNodeLifecycleTrigger.START));

        // Starting a (previously stopped) node must trigger a rebuild of the opensearch configuration instead of
        // directly firing the state machine with whatever configuration was cached from before it stopped.
        verify(eventBus).register(service);
        verify(eventBus).post(new OpensearchStartRequestedEvent());
        verifyNoMoreInteractions(eventBus);
        verifyNoInteractions(stateMachine);
    }

    @Test
    void ignoresEventsForOtherNodes() {
        service.handleNodeLifecycleEvent(DataNodeLifecycleEvent.create("some-other-node", DataNodeLifecycleTrigger.START));

        verify(eventBus).register(service);
        verifyNoMoreInteractions(eventBus);
        verifyNoInteractions(stateMachine);
    }
}
