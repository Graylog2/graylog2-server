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

import org.graylog2.plugin.IOState;
import org.graylog2.plugin.inputs.MessageInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InputRegistryTest {

    private InputRegistry inputRegistry;

    @BeforeEach
    void setUp() {
        inputRegistry = new InputRegistry();
    }

    @Test
    void testThreadSafe() {
        IntStream.range(0, 100).parallel().forEach(i -> {
            if (i % 2 == 0) {
                var ignored = inputRegistry.stream().toList();
            } else {
                inputRegistry.add(mockIOState("input-" + i, IOState.Type.CREATED));
            }
        });
    }

    @Test
    void addAndGetInputState() {
        IOState<MessageInput> state = mockIOState("input-1", IOState.Type.RUNNING);
        assertTrue(inputRegistry.add(state));

        assertNotNull(inputRegistry.getInputState("input-1"));
        assertNull(inputRegistry.getInputState("nonexistent"));
    }

    @Test
    void addDuplicateReturnsFalse() {
        IOState<MessageInput> state1 = mockIOState("input-1", IOState.Type.RUNNING);
        IOState<MessageInput> state2 = mockIOState("input-1", IOState.Type.FAILED);

        assertTrue(inputRegistry.add(state1));
        assertFalse(inputRegistry.add(state2));
    }

    @Test
    void getInputStatesReturnsAll() {
        inputRegistry.add(mockIOState("input-1", IOState.Type.RUNNING));
        inputRegistry.add(mockIOState("input-2", IOState.Type.FAILED));
        inputRegistry.add(mockIOState("input-3", IOState.Type.RUNNING));

        Set<IOState<MessageInput>> states = inputRegistry.getInputStates();
        assertEquals(3, states.size());
    }

    @Test
    void getRunningInputs() {
        inputRegistry.add(mockIOState("input-1", IOState.Type.RUNNING));
        inputRegistry.add(mockIOState("input-2", IOState.Type.FAILED));
        inputRegistry.add(mockIOState("input-3", IOState.Type.RUNNING));

        Set<IOState<MessageInput>> running = inputRegistry.getRunningInputs();
        assertEquals(2, running.size());
    }

    @Test
    void runningCount() {
        inputRegistry.add(mockIOState("input-1", IOState.Type.RUNNING));
        inputRegistry.add(mockIOState("input-2", IOState.Type.FAILED));
        inputRegistry.add(mockIOState("input-3", IOState.Type.RUNNING));

        assertEquals(2, inputRegistry.runningCount());
    }

    @Test
    void streamReturnsAllStates() {
        inputRegistry.add(mockIOState("input-1", IOState.Type.RUNNING));
        inputRegistry.add(mockIOState("input-2", IOState.Type.FAILED));

        assertEquals(2, inputRegistry.stream().count());
    }

    @SuppressWarnings("unchecked")
    private IOState<MessageInput> mockIOState(String inputId, IOState.Type state) {
        IOState<MessageInput> ioState = mock(IOState.class);
        MessageInput messageInput = mock(MessageInput.class);
        when(messageInput.getId()).thenReturn(inputId);
        when(ioState.getStoppable()).thenReturn(messageInput);
        lenient().when(ioState.getState()).thenReturn(state);
        return ioState;
    }
}
