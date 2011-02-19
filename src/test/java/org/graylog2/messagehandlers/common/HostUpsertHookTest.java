/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.graylog2.messagehandlers.common;

import org.graylog2.messagehandlers.gelf.GELFMessage;
import org.junit.Test;
import org.productivity.java.syslog4j.server.impl.event.SyslogServerEvent;

/**
 *
 * @author lennart
 */
public class HostUpsertHookTest {

    @Test
    public void testProcessWithGELFMessage() {
        GELFMessage message = new GELFMessage();
        HostUpsertHook instance = new HostUpsertHook();
        instance.process(message);
    }

/*    @Test
    public void testProcessWithSyslogMessage() {
        SyslogServerEvent message = new SyslogServerEvent("foo".getBytes(), "foo".getBytes().length, null);
        HostUpsertHook instance = new HostUpsertHook();
        instance.process(message);
    }
*/
}