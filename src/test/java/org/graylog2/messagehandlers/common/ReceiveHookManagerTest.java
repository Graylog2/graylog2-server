/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.graylog2.messagehandlers.common;

import org.graylog2.messagehandlers.gelf.GELFMessage;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author lennart
 */
public class ReceiveHookManagerTest {

    @Before
    public void setUp() {
        MessageCounter.getInstance().reset(MessageCounter.ALL_HOSTS);
    }


    @Test
    public void testPostProcessWithHookThatIsUsingTheMesage() {
        MessagePostReceiveHookIF hook = new HostUpsertHook();
        GELFMessage message = new GELFMessage();
        ReceiveHookManager.postProcess(hook, message);
    }

    @Test
    public void testPostProcessWithHookThatIsNotUsingTheMesage() {
        MessagePostReceiveHookIF hook = new MessageCounterHook();
        ReceiveHookManager.postProcess(hook, null);

        assertEquals(1, MessageCounter.getInstance().getCount(MessageCounter.ALL_HOSTS));
    }

}