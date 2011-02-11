/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.graylog2.messagehandlers.common;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author lennart
 */
public class MessageCounterHookTest {

    @Before
    public void setUp() {
        MessageCounter.getInstance().reset(MessageCounter.ALL_HOSTS);
    }

    /**
     * Test of process method, of class MessageCounterHook.
     */
    @Test
    public void testProcess() {
        Object message = null;

        int count_to = 12;
        for (int i = 0; i < count_to; i++) {
            MessageCounterHook instance = new MessageCounterHook();
            instance.process(message);
        }
        
        assertEquals(count_to, MessageCounter.getInstance().getCount(MessageCounter.ALL_HOSTS));
    }

}