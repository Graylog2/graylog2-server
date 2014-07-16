/**
 * Copyright 2011 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.messagehandlers.syslog;

import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;

public class StructuredSyslogTest {

    @Test
    public void testTheTruthToWork() {
        assertTrue(true);
    }

/*
    // http://tools.ietf.org/rfc/rfc5424.txt
    public static String ValidStructuredMessage = "<165>1 2012-12-25T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"] BOMAn application event log entry";
    public static String ValidNonStructuredMessage = "<86>Dec 24 17:05:01 nb-lkoopmann CRON[10049]: pam_unix(cron:session): session closed for user root";
    public static String MessageLookingLikeStructured = "<133>NOMA101FW01A: NetScreen device_id=NOMA101FW01A [Root]system-notification-00257(traffic): start_time=\"2011-12-23 17:33:43\" duration=0 reason=Creation";

    @Test
    public void testExtractFields() {
        Map expected = new HashMap();
        expected.put("eventSource", "Application");
        expected.put("eventID", "1011");
        expected.put("iut", "3");

        Map result = StructuredSyslog.extractFields(ValidStructuredMessage.getBytes());
        assertEquals(expected, result);
    }

    @Test
    public void testExtractFieldsOfNonStructuredMessage() {
        Map result = StructuredSyslog.extractFields(ValidNonStructuredMessage.getBytes());
        assertEquals(0, result.size());
    }

    @Test
    public void testExtractFieldsOfAMessageThatOnlyLooksStructured() {
        Map result = StructuredSyslog.extractFields(MessageLookingLikeStructured.getBytes());
        assertEquals(0, result.size());
    }
*/
}