/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.codecs;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.InetSocketAddress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SyslogCodecTest {
    private static final int YEAR = Tools.iso8601().getYear();
    public static String STRUCTURED = "<165>1 2012-12-25T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"] BOMAn application event log entry";
    public static String STRUCTURED_ISSUE_845 = "<190>1 2015-01-06T20:56:33.287Z app-1 app - - [mdc@18060 ip=\"::ffff:132.123.15.30\" logger=\"{c.corp.Handler}\" session=\"4ot7\" user=\"user@example.com\" user-agent=\"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/600.2.5 (KHTML, like Gecko) Version/7.1.2 Safari/537.85.11\"] User page 13 requested";
    public static String STRUCTURED_ISSUE_845_EMPTY = "<128>1 2015-01-11T16:35:21.335797+01:00 s000000.example.com - - - - tralala";
    // The folowing message from issue 549 is from a Juniper SRX 240 device.
    public static String STRUCTURED_ISSUE_549 = "<14>1 2014-05-01T08:26:51.179Z fw01 RT_FLOW - RT_FLOW_SESSION_DENY [junos@2636.1.1.1.2.39 source-address=\"1.2.3.4\" source-port=\"56639\" destination-address=\"5.6.7.8\" destination-port=\"2003\" service-name=\"None\" protocol-id=\"6\" icmp-type=\"0\" policy-name=\"log-all-else\" source-zone-name=\"campus\" destination-zone-name=\"mngmt\" application=\"UNKNOWN\" nested-application=\"UNKNOWN\" username=\"N/A\" roles=\"N/A\" packet-incoming-interface=\"reth6.0\" encrypted=\"No\"]";
    private final String UNSTRUCTURED = "<45>Oct 21 12:09:37 c4dc57ba1ebb syslog-ng[7208]: syslog-ng starting up; version='3.5.3'";

    @Mock
    private Configuration configuration;
    @Mock
    private MetricRegistry metricRegistry;
    @Mock
    private Timer mockedTimer;

    private Codec codec;

    @Before
    public void setUp() throws Exception {
        when(metricRegistry.timer(any(String.class))).thenReturn(mockedTimer);
        when(mockedTimer.time()).thenReturn(mock(Timer.Context.class));

        codec = new SyslogCodec(configuration, metricRegistry);
    }

    @Test
    public void testDecodeStructured() throws Exception {
        final Message message = codec.decode(buildRawMessage(STRUCTURED));

        assertNotNull(message);
        assertEquals(message.getMessage(), "BOMAn application event log entry");
        assertEquals(((DateTime) message.getField("timestamp")).withZone(DateTimeZone.UTC), new DateTime("2012-12-25T22:14:15.003Z", DateTimeZone.UTC));
        assertEquals(message.getField("source"), "mymachine.example.com");
        assertEquals(message.getField("level"), 5);
        assertEquals(message.getField("facility"), "local4");
        assertEquals(message.getField("eventSource"), "Application");
        assertEquals(message.getField("eventID"), "1011");
        assertEquals(message.getField("iut"), "3");
        assertEquals(message.getField("application_name"), "evntslog");
    }

    @Test
    public void testDecodeStructuredIssue845() throws Exception {
        final Message message = codec.decode(buildRawMessage(STRUCTURED_ISSUE_845));

        assertNotNull(message);
        assertEquals(message.getMessage(), "User page 13 requested");
        assertEquals(((DateTime) message.getField("timestamp")).withZone(DateTimeZone.UTC), new DateTime("2015-01-06T20:56:33.287Z", DateTimeZone.UTC));
        assertEquals(message.getField("source"), "app-1");
        assertEquals(message.getField("level"), 6);
        assertEquals(message.getField("facility"), "local7");
        assertEquals(message.getField("ip"), "::ffff:132.123.15.30");
        assertEquals(message.getField("logger"), "{c.corp.Handler}");
        assertEquals(message.getField("session"), "4ot7");
        assertEquals(message.getField("user"), "user@example.com");
        assertEquals(message.getField("user-agent"), "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/600.2.5 (KHTML, like Gecko) Version/7.1.2 Safari/537.85.11");
        assertEquals(message.getField("application_name"), "app");
    }

    @Test
    public void testDecodeStructuredIssue845WithExpandStructuredData() throws Exception {
        when(configuration.getBoolean(SyslogCodec.CK_EXPAND_STRUCTURED_DATA)).thenReturn(true);

        final SyslogCodec codec = new SyslogCodec(configuration, metricRegistry);
        final Message message = codec.decode(buildRawMessage(STRUCTURED_ISSUE_845));

        assertNotNull(message);
        assertEquals(message.getMessage(), "User page 13 requested");
        assertEquals(((DateTime) message.getField("timestamp")).withZone(DateTimeZone.UTC), new DateTime("2015-01-06T20:56:33.287Z", DateTimeZone.UTC));
        assertEquals(message.getField("source"), "app-1");
        assertEquals(message.getField("level"), 6);
        assertEquals(message.getField("facility"), "local7");
        assertEquals(message.getField("mdc@18060_ip"), "::ffff:132.123.15.30");
        assertEquals(message.getField("mdc@18060_logger"), "{c.corp.Handler}");
        assertEquals(message.getField("mdc@18060_session"), "4ot7");
        assertEquals(message.getField("mdc@18060_user"), "user@example.com");
        assertEquals(message.getField("mdc@18060_user-agent"), "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/600.2.5 (KHTML, like Gecko) Version/7.1.2 Safari/537.85.11");
        assertEquals(message.getField("application_name"), "app");
    }

    @Test
    public void testDecodeStructuredIssue845Empty() throws Exception {
        final Message message = codec.decode(buildRawMessage(STRUCTURED_ISSUE_845_EMPTY));

        assertNotNull(message);
        assertEquals(message.getMessage(), "- - tralala");
        assertEquals(((DateTime) message.getField("timestamp")).withZone(DateTimeZone.UTC), new DateTime("2015-01-11T15:35:21.335797Z", DateTimeZone.UTC));
        assertEquals(message.getField("source"), "s000000.example.com");
        assertEquals(message.getField("level"), 0);
        assertEquals(message.getField("facility"), "local0");
    }

    @Test
    public void testDecodeStructuredWithFullMessage() throws Exception {
        when(configuration.getBoolean(SyslogCodec.CK_STORE_FULL_MESSAGE)).thenReturn(true);

        final Message message = codec.decode(buildRawMessage(STRUCTURED));

        assertNotNull(message);
        assertEquals(message.getMessage(), "BOMAn application event log entry");
        assertEquals(((DateTime) message.getField("timestamp")).withZone(DateTimeZone.UTC), new DateTime("2012-12-25T22:14:15.003Z", DateTimeZone.UTC));
        assertEquals(message.getField("source"), "mymachine.example.com");
        assertEquals(message.getField("level"), 5);
        assertEquals(message.getField("facility"), "local4");
        assertEquals(message.getField("full_message"), STRUCTURED);
        assertEquals(message.getField("eventSource"), "Application");
        assertEquals(message.getField("eventID"), "1011");
        assertEquals(message.getField("iut"), "3");
        assertEquals(message.getField("application_name"), "evntslog");
    }

    @Test
    public void testDecodeStructuredIssue549() throws Exception {
        final Message message = codec.decode(buildRawMessage(STRUCTURED_ISSUE_549));

        assertNotNull(message);
        assertEquals(message.getMessage(), "RT_FLOW_SESSION_DENY [junos@2636.1.1.1.2.39 source-address=\"1.2.3.4\" source-port=\"56639\" destination-address=\"5.6.7.8\" destination-port=\"2003\" service-name=\"None\" protocol-id=\"6\" icmp-type=\"0\" policy-name=\"log-all-else\" source-zone-name=\"campus\" destination-zone-name=\"mngmt\" application=\"UNKNOWN\" nested-application=\"UNKNOWN\" username=\"N/A\" roles=\"N/A\" packet-incoming-interface=\"reth6.0\" encrypted=\"No\"]");
        assertEquals(((DateTime) message.getField("timestamp")).withZone(DateTimeZone.UTC), new DateTime("2014-05-01T08:26:51.179Z", DateTimeZone.UTC));
        assertEquals(message.getField("source-address"), "1.2.3.4");
        assertEquals(message.getField("source-port"), "56639");
        assertEquals(message.getField("destination-address"), "5.6.7.8");
        assertEquals(message.getField("destination-port"), "2003");
        assertEquals(message.getField("service-name"), "None");
        assertEquals(message.getField("protocol-id"), "6");
        assertEquals(message.getField("icmp-type"), "0");
        assertEquals(message.getField("policy-name"), "log-all-else");
        assertEquals(message.getField("source-zone-name"), "campus");
        assertEquals(message.getField("destination-zone-name"), "mngmt");
        assertEquals(message.getField("application"), "UNKNOWN");
        assertEquals(message.getField("nested-application"), "UNKNOWN");
        assertEquals(message.getField("username"), "N/A");
        assertEquals(message.getField("roles"), "N/A");
        assertEquals(message.getField("packet-incoming-interface"), "reth6.0");
        assertEquals(message.getField("encrypted"), "No");
    }

    @Test
    public void testDecodeUnstructured() throws Exception {
        final Message message = codec.decode(buildRawMessage(UNSTRUCTURED));

        assertNotNull(message);
        assertEquals(message.getMessage(), "c4dc57ba1ebb syslog-ng[7208]: syslog-ng starting up; version='3.5.3'");
        assertEquals(message.getField("timestamp"), new DateTime(YEAR + "-10-21T12:09:37"));
        assertEquals(message.getField("source"), "c4dc57ba1ebb");
        assertEquals(message.getField("level"), 5);
        assertEquals(message.getField("facility"), "syslogd");
        assertNull(message.getField("full_message"));
    }

    @Test
    public void testDecodeUnstructuredWithFullMessage() throws Exception {
        when(configuration.getBoolean(SyslogCodec.CK_STORE_FULL_MESSAGE)).thenReturn(true);

        final Message message = codec.decode(buildRawMessage(UNSTRUCTURED));

        assertNotNull(message);
        assertEquals(message.getMessage(), "c4dc57ba1ebb syslog-ng[7208]: syslog-ng starting up; version='3.5.3'");
        assertEquals(message.getField("timestamp"), new DateTime(YEAR + "-10-21T12:09:37"));
        assertEquals(message.getField("source"), "c4dc57ba1ebb");
        assertEquals(message.getField("level"), 5);
        assertEquals(message.getField("facility"), "syslogd");
        assertEquals(message.getField("full_message"), UNSTRUCTURED);
    }

    private RawMessage buildRawMessage(String message) {
        return new RawMessage(message.getBytes(), new InetSocketAddress(5140));
    }
}
