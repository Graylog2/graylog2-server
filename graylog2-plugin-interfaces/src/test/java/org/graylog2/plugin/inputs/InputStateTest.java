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
