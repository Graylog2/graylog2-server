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

import org.graylog2.plugin.Tools;
import java.net.InetAddress;
import org.graylog2.Configuration;
import org.graylog2.GraylogServerStub;
import org.graylog2.plugin.logmessage.LogMessage;
import org.junit.Test;
import static org.junit.Assert.*;

public class SyslogProcessorTest {

    public static String ValidStructuredMessage = "<165>1 2012-12-25T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"] BOMAn application event log entry";
    public static String ValidStructuedMessageWithDifferentDateFormat = "<165>1 2003-08-24T05:14:15.000003-07:00 192.0.2.1 myproc 8710 - - %% It's time to make the do-nuts";
    public static String ValidNonStructuredMessage = "<86>Dec 24 17:05:01 foo-bar CRON[10049]: pam_unix(cron:session): session closed for user root";    
    public static String MessageLookingLikeStructured = "<133>NOMA101FW01A: NetScreen device_id=NOMA101FW01A [Root]system-notification-00257(traffic): start_time=\"2011-12-23 17:33:43\" duration=0 reason=Creation";

    // http://jira.graylog2.org/browse/SERVER-287
    public static String ValidNonStructuredMessageWithShortDate = "<38>Feb 5 10:18:12 foo-bar sshd[593115]: Accepted publickey for root from 94.XXX.XXX.XXX port 5992 ssh2";
    
    @Test
    public void testMessageReceivedWithNonStructuredMessage() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        Configuration configStub = new Configuration();
        serverStub.setConfigurationStub(configStub);
        SyslogProcessor processor = new SyslogProcessor(serverStub);

        processor.messageReceived(ValidNonStructuredMessage, InetAddress.getLocalHost(), true);
        processor.messageReceived(ValidNonStructuredMessage, InetAddress.getLocalHost(), true);

        LogMessage lm = serverStub.lastInsertedToProcessBuffer;

        assertEquals(2, serverStub.callsToProcessBufferInserter);

        assertEquals("security/authorization", lm.getFacility());
        assertEquals("foo-bar", lm.getHost());
        assertEquals(6, lm.getLevel());
        assertEquals(ValidNonStructuredMessage, lm.getFullMessage());
        assertEquals(0, lm.getAdditionalData().size());
    }
    
    @Test
    public void testMessageReceivedWithNonStructuredMessageAndShortDate() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        Configuration configStub = new Configuration();
        serverStub.setConfigurationStub(configStub);
        SyslogProcessor processor = new SyslogProcessor(serverStub);

        processor.messageReceived(ValidNonStructuredMessageWithShortDate, InetAddress.getLocalHost(), true);

        LogMessage lm = serverStub.lastInsertedToProcessBuffer;

        assertEquals(1, serverStub.callsToProcessBufferInserter);

        assertEquals("security/authorization", lm.getFacility());
        assertEquals("foo-bar", lm.getHost());
        assertEquals(6, lm.getLevel());
        assertEquals(ValidNonStructuredMessageWithShortDate, lm.getFullMessage());
        assertEquals(0, lm.getAdditionalData().size());
    }

    @Test
    public void testMessageReceivedWithStructuredMessage() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        Configuration configStub = new Configuration();
        serverStub.setConfigurationStub(configStub);
        SyslogProcessor processor = new SyslogProcessor(serverStub);

        processor.messageReceived(ValidStructuredMessage, InetAddress.getLocalHost(), true);

        LogMessage lm = serverStub.lastInsertedToProcessBuffer;

        assertEquals(1, serverStub.callsToProcessBufferInserter);

        assertEquals("local4", lm.getFacility());
        assertEquals("mymachine.example.com", lm.getHost());
        assertEquals(5, lm.getLevel());
        assertEquals(ValidStructuredMessage, lm.getFullMessage());
        assertEquals("evntslog", lm.getAdditionalData().get("_application_name"));
        assertEquals("1011", lm.getAdditionalData().get("_eventID"));
        assertEquals("Application", lm.getAdditionalData().get("_eventSource"));
        assertEquals("3", lm.getAdditionalData().get("_iut"));
        assertEquals(4, lm.getAdditionalData().size());
    }
    
    @Test
    public void testMessageReceivedWithStructuredMessageThatHasOtherDateFormat() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        Configuration configStub = new Configuration();
        serverStub.setConfigurationStub(configStub);
        SyslogProcessor processor = new SyslogProcessor(serverStub);

        processor.messageReceived(ValidStructuedMessageWithDifferentDateFormat, InetAddress.getLocalHost(), true);
        processor.messageReceived(ValidStructuedMessageWithDifferentDateFormat, InetAddress.getLocalHost(), true);

        LogMessage lm = serverStub.lastInsertedToProcessBuffer;

        assertEquals(2, serverStub.callsToProcessBufferInserter);

        assertEquals("local4", lm.getFacility());
        assertEquals("192.0.2.1", lm.getHost());
        assertEquals(5, lm.getLevel());
        assertEquals(ValidStructuedMessageWithDifferentDateFormat, lm.getFullMessage());
        assertEquals("myproc", lm.getAdditionalData().get("_application_name"));
        assertEquals("8710", lm.getAdditionalData().get("_process_id"));
        assertEquals(2, lm.getAdditionalData().size());
    }

    @Test
    public void testMessageReceivedWithInvalidMessage() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        Configuration configStub = new Configuration();
        serverStub.setConfigurationStub(configStub);
        SyslogProcessor processor = new SyslogProcessor(serverStub);

        processor.messageReceived("LOLWAT", InetAddress.getLocalHost(), true);

        // Message is not inserted to process buffer.
        assertEquals(0, serverStub.callsToProcessBufferInserter);
        assertNull(serverStub.lastInsertedToProcessBuffer);
    }
    
    @Test
    public void testFullMessageIsNotStoredIfConfigured() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        Configuration configStub = new Configuration();
        configStub.setForceSyslogRdns(true);
        configStub.setISyslogStoreFullMessageEnabled(false);
        serverStub.setConfigurationStub(configStub);
        SyslogProcessor processor = new SyslogProcessor(serverStub);
        
        processor.messageReceived(ValidNonStructuredMessage, InetAddress.getLocalHost(), true);

        LogMessage lm = serverStub.lastInsertedToProcessBuffer;

        assertNull(lm.getFullMessage());
    }

}