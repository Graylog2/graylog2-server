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
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.rest.models.system.inputs.responses.InputCreated;
import org.graylog2.rest.models.system.inputs.responses.InputDeleted;
import org.graylog2.rest.models.system.inputs.responses.InputUpdated;
import org.graylog2.shared.inputs.InputLauncher;
import org.graylog2.shared.inputs.InputRegistry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class InputEventListenerTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private InputLauncher inputLauncher;
    @Mock
    private InputRegistry inputRegistry;
    @Mock
    private InputService inputService;
    @Mock
    private NodeId nodeId;
    private InputEventListener listener;

    @Before
    public void setUp() throws Exception {
        final EventBus eventBus = new EventBus(this.getClass().getSimpleName());
        listener = new InputEventListener(eventBus, inputLauncher, inputRegistry, inputService, nodeId);
    }

    @Test
    public void inputCreatedDoesNothingIfInputDoesNotExist() throws Exception {
        final String inputId = "input-id";
        when(inputService.find(inputId)).thenThrow(NotFoundException.class);

        listener.inputCreated(InputCreated.create(inputId));

        verifyZeroInteractions(inputLauncher, inputRegistry, nodeId);
    }

    @Test
    public void inputCreatedStopsInputIfItIsRunning() throws Exception {
        final String inputId = "input-id";
        final Input input = mock(Input.class);
        @SuppressWarnings("unchecked") final IOState<MessageInput> inputState = mock(IOState.class);
        when(inputService.find(inputId)).thenReturn(input);
        when(inputRegistry.getInputState(inputId)).thenReturn(inputState);

        listener.inputCreated(InputCreated.create(inputId));

        verify(inputRegistry, times(1)).remove(inputState);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void inputCreatedDoesNotStopInputIfItIsNotRunning() throws Exception {
        final String inputId = "input-id";
        final Input input = mock(Input.class);
        when(inputService.find(inputId)).thenReturn(input);
        when(inputRegistry.getInputState(inputId)).thenReturn(null);

        listener.inputCreated(InputCreated.create(inputId));

        verify(inputRegistry, never()).remove(any(IOState.class));
    }

    @Test
    public void inputCreatedStartsGlobalInputOnOtherNode() throws Exception {
        final String inputId = "input-id";
        final Input input = mock(Input.class);
        when(inputService.find(inputId)).thenReturn(input);
        when(nodeId.toString()).thenReturn("node-id");
        when(input.getNodeId()).thenReturn("other-node-id");
        when(input.isGlobal()).thenReturn(true);

        final MessageInput messageInput = mock(MessageInput.class);
        when(inputService.getMessageInput(input)).thenReturn(messageInput);

        listener.inputCreated(InputCreated.create(inputId));

        verify(inputLauncher, times(1)).launch(messageInput);
    }

    @Test
    public void inputCreatedDoesNotStartLocalInputOnAnyNode() throws Exception {
        final String inputId = "input-id";
        final Input input = mock(Input.class);
        when(inputService.find(inputId)).thenReturn(input);
        when(nodeId.toString()).thenReturn("node-id");
        when(input.getNodeId()).thenReturn("other-node-id");
        when(input.isGlobal()).thenReturn(false);

        final MessageInput messageInput = mock(MessageInput.class);
        when(inputService.getMessageInput(input)).thenReturn(messageInput);

        listener.inputCreated(InputCreated.create(inputId));

        verify(inputLauncher, never()).launch(messageInput);
    }

    @Test
    public void inputCreatedStartsLocalInputOnLocalNode() throws Exception {
        final String inputId = "input-id";
        final Input input = mock(Input.class);
        when(inputService.find(inputId)).thenReturn(input);
        when(nodeId.toString()).thenReturn("node-id");
        when(input.getNodeId()).thenReturn("node-id");
        when(input.isGlobal()).thenReturn(false);

        final MessageInput messageInput = mock(MessageInput.class);
        when(inputService.getMessageInput(input)).thenReturn(messageInput);

        listener.inputCreated(InputCreated.create(inputId));

        verify(inputLauncher, times(1)).launch(messageInput);
    }

    @Test
    public void inputUpdatedDoesNothingIfInputDoesNotExist() throws Exception {
        final String inputId = "input-id";
        when(inputService.find(inputId)).thenThrow(NotFoundException.class);

        listener.inputUpdated(InputUpdated.create(inputId));

        verifyZeroInteractions(inputLauncher, inputRegistry, nodeId);
    }

    @Test
    public void inputUpdatedStopsInputIfItIsRunning() throws Exception {
        final String inputId = "input-id";
        final Input input = mock(Input.class);
        @SuppressWarnings("unchecked") final IOState<MessageInput> inputState = mock(IOState.class);
        when(inputService.find(inputId)).thenReturn(input);
        when(inputRegistry.getInputState(inputId)).thenReturn(inputState);

        listener.inputUpdated(InputUpdated.create(inputId));

        verify(inputRegistry, times(1)).remove(inputState);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void inputUpdatedDoesNotStopInputIfItIsNotRunning() throws Exception {
        final String inputId = "input-id";
        final Input input = mock(Input.class);
        when(inputService.find(inputId)).thenReturn(input);
        when(inputRegistry.getInputState(inputId)).thenReturn(null);

        listener.inputUpdated(InputUpdated.create(inputId));

        verify(inputRegistry, never()).remove(any(IOState.class));
    }

    @Test
    public void inputUpdatedRestartsGlobalInputOnAnyNode() throws Exception {
        final String inputId = "input-id";
        final Input input = mock(Input.class);
        @SuppressWarnings("unchecked") final IOState<MessageInput> inputState = mock(IOState.class);
        when(inputState.getState()).thenReturn(IOState.Type.RUNNING);
        when(inputService.find(inputId)).thenReturn(input);
        when(inputRegistry.getInputState(inputId)).thenReturn(inputState);
        when(nodeId.toString()).thenReturn("node-id");
        when(input.getNodeId()).thenReturn("other-node-id");
        when(input.isGlobal()).thenReturn(true);

        final MessageInput messageInput = mock(MessageInput.class);
        when(inputService.getMessageInput(input)).thenReturn(messageInput);

        listener.inputUpdated(InputUpdated.create(inputId));

        verify(inputLauncher, times(1)).launch(messageInput);
    }

    @Test
    public void inputUpdatedDoesNotStartLocalInputOnOtherNode() throws Exception {
        final String inputId = "input-id";
        final Input input = mock(Input.class);
        @SuppressWarnings("unchecked") final IOState<MessageInput> inputState = mock(IOState.class);
        when(inputState.getState()).thenReturn(IOState.Type.RUNNING);
        when(inputService.find(inputId)).thenReturn(input);
        when(nodeId.toString()).thenReturn("node-id");
        when(input.getNodeId()).thenReturn("other-node-id");
        when(input.isGlobal()).thenReturn(false);

        final MessageInput messageInput = mock(MessageInput.class);
        when(inputService.getMessageInput(input)).thenReturn(messageInput);

        listener.inputUpdated(InputUpdated.create(inputId));

        verify(inputLauncher, never()).launch(messageInput);
    }

    @Test
    public void inputUpdatedRestartsLocalInputOnLocalNode() throws Exception {
        final String inputId = "input-id";
        final Input input = mock(Input.class);
        @SuppressWarnings("unchecked") final IOState<MessageInput> inputState = mock(IOState.class);
        when(inputState.getState()).thenReturn(IOState.Type.RUNNING);
        when(inputService.find(inputId)).thenReturn(input);
        when(inputRegistry.getInputState(inputId)).thenReturn(inputState);
        when(nodeId.toString()).thenReturn("node-id");
        when(input.getNodeId()).thenReturn("node-id");
        when(input.isGlobal()).thenReturn(false);

        final MessageInput messageInput = mock(MessageInput.class);
        when(inputService.getMessageInput(input)).thenReturn(messageInput);

        listener.inputUpdated(InputUpdated.create(inputId));

        verify(inputLauncher, times(1)).launch(messageInput);
    }

    @Test
    public void inputUpdatedDoesNotStartLocalInputOnLocalNodeIfItWasNotRunning() throws Exception {
        final String inputId = "input-id";
        final Input input = mock(Input.class);
        @SuppressWarnings("unchecked") final IOState<MessageInput> inputState = mock(IOState.class);
        when(inputState.getState()).thenReturn(IOState.Type.STOPPED);
        when(inputService.find(inputId)).thenReturn(input);
        when(inputRegistry.getInputState(inputId)).thenReturn(inputState);
        when(nodeId.toString()).thenReturn("node-id");
        when(input.getNodeId()).thenReturn("node-id");
        when(input.isGlobal()).thenReturn(false);

        final MessageInput messageInput = mock(MessageInput.class);
        when(inputService.getMessageInput(input)).thenReturn(messageInput);

        listener.inputUpdated(InputUpdated.create(inputId));

        verify(inputLauncher, never()).launch(messageInput);
    }

    @Test
    public void inputDeletedStopsInputIfItIsRunning() throws Exception {
        final String inputId = "input-id";
        @SuppressWarnings("unchecked") final IOState<MessageInput> inputState = mock(IOState.class);
        when(inputState.getState()).thenReturn(IOState.Type.RUNNING);
        when(inputRegistry.getInputState(inputId)).thenReturn(inputState);

        listener.inputDeleted(InputDeleted.create(inputId));

        verify(inputRegistry, never()).remove(any(MessageInput.class));
    }

    @Test
    public void inputDeletedDoesNothingIfInputIsNotRunning() throws Exception {
        final String inputId = "input-id";
        @SuppressWarnings("unchecked") final IOState<MessageInput> inputState = mock(IOState.class);
        when(inputState.getState()).thenReturn(null);
        when(inputRegistry.getInputState(inputId)).thenReturn(inputState);

        listener.inputDeleted(InputDeleted.create(inputId));

        verify(inputRegistry, never()).remove(any(MessageInput.class));
    }
}