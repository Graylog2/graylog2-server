/**
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
package org.graylog2.messagehandlers.syslog;

import org.graylog2.inputs.codecs.SyslogCodec;
import org.graylog2.syslog4j.server.impl.event.structured.StructuredSyslogServerEvent;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class StructuredSyslogTest {
    // http://tools.ietf.org/rfc/rfc5424.txt
    public static String ValidStructuredMessage = "<165>1 2012-12-25T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"] BOMAn application event log entry";
    public static String ValidStructuredMultiMessage = "<165>1 2012-12-25T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"][meta sequenceId=\"1\"] BOMAn application event log entry";
    public static String ValidStructuredMultiMessageSameKey = "<165>1 2012-12-25T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventID=\"1011\"][meta iut=\"10\"] BOMAn application event log entry";
    public static String ValidStructuredNoStructValues = "<165>1 2012-12-25T22:14:15.003Z mymachine.example.com evntslog - ID47 - BOMAn application event log entry";
    public static String ValidNonStructuredMessage = "<86>Dec 24 17:05:01 nb-lkoopmann CRON[10049]: pam_unix(cron:session): session closed for user root";
    public static String MessageLookingLikeStructured = "<133>NOMA101FW01A: NetScreen device_id=NOMA101FW01A [Root]system-notification-00257(traffic): start_time=\"2011-12-23 17:33:43\" duration=0 reason=Creation";

    private StructuredSyslogServerEvent newEvent(String message) {
        return new StructuredSyslogServerEvent(message, new InetSocketAddress(514).getAddress());
    }

    @Test
    public void testExtractFields() {
        Map expected = new HashMap();
        expected.put("eventSource", "Application");
        expected.put("eventID", "1011");
        expected.put("iut", "3");

        Map result = SyslogCodec.StructuredSyslog.extractFields(newEvent(ValidStructuredMessage));
        assertEquals(result, expected);
    }

    @Test
    public void testExtractMoreFields() {
        Map expected = new HashMap();
        expected.put("eventSource", "Application");
        expected.put("eventID", "1011");
        expected.put("iut", "3");
        expected.put("sequenceId", "1");

        Map result = SyslogCodec.StructuredSyslog.extractFields(newEvent(ValidStructuredMultiMessage));
        assertEquals(result, expected);
    }

    @Test
    public void testExtractFieldsOverwriting() {
        // TODO: The current implementation does not handle two different SD-ELEMENTS with the same SD-PARAM keys very well.
        // TODO: Order is not guaranteed in the current syslog4j implementation!

        Map result = SyslogCodec.StructuredSyslog.extractFields(newEvent(ValidStructuredMultiMessageSameKey));
        assertTrue(Pattern.compile("3|10").matcher((String) result.get("iut")).matches(), "iut value is not 3 or 10!");
    }

    @Test
    public void testExtractNoFields() {
        Map result = SyslogCodec.StructuredSyslog.extractFields(newEvent(ValidStructuredNoStructValues));
        assertEquals(result, new HashMap());
    }

    @Test
    public void testExtractFieldsOfNonStructuredMessage() {
        Map result = SyslogCodec.StructuredSyslog.extractFields(newEvent(ValidNonStructuredMessage));
        assertEquals(result.size(), 0);
    }

    @Test
    public void testExtractFieldsOfAMessageThatOnlyLooksStructured() {
        Map result = SyslogCodec.StructuredSyslog.extractFields(newEvent(MessageLookingLikeStructured));
        assertEquals(result.size(), 0);
    }
}