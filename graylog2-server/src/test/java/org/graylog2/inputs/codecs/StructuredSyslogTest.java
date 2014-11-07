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
package org.graylog2.inputs.codecs;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.syslog4j.server.impl.event.structured.StructuredSyslogServerEvent;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class StructuredSyslogTest {
    // http://tools.ietf.org/rfc/rfc5424.txt
    private static final String ValidStructuredMessage = "<165>1 2012-12-25T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"] BOMAn application event log entry";
    private static final String ValidStructuredMultiMessage = "<165>1 2012-12-25T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"][meta sequenceId=\"1\"] BOMAn application event log entry";
    private static final String ValidStructuredMultiMessageSameKey = "<165>1 2012-12-25T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventID=\"1011\"][meta iut=\"10\"] BOMAn application event log entry";
    private static final String ValidStructuredNoStructValues = "<165>1 2012-12-25T22:14:15.003Z mymachine.example.com evntslog - ID47 - BOMAn application event log entry";
    private static final String ValidNonStructuredMessage = "<86>Dec 24 17:05:01 nb-lkoopmann CRON[10049]: pam_unix(cron:session): session closed for user root";
    private static final String MessageLookingLikeStructured = "<133>NOMA101FW01A: NetScreen device_id=NOMA101FW01A [Root]system-notification-00257(traffic): start_time=\"2011-12-23 17:33:43\" duration=0 reason=Creation";

    private final Configuration configuration = new Configuration(ImmutableMap.<String, Object>of(
            SyslogCodec.CK_FORCE_RDNS, false,
            SyslogCodec.CK_ALLOW_OVERRIDE_DATE, false,
            SyslogCodec.CK_EXPAND_STRUCTURED_DATA, true,
            SyslogCodec.CK_STORE_FULL_MESSAGE, true
    ));
    private SyslogCodec syslogCodec;
    @Mock private MetricRegistry metricRegistry;
    @Mock private Timer mockedTimer;

    @BeforeTest
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(metricRegistry.timer(any(String.class))).thenReturn(mockedTimer);
        when(mockedTimer.time()).thenReturn(mock(Timer.Context.class));

        syslogCodec = new SyslogCodec(configuration, metricRegistry);
    }

    private StructuredSyslogServerEvent newEvent(String message) {
        return new StructuredSyslogServerEvent(message, new InetSocketAddress(514).getAddress());
    }

    @Test
    public void testExtractFields() {
        Map<String, Object> expected = new HashMap<>();
        expected.put("eventSource", "Application");
        expected.put("eventID", "1011");
        expected.put("iut", "3");

        Map<String, Object> result = syslogCodec.extractFields(newEvent(ValidStructuredMessage), false);
        assertEquals(result, expected);
    }

    @Test
    public void testExtractMoreFields() {
        Map<String, Object> expected = new HashMap<>();
        expected.put("eventSource", "Application");
        expected.put("eventID", "1011");
        expected.put("iut", "3");
        expected.put("sequenceId", "1");

        Map<String, Object> result = syslogCodec.extractFields(newEvent(ValidStructuredMultiMessage), false);
        assertEquals(result, expected);
    }

    @Test
    public void testExtractFieldsWithoutExpansion() {
        // Order is not guaranteed in the current syslog4j implementation!
        Map<String, Object> result = syslogCodec.extractFields(newEvent(ValidStructuredMultiMessageSameKey), false);
        assertTrue(Pattern.compile("3|10").matcher((String) result.get("iut")).matches(), "iut value is not 3 or 10!");
    }

    @Test
    public void testExtractFieldsWithExpansion() {
        Map<String, Object> result = syslogCodec.extractFields(newEvent(ValidStructuredMultiMessageSameKey), true);
        assertEquals(result.get("exampleSDID@32473_iut"), "3");
        assertEquals(result.get("meta_iut"), "10");
    }

    @Test
    public void testExtractNoFields() {
        Map<String, Object> result = syslogCodec.extractFields(newEvent(ValidStructuredNoStructValues), false);
        assertEquals(result, Collections.emptyMap());
    }

    @Test
    public void testExtractFieldsOfNonStructuredMessage() {
        Map<String, Object> result = syslogCodec.extractFields(newEvent(ValidNonStructuredMessage), false);
        assertEquals(result.size(), 0);
    }

    @Test
    public void testExtractFieldsOfAMessageThatOnlyLooksStructured() {
        Map<String, Object> result = syslogCodec.extractFields(newEvent(MessageLookingLikeStructured), false);
        assertEquals(result.size(), 0);
    }
}