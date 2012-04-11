/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.graylog2.messagehandlers.common;

import org.graylog2.LogMessage;
import org.junit.Test;

/**
 *
 * @author lennart
 */
public class HostUpsertHookTest {

    @Test
    public void testProcessWithGELFMessage() {
        LogMessage message = new LogMessage();
        //message.setHost("lolwat");
        HostUpsertHook instance = new HostUpsertHook();
        instance.process(message);
    }

}