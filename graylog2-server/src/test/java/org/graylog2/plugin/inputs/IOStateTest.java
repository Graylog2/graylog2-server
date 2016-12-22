/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
