/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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
 *
 */

package org.graylog2.inputs.syslog;

import org.graylog2.Tools;
import org.graylog2.logmessage.LogMessage;
import java.net.InetAddress;
import org.graylog2.Configuration;
import org.graylog2.GraylogServerStub;
import org.junit.Test;
import static org.junit.Assert.*;

public class SyslogProcessorTest {

    // http://tools.ietf.org/rfc/rfc5424.txt
    public static String ValidStructuredMessage = "<165>1 2012-12-25T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"] BOMAn application event log entry";
    public static String ValidNonStructuredMessage = "<86>Dec 24 17:05:01 foo-bar CRON[10049]: pam_unix(cron:session): session closed for user root";
    public static String MessageLookingLikeStructured = "<133>NOMA101FW01A: NetScreen device_id=NOMA101FW01A [Root]system-notification-00257(traffic): start_time=\"2011-12-23 17:33:43\" duration=0 reason=Creation";

    @Test
    public void testMessageReceivedWithNonStructuredMessage() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        Configuration configStub = new Configuration();
        configStub.setForceSyslogRdns(false);
        serverStub.setConfigurationStub(configStub);
        SyslogProcessor processor = new SyslogProcessor(serverStub);

        processor.messageReceived(ValidNonStructuredMessage, InetAddress.getLocalHost());
        processor.messageReceived(ValidNonStructuredMessage, InetAddress.getLocalHost());

        LogMessage lm = serverStub.lastInsertedToProcessBuffer;

        assertEquals(2, serverStub.callsToProcessBufferInserter);

        assertEquals("Dec 24 17:05:01 foo-bar CRON[10049]: pam_unix(cron:session): session closed for user root", lm.getShortMessage());
        assertEquals(InetAddress.getLocalHost().getHostName(), lm.getHost());
        assertEquals("security/authorization", lm.getFacility());
        assertEquals(6, lm.getLevel());
        assertEquals(ValidNonStructuredMessage, lm.getFullMessage());
        assertEquals((int) Tools.getUTCTimestampWithMilliseconds(), (int) lm.getCreatedAt());
        assertEquals(0, lm.getAdditionalData().size());
    }

    @Test
    public void testMessageReceivedWithStructuredMessage() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        Configuration configStub = new Configuration();
        configStub.setForceSyslogRdns(false);
        serverStub.setConfigurationStub(configStub);
        SyslogProcessor processor = new SyslogProcessor(serverStub);

        processor.messageReceived(ValidStructuredMessage, InetAddress.getLocalHost());
        processor.messageReceived(ValidStructuredMessage, InetAddress.getLocalHost());

        LogMessage lm = serverStub.lastInsertedToProcessBuffer;

        assertEquals(2, serverStub.callsToProcessBufferInserter);

        assertEquals("1 2012-12-25T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"] BOMAn application event log entry",
                lm.getShortMessage());
        assertEquals(InetAddress.getLocalHost().getHostName(), lm.getHost());
        assertEquals("local4", lm.getFacility());
        assertEquals(5, lm.getLevel());
        assertEquals(ValidStructuredMessage, lm.getFullMessage());
        assertEquals((int) Tools.getUTCTimestampWithMilliseconds(), (int) lm.getCreatedAt());
        assertEquals(3, lm.getAdditionalData().size());
    }

    @Test
    public void testMessageReceivedWithInvalidMessage() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        Configuration configStub = new Configuration();
        configStub.setForceSyslogRdns(false);
        serverStub.setConfigurationStub(configStub);
        SyslogProcessor processor = new SyslogProcessor(serverStub);

        processor.messageReceived("LOLWAT", InetAddress.getLocalHost());

        // Message is not inserted to process buffer.
        assertEquals(0, serverStub.callsToProcessBufferInserter);
        assertNull(serverStub.lastInsertedToProcessBuffer);
    }

}