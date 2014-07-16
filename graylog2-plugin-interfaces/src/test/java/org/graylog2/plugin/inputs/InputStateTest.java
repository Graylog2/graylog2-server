/*
 * Copyright 2012-2014 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.plugin.inputs;

import org.testng.annotations.Test;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Test
public class InputStateTest {
    public void testAsMap() throws Exception {
        MessageInput messageInput = mock(MessageInput.class);
        InputState inputState = new InputState(messageInput);

        Map<String, Object> asMap = inputState.asMap();

        assertNotNull(asMap.get("id"));
        assertNotNull(asMap.get("state"));
        assertEquals(asMap.get("message_input"), messageInput.asMap());
    }

    public void testEqualsDifferentId() throws Exception {
        MessageInput messageInput = mock(MessageInput.class);

        InputState inputState1 = new InputState(messageInput);
        InputState inputState2 = new InputState(messageInput);

        assertFalse(inputState1.equals(inputState2));
        assertFalse(inputState2.equals(inputState1));
    }

    public void testEqualsSameId() throws Exception {
        MessageInput messageInput = mock(MessageInput.class);

        String id = UUID.randomUUID().toString();

        InputState inputState1 = new InputState(messageInput, id);
        InputState inputState2 = new InputState(messageInput, id);

        assertTrue(inputState1.equals(inputState2));
        assertTrue(inputState2.equals(inputState1));
    }
}
