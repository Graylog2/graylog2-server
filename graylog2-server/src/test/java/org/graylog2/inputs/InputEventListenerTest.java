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
package org.graylog2.inputs;

import com.google.common.eventbus.EventBus;
import org.graylog2.cluster.leader.LeaderElectionService;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.rest.models.system.inputs.responses.InputCreated;
import org.graylog2.rest.models.system.inputs.responses.InputDeleted;
import org.graylog2.rest.models.system.inputs.responses.InputUpdated;
import org.graylog2.shared.inputs.InputLauncher;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.PersistedInputs;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class InputEventListenerTest {
    private static final String INPUT_ID = "input-id";
    private static final String THIS_NODE_ID = "5ca1ab1e-0000-4000-a000-000000000000";
    private static final String OTHER_NODE_ID = "c0c0a000-0000-4000-a000-000000000000";

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private InputLauncher inputLauncher;
    @Mock
    private InputRegistry inputRegistry;
    @Mock
    private InputService inputService;
    @Mock
    private LeaderElectionService leaderElectionService;
    @Mock
    private PersistedInputs persistedInputs;
    @Mock
    private ServerStatus serverStatus;
    @Mock
    private IOState<MessageInput> inputState;
    @Mock
    private Input input;

    private final NodeId nodeId = new SimpleNodeId(THIS_NODE_ID);
    private InputEventListener listener;

    @Before
    public void setUp() throws Exception {
        final EventBus eventBus = new EventBus(this.getClass().getSimpleName());
        listener = new InputEventListener(eventBus, inputLauncher, inputRegistry, inputService, nodeId, leaderElectionService, persistedInputs, serverStatus);
    }

    @Test
    public void inputCreatedDoesNothingIfInputDoesNotExist() throws Exception {
        when(inputService.find(INPUT_ID)).thenThrow(NotFoundException.class);

        listener.inputCreated(InputCreated.create(INPUT_ID));

        verifyNoMoreInteractions(inputLauncher, inputRegistry);
    }

    @Test
    public void inputCreatedStopsInputIfItIsRunning() throws Exception {
        when(inputService.find(INPUT_ID)).thenReturn(input);
        when(inputRegistry.getInputState(INPUT_ID)).thenReturn(inputState);

        listener.inputCreated(InputCreated.create(INPUT_ID));

        verify(inputRegistry, times(1)).remove(inputState);
    }

    @Test
    public void inputCreatedDoesNotStopInputIfItIsNotRunning() throws Exception {
        when(inputService.find(INPUT_ID)).thenReturn(input);
        when(inputRegistry.getInputState(INPUT_ID)).thenReturn(null);

        listener.inputCreated(InputCreated.create(INPUT_ID));

        verify(inputRegistry, never()).remove(Mockito.<IOState<MessageInput>>any());
    }

    @Test
    public void inputCreatedStartsGlobalInputOnOtherNode() throws Exception {
        when(inputService.find(INPUT_ID)).thenReturn(input);
        when(input.getNodeId()).thenReturn(OTHER_NODE_ID);
        when(input.isGlobal()).thenReturn(true);

        final MessageInput messageInput = mock(MessageInput.class);
        when(inputService.getMessageInput(input)).thenReturn(messageInput);

        listener.inputCreated(InputCreated.create(INPUT_ID));

        verify(inputLauncher, times(1)).launch(messageInput);
    }

    @Test
    public void inputCreatedDoesNotStartLocalInputOnAnyNode() throws Exception {
        when(inputService.find(INPUT_ID)).thenReturn(input);
        when(input.getNodeId()).thenReturn(OTHER_NODE_ID);
        when(input.isGlobal()).thenReturn(false);

        final MessageInput messageInput = mock(MessageInput.class);
        when(inputService.getMessageInput(input)).thenReturn(messageInput);

        listener.inputCreated(InputCreated.create(INPUT_ID));

        verify(inputLauncher, never()).launch(messageInput);
    }

    @Test
    public void inputCreatedStartsLocalInputOnLocalNode() throws Exception {
        when(inputService.find(INPUT_ID)).thenReturn(input);
        when(input.getNodeId()).thenReturn(THIS_NODE_ID);
        when(input.isGlobal()).thenReturn(false);

        final MessageInput messageInput = mock(MessageInput.class);
        when(inputService.getMessageInput(input)).thenReturn(messageInput);

        listener.inputCreated(InputCreated.create(INPUT_ID));

        verify(inputLauncher, times(1)).launch(messageInput);
    }

    @Test
    public void inputUpdatedDoesNothingIfInputDoesNotExist() throws Exception {
        when(inputService.find(INPUT_ID)).thenThrow(NotFoundException.class);

        listener.inputUpdated(InputUpdated.create(INPUT_ID));

        verifyNoMoreInteractions(inputLauncher, inputRegistry);
    }

    @Test
    public void inputUpdatedStopsInputIfItIsRunning() throws Exception {
        when(inputService.find(INPUT_ID)).thenReturn(input);
        when(inputRegistry.getInputState(INPUT_ID)).thenReturn(inputState);

        listener.inputUpdated(InputUpdated.create(INPUT_ID));

        verify(inputRegistry, times(1)).remove(inputState);
    }

    @Test
    public void inputUpdatedDoesNotStopInputIfItIsNotRunning() throws Exception {
        when(inputService.find(INPUT_ID)).thenReturn(input);
        when(inputRegistry.getInputState(INPUT_ID)).thenReturn(null);

        listener.inputUpdated(InputUpdated.create(INPUT_ID));

        verify(inputRegistry, never()).remove(Mockito.<IOState<MessageInput>>any());
    }

    @Test
    public void inputUpdateLaunchInputsInSetupMode() throws Exception {
        when(inputState.getState()).thenReturn(IOState.Type.SETUP);
        when(inputService.find(INPUT_ID)).thenReturn(input);
        when(inputRegistry.getInputState(INPUT_ID)).thenReturn(inputState);
        when(input.getNodeId()).thenReturn(OTHER_NODE_ID);
        when(input.isGlobal()).thenReturn(true);

        final MessageInput messageInput = mock(MessageInput.class);
        when(inputService.getMessageInput(input)).thenReturn(messageInput);

        listener.inputUpdated(InputUpdated.create(INPUT_ID));

        verify(inputLauncher, times(1)).launch(messageInput);
    }

    @Test
    public void inputUpdatedRestartsGlobalInputOnAnyNode() throws Exception {
        when(inputState.getState()).thenReturn(IOState.Type.RUNNING);
        when(inputService.find(INPUT_ID)).thenReturn(input);
        when(inputRegistry.getInputState(INPUT_ID)).thenReturn(inputState);
        when(input.getNodeId()).thenReturn(OTHER_NODE_ID);
        when(input.isGlobal()).thenReturn(true);

        final MessageInput messageInput = mock(MessageInput.class);
        when(inputService.getMessageInput(input)).thenReturn(messageInput);

        listener.inputUpdated(InputUpdated.create(INPUT_ID));

        verify(inputLauncher, times(1)).launch(messageInput);
    }

    @Test
    public void inputUpdatedDoesNotStartLocalInputOnOtherNode() throws Exception {
        when(inputState.getState()).thenReturn(IOState.Type.RUNNING);
        when(inputService.find(INPUT_ID)).thenReturn(input);
        when(input.getNodeId()).thenReturn(OTHER_NODE_ID);
        when(input.isGlobal()).thenReturn(false);

        final MessageInput messageInput = mock(MessageInput.class);
        when(inputService.getMessageInput(input)).thenReturn(messageInput);

        listener.inputUpdated(InputUpdated.create(INPUT_ID));

        verify(inputLauncher, never()).launch(messageInput);
    }

    @Test
    public void inputUpdatedRestartsLocalInputOnLocalNode() throws Exception {
        when(inputState.getState()).thenReturn(IOState.Type.RUNNING);
        when(inputService.find(INPUT_ID)).thenReturn(input);
        when(inputRegistry.getInputState(INPUT_ID)).thenReturn(inputState);
        when(input.getNodeId()).thenReturn(THIS_NODE_ID);
        when(input.isGlobal()).thenReturn(false);

        final MessageInput messageInput = mock(MessageInput.class);
        when(inputService.getMessageInput(input)).thenReturn(messageInput);

        listener.inputUpdated(InputUpdated.create(INPUT_ID));

        verify(inputLauncher, times(1)).launch(messageInput);
    }

    @Test
    public void inputUpdatedDoesNotStartLocalInputOnLocalNodeIfItWasNotRunning() throws Exception {
        when(inputState.getState()).thenReturn(IOState.Type.STOPPED);
        when(inputService.find(INPUT_ID)).thenReturn(input);
        when(inputRegistry.getInputState(INPUT_ID)).thenReturn(inputState);
        when(input.getNodeId()).thenReturn(THIS_NODE_ID);
        when(input.isGlobal()).thenReturn(false);

        final MessageInput messageInput = mock(MessageInput.class);
        when(inputService.getMessageInput(input)).thenReturn(messageInput);

        listener.inputUpdated(InputUpdated.create(INPUT_ID));

        verify(inputLauncher, never()).launch(messageInput);
    }

    @Test
    public void inputDeletedStopsInputIfItIsRunning() {
        when(inputState.getState()).thenReturn(IOState.Type.RUNNING);
        when(inputRegistry.getInputState(INPUT_ID)).thenReturn(inputState);

        listener.inputDeleted(InputDeleted.create(INPUT_ID));

        verify(inputRegistry, never()).remove(any(MessageInput.class));
    }

    @Test
    public void inputDeletedDoesNothingIfInputIsNotRunning() {
        when(inputState.getState()).thenReturn(null);
        when(inputRegistry.getInputState(INPUT_ID)).thenReturn(inputState);

        listener.inputDeleted(InputDeleted.create(INPUT_ID));

        verify(inputRegistry, never()).remove(any(MessageInput.class));
    }
}
