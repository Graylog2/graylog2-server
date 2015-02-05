/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin.inputs;

import com.google.common.eventbus.EventBus;
import org.graylog2.plugin.IOState;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Test
public class IOStateTest {
    public void testNotEqualIfDifferentInput() throws Exception {
        EventBus eventBus = mock(EventBus.class);
        MessageInput messageInput1 = mock(MessageInput.class);
        MessageInput messageInput2 = mock(MessageInput.class);

        IOState inputState1 = new IOState<MessageInput>(eventBus, messageInput1);
        IOState inputState2 = new IOState<MessageInput>(eventBus, messageInput2);

        assertFalse(inputState1.equals(inputState2));
        assertFalse(inputState2.equals(inputState1));
    }

    public void testEqualsSameState() throws Exception {
        EventBus eventBus = mock(EventBus.class);
        MessageInput messageInput = mock(MessageInput.class);

        IOState inputState1 = new IOState<MessageInput>(eventBus, messageInput, IOState.Type.RUNNING);
        IOState inputState2 = new IOState<MessageInput>(eventBus, messageInput, IOState.Type.RUNNING);

        assertTrue(inputState1.equals(inputState2));
        assertTrue(inputState2.equals(inputState1));
    }

    public void testNotEqualIfDifferentState() throws Exception {
        EventBus eventBus = mock(EventBus.class);
        MessageInput messageInput = mock(MessageInput.class);

        IOState inputState1 = new IOState<MessageInput>(eventBus, messageInput, IOState.Type.RUNNING);
        IOState inputState2 = new IOState<MessageInput>(eventBus, messageInput, IOState.Type.STOPPED);

        assertTrue(inputState1.equals(inputState2));
        assertTrue(inputState2.equals(inputState1));
    }
}
