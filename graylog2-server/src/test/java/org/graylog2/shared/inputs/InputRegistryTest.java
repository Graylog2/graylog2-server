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
package org.graylog2.shared.inputs;

import com.google.common.eventbus.EventBus;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.inputs.MessageInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InputRegistryTest {

    private EventBus eventBus;
    private InputRegistry inputRegistry;

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        inputRegistry = new InputRegistry(eventBus);
    }

    @Test
    void testThreadSafe() {
        // test which was failing pretty reliably back in the days when the input registry was not threadsafe
        IntStream.range(0, 100).parallel().forEach(i -> {
            if (i % 2 == 0) {
                var ignored = inputRegistry.stream().toList();
            } else {
                //noinspection unchecked
                inputRegistry.add(mockIOState("input-" + i, IOState.Type.CREATED));
            }
        });
    }

    @Test
    void addIndexesCurrentState() {
        IOState<MessageInput> state = mockIOState("input-1", IOState.Type.RUNNING);
        inputRegistry.add(state);

        Map<String, String> statuses = inputRegistry.getStatusesByInputId();
        assertEquals("RUNNING", statuses.get("input-1"));
        assertEquals(Set.of("input-1"), inputRegistry.getInputIdsByState(IOState.Type.RUNNING));
    }

    @Test
    void stateChangeUpdatesIndex() {
        IOState<MessageInput> state = new IOState<>(eventBus, mockMessageInput("input-1"), IOState.Type.CREATED);
        inputRegistry.add(state);

        assertEquals(Set.of("input-1"), inputRegistry.getInputIdsByState(IOState.Type.CREATED));
        assertTrue(inputRegistry.getInputIdsByState(IOState.Type.RUNNING).isEmpty());

        state.setState(IOState.Type.RUNNING);

        assertEquals(Set.of("input-1"), inputRegistry.getInputIdsByState(IOState.Type.RUNNING));
        assertTrue(inputRegistry.getInputIdsByState(IOState.Type.CREATED).isEmpty());
    }

    @Test
    void multipleStateTransitionsTrackedCorrectly() {
        IOState<MessageInput> state = new IOState<>(eventBus, mockMessageInput("input-1"), IOState.Type.CREATED);
        inputRegistry.add(state);

        state.setState(IOState.Type.STARTING);
        state.setState(IOState.Type.RUNNING);
        state.setState(IOState.Type.STOPPING);
        state.setState(IOState.Type.STOPPED);

        Map<String, String> statuses = inputRegistry.getStatusesByInputId();
        assertEquals("STOPPED", statuses.get("input-1"));
        assertEquals(Set.of("input-1"), inputRegistry.getInputIdsByState(IOState.Type.STOPPED));
        assertTrue(inputRegistry.getInputIdsByState(IOState.Type.RUNNING).isEmpty());
        assertTrue(inputRegistry.getInputIdsByState(IOState.Type.CREATED).isEmpty());
    }

    @Test
    void getStatusesByInputIdReturnsAllInputs() {
        inputRegistry.add(mockIOState("input-1", IOState.Type.RUNNING));
        inputRegistry.add(mockIOState("input-2", IOState.Type.FAILED));
        inputRegistry.add(mockIOState("input-3", IOState.Type.RUNNING));

        Map<String, String> statuses = inputRegistry.getStatusesByInputId();
        assertEquals(3, statuses.size());
        assertEquals("RUNNING", statuses.get("input-1"));
        assertEquals("FAILED", statuses.get("input-2"));
        assertEquals("RUNNING", statuses.get("input-3"));
    }

    @Test
    void getInputIdsByStateReturnsEmptyForUnknownState() {
        assertTrue(inputRegistry.getInputIdsByState(IOState.Type.RUNNING).isEmpty());
    }

    @Test
    void stateChangeForNonRegisteredInputIsIgnored() {
        IOState<MessageInput> state = new IOState<>(eventBus, mockMessageInput("outside-input"), IOState.Type.CREATED);
        state.setState(IOState.Type.RUNNING);

        assertTrue(inputRegistry.getStatusesByInputId().isEmpty());
        assertTrue(inputRegistry.getInputIdsByState(IOState.Type.RUNNING).isEmpty());
    }

    @SuppressWarnings("unchecked")
    private IOState<MessageInput> mockIOState(String inputId, IOState.Type state) {
        IOState<MessageInput> ioState = mock(IOState.class);
        MessageInput messageInput = mock(MessageInput.class);
        when(messageInput.getId()).thenReturn(inputId);
        when(ioState.getStoppable()).thenReturn(messageInput);
        when(ioState.getState()).thenReturn(state);
        return ioState;
    }

    private MessageInput mockMessageInput(String inputId) {
        MessageInput messageInput = mock(MessageInput.class);
        when(messageInput.getId()).thenReturn(inputId);
        when(messageInput.getPersistId()).thenReturn(inputId);
        return messageInput;
    }
}
