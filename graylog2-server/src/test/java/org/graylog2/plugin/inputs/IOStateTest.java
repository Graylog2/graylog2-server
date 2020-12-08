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
package org.graylog2.plugin.inputs;

import com.google.common.eventbus.EventBus;
import org.graylog2.plugin.IOState;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class IOStateTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void testNotEqualIfDifferentInput() throws Exception {
        EventBus eventBus = mock(EventBus.class);
        MessageInput messageInput1 = mock(MessageInput.class);
        MessageInput messageInput2 = mock(MessageInput.class);

        IOState<MessageInput> inputState1 = new IOState<>(eventBus, messageInput1);
        IOState<MessageInput> inputState2 = new IOState<>(eventBus, messageInput2);

        assertFalse(inputState1.equals(inputState2));
        assertFalse(inputState2.equals(inputState1));
    }

    @Test
    public void testEqualsSameState() throws Exception {
        EventBus eventBus = mock(EventBus.class);
        MessageInput messageInput = mock(MessageInput.class);

        IOState<MessageInput> inputState1 = new IOState<>(eventBus, messageInput, IOState.Type.RUNNING);
        IOState<MessageInput> inputState2 = new IOState<>(eventBus, messageInput, IOState.Type.RUNNING);

        assertTrue(inputState1.equals(inputState2));
        assertTrue(inputState2.equals(inputState1));
    }

    @Test
    public void testNotEqualIfDifferentState() throws Exception {
        EventBus eventBus = mock(EventBus.class);
        MessageInput messageInput = mock(MessageInput.class);

        IOState<MessageInput> inputState1 = new IOState<>(eventBus, messageInput, IOState.Type.RUNNING);
        IOState<MessageInput> inputState2 = new IOState<>(eventBus, messageInput, IOState.Type.STOPPED);

        assertTrue(inputState1.equals(inputState2));
        assertTrue(inputState2.equals(inputState1));
    }
}
