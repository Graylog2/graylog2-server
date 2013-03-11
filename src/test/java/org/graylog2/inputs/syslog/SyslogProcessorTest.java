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

import java.net.InetAddress;
import org.graylog2.Configuration;
import org.graylog2.GraylogServerStub;
import org.graylog2.plugin.Message;
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

        processor.messageReceived(ValidNonStructuredMessage, InetAddress.getLocalHost());
        processor.messageReceived(ValidNonStructuredMessage, InetAddress.getLocalHost());

        Message lm = serverStub.lastInsertedToProcessBuffer;

        assertEquals(2, serverStub.callsToProcessBufferInserter);

        assertEquals("security/authorization", lm.getField("facility"));
        assertEquals("foo-bar", lm.getField("source"));
        assertEquals(6, lm.getField("level"));
        assertEquals(ValidNonStructuredMessage, lm.getField("full_message"));
        assertEquals(7, lm.getFields().size());
    }
    
    @Test
    public void testMessageReceivedWithNonStructuredMessageAndShortDate() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        Configuration configStub = new Configuration();
        serverStub.setConfigurationStub(configStub);
        SyslogProcessor processor = new SyslogProcessor(serverStub);

        processor.messageReceived(ValidNonStructuredMessageWithShortDate, InetAddress.getLocalHost());

        Message lm = serverStub.lastInsertedToProcessBuffer;

        assertEquals(1, serverStub.callsToProcessBufferInserter);

        assertEquals("security/authorization", lm.getField("facility"));
        assertEquals("foo-bar", lm.getField("source"));
        assertEquals(6, lm.getField("level"));
        assertEquals(ValidNonStructuredMessageWithShortDate, lm.getField("full_message"));
        assertEquals(7, lm.getFields().size());
    }

    @Test
    public void testMessageReceivedWithStructuredMessage() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        Configuration configStub = new Configuration();
        serverStub.setConfigurationStub(configStub);
        SyslogProcessor processor = new SyslogProcessor(serverStub);

        processor.messageReceived(ValidStructuredMessage, InetAddress.getLocalHost());

        Message lm = serverStub.lastInsertedToProcessBuffer;

        assertEquals(1, serverStub.callsToProcessBufferInserter);

        assertEquals("local4", lm.getField("facility"));
        assertEquals("mymachine.example.com", lm.getField("source"));
        assertEquals(5, lm.getField("level"));
        assertEquals(ValidStructuredMessage, lm.getField("full_message"));
        assertEquals("evntslog", lm.getField("application_name"));
        assertEquals("1011", lm.getField("eventID"));
        assertEquals("Application", lm.getField("eventSource"));
        assertEquals("3", lm.getField("iut"));
        assertEquals(11, lm.getFields().size());
    }
    
    @Test
    public void testMessageReceivedWithStructuredMessageThatHasOtherDateFormat() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        Configuration configStub = new Configuration();
        serverStub.setConfigurationStub(configStub);
        SyslogProcessor processor = new SyslogProcessor(serverStub);

        processor.messageReceived(ValidStructuedMessageWithDifferentDateFormat, InetAddress.getLocalHost());
        processor.messageReceived(ValidStructuedMessageWithDifferentDateFormat, InetAddress.getLocalHost());

        Message lm = serverStub.lastInsertedToProcessBuffer;

        assertEquals(2, serverStub.callsToProcessBufferInserter);

        assertEquals("local4", lm.getField("facility"));
        assertEquals("192.0.2.1", lm.getField("source"));
        assertEquals(5, lm.getField("level"));
        assertEquals(ValidStructuedMessageWithDifferentDateFormat, lm.getField("full_message"));
        assertEquals("myproc", lm.getField("application_name"));
        assertEquals("8710", lm.getField("process_id"));
        assertEquals(9, lm.getFields().size());
    }

    @Test
    public void testMessageReceivedWithInvalidMessage() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        Configuration configStub = new Configuration();
        serverStub.setConfigurationStub(configStub);
        SyslogProcessor processor = new SyslogProcessor(serverStub);

        processor.messageReceived("LOLWAT", InetAddress.getLocalHost());

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
        
        processor.messageReceived(ValidNonStructuredMessage, InetAddress.getLocalHost());

        Message lm = serverStub.lastInsertedToProcessBuffer;

        assertNull(lm.getField("full_message"));
    }

}