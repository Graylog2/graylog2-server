/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.inputs.codecs;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.SuppressForbidden;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SyslogCodecTest {
    private static final int YEAR = Tools.nowUTC().getYear();
    private static String STRUCTURED = "<165>1 2012-12-25T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"] BOMAn application event log entry";
    private static String STRUCTURED_ISSUE_845 = "<190>1 2015-01-06T20:56:33.287Z app-1 app - - [mdc@18060 ip=\"::ffff:132.123.15.30\" logger=\"{c.corp.Handler}\" session=\"4ot7\" user=\"user@example.com\" user-agent=\"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/600.2.5 (KHTML, like Gecko) Version/7.1.2 Safari/537.85.11\"] User page 13 requested";
    private static String STRUCTURED_ISSUE_845_EMPTY = "<128>1 2015-01-11T16:35:21.335797+01:00 s000000.example.com - - - - tralala";
    // The following message from issue 549 is from a Juniper SRX 240 device.
    private static String STRUCTURED_ISSUE_549 = "<14>1 2014-05-01T08:26:51.179Z fw01 RT_FLOW - RT_FLOW_SESSION_DENY [junos@2636.1.1.1.2.39 source-address=\"1.2.3.4\" source-port=\"56639\" destination-address=\"5.6.7.8\" destination-port=\"2003\" service-name=\"None\" protocol-id=\"6\" icmp-type=\"0\" policy-name=\"log-all-else\" source-zone-name=\"campus\" destination-zone-name=\"mngmt\" application=\"UNKNOWN\" nested-application=\"UNKNOWN\" username=\"N/A\" roles=\"N/A\" packet-incoming-interface=\"reth6.0\" encrypted=\"No\"]";
    private final String UNSTRUCTURED = "<45>Oct 21 12:09:37 c4dc57ba1ebb syslog-ng[7208]: syslog-ng starting up; version='3.5.3'";

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private Configuration configuration;
    @Mock
    private MetricRegistry metricRegistry;
    @Mock
    private Timer mockedTimer;

    private SyslogCodec codec;

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
        assertEquals("BOMAn application event log entry", message.getMessage());
        assertEquals(new DateTime("2012-12-25T22:14:15.003Z", DateTimeZone.UTC), ((DateTime) message.getField("timestamp")).withZone(DateTimeZone.UTC));
        assertEquals("mymachine.example.com", message.getField("source"));
        assertEquals(5, message.getField("level"));
        assertEquals("local4", message.getField("facility"));
        assertEquals("Application", message.getField("eventSource"));
        assertEquals("1011", message.getField("eventID"));
        assertEquals("3", message.getField("iut"));
        assertEquals("evntslog", message.getField("application_name"));
        assertEquals(20, message.getField("facility_num"));
    }

    @Test
    public void testDecodeStructuredIssue845() throws Exception {
        final Message message = codec.decode(buildRawMessage(STRUCTURED_ISSUE_845));

        assertNotNull(message);
        assertEquals("User page 13 requested", message.getMessage());
        assertEquals(new DateTime("2015-01-06T20:56:33.287Z", DateTimeZone.UTC), ((DateTime) message.getField("timestamp")).withZone(DateTimeZone.UTC));
        assertEquals("app-1", message.getField("source"));
        assertEquals(6, message.getField("level"));
        assertEquals("local7", message.getField("facility"));
        assertEquals("::ffff:132.123.15.30", message.getField("ip"));
        assertEquals("{c.corp.Handler}", message.getField("logger"));
        assertEquals("4ot7", message.getField("session"));
        assertEquals("user@example.com", message.getField("user"));
        assertEquals("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/600.2.5 (KHTML, like Gecko) Version/7.1.2 Safari/537.85.11", message.getField("user-agent"));
        assertEquals("app", message.getField("application_name"));
        assertEquals(23, message.getField("facility_num"));
    }

    @Test
    public void testDecodeStructuredIssue845WithExpandStructuredData() throws Exception {
        when(configuration.getBoolean(SyslogCodec.CK_EXPAND_STRUCTURED_DATA)).thenReturn(true);

        final SyslogCodec codec = new SyslogCodec(configuration, metricRegistry);
        final Message message = codec.decode(buildRawMessage(STRUCTURED_ISSUE_845));

        assertNotNull(message);
        assertEquals("User page 13 requested", message.getMessage());
        assertEquals(new DateTime("2015-01-06T20:56:33.287Z", DateTimeZone.UTC), ((DateTime) message.getField("timestamp")).withZone(DateTimeZone.UTC));
        assertEquals("app-1", message.getField("source"));
        assertEquals(6, message.getField("level"));
        assertEquals("local7", message.getField("facility"));
        assertEquals("::ffff:132.123.15.30", message.getField("mdc@18060_ip"));
        assertEquals("{c.corp.Handler}", message.getField("mdc@18060_logger"));
        assertEquals("4ot7", message.getField("mdc@18060_session"));
        assertEquals("user@example.com", message.getField("mdc@18060_user"));
        assertEquals("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/600.2.5 (KHTML, like Gecko) Version/7.1.2 Safari/537.85.11", message.getField("mdc@18060_user-agent"));
        assertEquals("app", message.getField("application_name"));
        assertEquals(23, message.getField("facility_num"));
    }

    @Test
    public void testDecodeStructuredIssue845Empty() throws Exception {
        final Message message = codec.decode(buildRawMessage(STRUCTURED_ISSUE_845_EMPTY));

        assertNotNull(message);
        assertEquals("tralala", message.getMessage());
        assertEquals(new DateTime("2015-01-11T15:35:21.335797Z", DateTimeZone.UTC), ((DateTime) message.getField("timestamp")).withZone(DateTimeZone.UTC));
        assertEquals("s000000.example.com", message.getField("source"));
        assertEquals(0, message.getField("level"));
        assertEquals("local0", message.getField("facility"));
        assertEquals(16, message.getField("facility_num"));
    }

    @Test
    public void testDecodeStructuredWithFullMessage() throws Exception {
        when(configuration.getBoolean(SyslogCodec.CK_STORE_FULL_MESSAGE)).thenReturn(true);

        final Message message = codec.decode(buildRawMessage(STRUCTURED));

        assertNotNull(message);
        assertEquals("BOMAn application event log entry", message.getMessage());
        assertEquals(new DateTime("2012-12-25T22:14:15.003Z", DateTimeZone.UTC), ((DateTime) message.getField("timestamp")).withZone(DateTimeZone.UTC));
        assertEquals("mymachine.example.com", message.getField("source"));
        assertEquals(5, message.getField("level"));
        assertEquals("local4", message.getField("facility"));
        assertEquals(STRUCTURED, message.getField("full_message"));
        assertEquals("Application", message.getField("eventSource"));
        assertEquals("1011", message.getField("eventID"));
        assertEquals("3", message.getField("iut"));
        assertEquals("evntslog", message.getField("application_name"));
        assertEquals(20, message.getField("facility_num"));
    }

    @Test
    public void testDecodeStructuredIssue549() throws Exception {
        final Message message = codec.decode(buildRawMessage(STRUCTURED_ISSUE_549));

        assertNotNull(message);
        assertEquals("RT_FLOW_SESSION_DENY [junos@2636.1.1.1.2.39 source-address=\"1.2.3.4\" source-port=\"56639\" destination-address=\"5.6.7.8\" destination-port=\"2003\" service-name=\"None\" protocol-id=\"6\" icmp-type=\"0\" policy-name=\"log-all-else\" source-zone-name=\"campus\" destination-zone-name=\"mngmt\" application=\"UNKNOWN\" nested-application=\"UNKNOWN\" username=\"N/A\" roles=\"N/A\" packet-incoming-interface=\"reth6.0\" encrypted=\"No\"]", message.getMessage());
        assertEquals(new DateTime("2014-05-01T08:26:51.179Z", DateTimeZone.UTC), ((DateTime) message.getField("timestamp")).withZone(DateTimeZone.UTC));
        assertEquals("1.2.3.4", message.getField("source-address"));
        assertEquals("56639", message.getField("source-port"));
        assertEquals("5.6.7.8", message.getField("destination-address"));
        assertEquals("2003", message.getField("destination-port"));
        assertEquals("None", message.getField("service-name"));
        assertEquals("6", message.getField("protocol-id"));
        assertEquals("0", message.getField("icmp-type"));
        assertEquals("log-all-else", message.getField("policy-name"));
        assertEquals("campus", message.getField("source-zone-name"));
        assertEquals("mngmt", message.getField("destination-zone-name"));
        assertEquals("UNKNOWN", message.getField("application"));
        assertEquals("UNKNOWN", message.getField("nested-application"));
        assertEquals("N/A", message.getField("username"));
        assertEquals("N/A", message.getField("roles"));
        assertEquals("reth6.0", message.getField("packet-incoming-interface"));
        assertEquals("No", message.getField("encrypted"));
        assertEquals(1, message.getField("facility_num"));
    }

    @Test
    public void testDecodeUnstructured() throws Exception {
        final Message message = codec.decode(buildRawMessage(UNSTRUCTURED));

        assertNotNull(message);
        assertEquals("c4dc57ba1ebb syslog-ng[7208]: syslog-ng starting up; version='3.5.3'", message.getMessage());
        assertEquals(new DateTime(YEAR + "-10-21T12:09:37"), message.getField("timestamp"));
        assertEquals("c4dc57ba1ebb", message.getField("source"));
        assertEquals(5, message.getField("level"));
        assertEquals("syslogd", message.getField("facility"));
        assertNull(message.getField("full_message"));
        assertEquals(5, message.getField("facility_num"));
    }

    @Test
    public void testDecodeUnstructuredWithFullMessage() throws Exception {
        when(configuration.getBoolean(SyslogCodec.CK_STORE_FULL_MESSAGE)).thenReturn(true);

        final Message message = codec.decode(buildRawMessage(UNSTRUCTURED));

        assertNotNull(message);
        assertEquals("c4dc57ba1ebb syslog-ng[7208]: syslog-ng starting up; version='3.5.3'", message.getMessage());
        assertEquals(new DateTime(YEAR + "-10-21T12:09:37"), message.getField("timestamp"));
        assertEquals("c4dc57ba1ebb", message.getField("source"));
        assertEquals(5, message.getField("level"));
        assertEquals("syslogd", message.getField("facility"));
        assertEquals(UNSTRUCTURED, message.getField("full_message"));
        assertEquals(5, message.getField("facility_num"));
    }

    @Test
    public void rfc3164_section5_4_messages() {
        // See https://tools.ietf.org/html/rfc3164#section-5.4
        final Map<String, Map<String, Object>> rfc3164messages = ImmutableMap.of(
            "<34>Oct 11 22:14:15 mymachine su: 'su root' failed for lonvick on /dev/pts/8",
            ImmutableMap.of(
                "timestamp", new DateTime(YEAR + "-10-11T22:14:15"),
                "source", "mymachine",
                "level", 2,
                "facility", "security/authorization",
                "message", "mymachine su: 'su root' failed for lonvick on /dev/pts/8"),

            "<13>Feb  5 17:32:18 10.0.0.99 Use the BFG!",
            ImmutableMap.of(
                "timestamp", new DateTime(YEAR + "-02-05T17:32:18"),
                "source", "10.0.0.99",
                "level", 5,
                "facility", "user-level",
                "message", "10.0.0.99 Use the BFG!")

            /* FAILING
            "<165>Aug 24 05:34:00 CST 1987 mymachine myproc[10]: %% It's time to make the do-nuts.  %%  Ingredients: Mix=OK, Jelly=OK # Devices: Mixer=OK, Jelly_Injector=OK, Frier=OK # Transport: Conveyer1=OK, Conveyer2=OK # %%",
            ImmutableMap.of(
                "timestamp", new DateTime("1987-08-24T05:34:00", DateTimeZone.forID("CST6CDT")),
                "source", "mymachine",
                "level", 5,
                "facility", "local4"
            )
            */

            /* FAILING
            "<0>1990 Oct 22 10:52:01 TZ-6 scapegoat.dmz.example.org 10.1.2.3 sched[0]: That's All Folks!",
            ImmutableMap.of(
                "timestamp", new DateTime("1990-10-22T10:52:01", DateTimeZone.forID("Etc/GMT-6")),
                "source", "scapegoat.dmz.example.org",
                "level", 0,
                "facility", "kernel"
            )
            */
        );

        for (Map.Entry<String, Map<String, Object>> entry : rfc3164messages.entrySet()) {
            final Message message = codec.decode(buildRawMessage(entry.getKey()));
            assertThat(message).isNotNull();
            assertThat(message.getFields()).containsAllEntriesOf(entry.getValue());
        }
    }

    @Test
    @SuppressForbidden("Deliberate invocation")
    public void rfc5424_section6_5_messages() {
        // See https://tools.ietf.org/html/rfc5424#section-6.5
        final Map<String, Map<String, Object>> rfc3164messages = ImmutableMap.of(
            "<34>1 2003-10-11T22:14:15.003Z mymachine.example.com su - ID47 - BOM'su root' failed for lonvick on /dev/pts/8",
            ImmutableMap.<String, Object>builder()
                .put("timestamp", new DateTime("2003-10-11T22:14:15.003", DateTimeZone.UTC).withZone(DateTimeZone.getDefault()))
                .put("source", "mymachine.example.com")
                .put("level", 2)
                .put("facility", "security/authorization")
                .put("application_name", "su")
                .put("message", "ID47 - BOM'su root' failed for lonvick on /dev/pts/8")
                .build(),

            "<165>1 2003-08-24T05:14:15.000003-07:00 192.0.2.1 myproc 8710 - - %% It's time to make the do-nuts.",
            ImmutableMap.<String, Object>builder()
                .put("timestamp", new DateTime("2003-08-24T05:14:15.000", DateTimeZone.forOffsetHours(-7)).withZone(DateTimeZone.getDefault()))
                .put("source", "192.0.2.1")
                .put("level", 5)
                .put("facility", "local4")
                .put("application_name", "myproc")
                .put("process_id", "8710")
                .put("message", "%% It's time to make the do-nuts.")
                .build(),

            "<165>1 2003-10-11T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"] An application event log entry...",
            ImmutableMap.<String, Object>builder()
                .put("timestamp", new DateTime("2003-10-11T22:14:15.003", DateTimeZone.UTC).withZone(DateTimeZone.getDefault()))
                .put("source", "mymachine.example.com")
                .put("level", 5)
                .put("facility", "local4")
                .put("application_name", "evntslog")
                .put("message", "An application event log entry...")
                .put("iut", "3")
                .put("eventID", "1011")
                .put("eventSource", "Application")
                .build(),

            "<165>1 2003-10-11T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"][examplePriority@32473 class=\"high\"]",
            ImmutableMap.<String, Object>builder()
                .put("timestamp", new DateTime("2003-10-11T22:14:15.003", DateTimeZone.UTC).withZone(DateTimeZone.getDefault()))
                .put("source", "mymachine.example.com")
                .put("level", 5)
                .put("facility", "local4")
                .put("application_name", "evntslog")
                .put("iut", "3")
                .put("eventID", "1011")
                .put("eventSource", "Application")
                .build()

        );

        for (Map.Entry<String, Map<String, Object>> entry : rfc3164messages.entrySet()) {
            final Message message = codec.decode(buildRawMessage(entry.getKey()));
            assertThat(message).isNotNull();
            assertThat(message.getFields()).containsAllEntriesOf(entry.getValue());
        }
    }

    @Test
    public void testIssue2954() throws Exception {
        // https://github.com/Graylog2/graylog2-server/issues/2954
        final RawMessage rawMessage = buildRawMessage("<6>2016-10-12T14:10:18Z hostname testmsg[20]: Test");
        final Message message = codec.decode(rawMessage);

        assertNotNull(message);
        assertEquals("hostname testmsg[20]: Test", message.getMessage());
        assertEquals(new DateTime(2016, 10, 12, 14, 10, 18, DateTimeZone.UTC), message.getTimestamp());
        assertEquals("hostname", message.getSource());
        assertEquals(6, message.getField("level"));
        assertEquals("kernel", message.getField("facility"));
        assertEquals(0, message.getField("facility_num"));
    }

    @Test
    public void testIssue3502() throws Exception {
        // https://github.com/Graylog2/graylog2-server/issues/3502
        final RawMessage rawMessage = buildRawMessage("<6>0 2017-02-15T16:01:07.000+01:00 hostname test - - -  test 4");
        final Message message = codec.decode(rawMessage);

        assertNotNull(message);
        assertEquals("test 4", message.getMessage());
        assertEquals(new DateTime(2017, 2, 15, 15, 1, 7, DateTimeZone.UTC), message.getTimestamp());
        assertEquals("hostname", message.getSource());
        assertEquals(6, message.getField("level"));
        assertEquals("kernel", message.getField("facility"));
        assertEquals("test", message.getField("application_name"));
        assertEquals(0, message.getField("facility_num"));
    }

    @Test
    @SuppressForbidden("Deliberate invocation")
    public void testCiscoSyslogMessages() {
        final int year = ZonedDateTime.now().getYear();
        final Map<String, Map<String, Object>> messages = ImmutableMap.<String, Map<String, Object>>builder()
                .put(
                        "<186>1541800: Feb 27 06:08:59.485: %HARDWARE-2-FAN_ERROR: Fan Failure",
                        ImmutableMap.<String, Object>builder()
                                .put("timestamp", new DateTime(year, 2, 27, 6, 8, 59, 485, DateTimeZone.UTC).withZone(DateTimeZone.getDefault()))
                                .put("source", "127.0.0.1")
                                .put("level", 2)
                                .put("facility", "local7")
                                .put("message", "%HARDWARE-2-FAN_ERROR: Fan Failure")
                                .put("sequence_number", 1541800)
                                .build())
                .put(
                        "<189>148093: Feb 27 06:07:28.713: %LINEPROTO-5-UPDOWN: Line protocol on Interface GigabitEthernet1/0/15, changed state to down",
                        ImmutableMap.<String, Object>builder()
                                .put("timestamp", new DateTime(year, 2, 27, 6, 7, 28, 713, DateTimeZone.UTC).withZone(DateTimeZone.getDefault()))
                                .put("source", "127.0.0.1")
                                .put("level", 5)
                                .put("facility", "local7")
                                .put("message", "%LINEPROTO-5-UPDOWN: Line protocol on Interface GigabitEthernet1/0/15, changed state to down")
                                .build())
                .put(
                        "<190>530470: *Sep 28 17:13:35.098: %SEC-6-IPACCESSLOGP: list MGMT_IN denied udp IP(49964) -> IP(161), 11 packets",
                        ImmutableMap.<String, Object>builder()
                                .put("timestamp", new DateTime(year, 9, 28, 17, 13, 35, 98, DateTimeZone.UTC).withZone(DateTimeZone.getDefault()))
                                .put("source", "127.0.0.1")
                                .put("level", 6)
                                .put("facility", "local7")
                                .put("message", "%SEC-6-IPACCESSLOGP: list MGMT_IN denied udp IP(49964) -> IP(161), 11 packets")
                                .build())
                .put(
                        "<190>: 2017 Mar 06 09:22:34 CET: %AUTHPRIV-6-SYSTEM_MSG: START: rsync pid=4311 from=::ffff:IP - xinetd[6219]",
                        ImmutableMap.<String, Object>builder()
                                .put("timestamp", new DateTime(2017, 3, 6, 9, 22, 34, DateTimeZone.forID("CET")).withZone(DateTimeZone.getDefault()))
                                .put("source", "127.0.0.1")
                                .put("level", 6)
                                .put("facility", "local7")
                                .put("message", "%AUTHPRIV-6-SYSTEM_MSG: START: rsync pid=4311 from=::ffff:IP - xinetd[6219]")
                                .build())
                .put(
                        "<134>: 2017 Mar  6 12:53:10 UTC: %POLICY_ENGINE-6-POLICY_LOOKUP_EVENT: policy=POLICYNAME rule=RULENAME action=Permit direction=egress src.net.ip-address=IP src.net.port=38321 dst.net.ip-address=IP dst.net.port=5666 net.protocol=6 net.ethertype=800 net.service=\"protocol 6 port 5666\"",
                        ImmutableMap.<String, Object>builder()
                                .put("timestamp", new DateTime(2017, 3, 6, 12, 53, 10, DateTimeZone.UTC).withZone(DateTimeZone.getDefault()))
                                .put("source", "127.0.0.1")
                                .put("level", 6)
                                .put("facility", "local0")
                                .put("message", "%POLICY_ENGINE-6-POLICY_LOOKUP_EVENT: policy=POLICYNAME rule=RULENAME action=Permit direction=egress src.net.ip-address=IP src.net.port=38321 dst.net.ip-address=IP dst.net.port=5666 net.protocol=6 net.ethertype=800 net.service=\"protocol 6 port 5666\"")
                                .build())
                .build();

        for (Map.Entry<String, Map<String, Object>> entry : messages.entrySet()) {
            final Message message = codec.decode(buildRawMessage(entry.getKey()));
            assertThat(message).isNotNull();
            assertThat(message.getFields()).containsAllEntriesOf(entry.getValue());
        }
    }

    @Test
    public void testFortiGateFirewall() {
        final RawMessage rawMessage = buildRawMessage("<45>date=2017-03-06 time=12:53:10 devname=DEVICENAME devid=DEVICEID logid=0000000013 type=traffic subtype=forward level=notice vd=ALIAS srcip=IP srcport=45748 srcintf=\"IF\" dstip=IP dstport=443 dstintf=\"IF\" sessionid=1122686199 status=close policyid=77 dstcountry=\"COUNTRY\" srccountry=\"COUNTRY\" trandisp=dnat tranip=IP tranport=443 service=HTTPS proto=6 appid=41540 app=\"SSL_TLSv1.2\" appcat=\"Network.Service\" applist=\"ACLNAME\" appact=detected duration=1 sentbyte=2313 rcvdbyte=14883 sentpkt=19 rcvdpkt=19 utmaction=passthrough utmevent=app-ctrl attack=\"SSL\" hostname=\"HOSTNAME\"");
        final Message message = codec.decode(rawMessage);

        assertThat(message).isNotNull();
        assertThat(message.getMessage()).isEqualTo("date=2017-03-06 time=12:53:10 devname=DEVICENAME devid=DEVICEID logid=0000000013 type=traffic subtype=forward level=notice vd=ALIAS srcip=IP srcport=45748 srcintf=\"IF\" dstip=IP dstport=443 dstintf=\"IF\" sessionid=1122686199 status=close policyid=77 dstcountry=\"COUNTRY\" srccountry=\"COUNTRY\" trandisp=dnat tranip=IP tranport=443 service=HTTPS proto=6 appid=41540 app=\"SSL_TLSv1.2\" appcat=\"Network.Service\" applist=\"ACLNAME\" appact=detected duration=1 sentbyte=2313 rcvdbyte=14883 sentpkt=19 rcvdpkt=19 utmaction=passthrough utmevent=app-ctrl attack=\"SSL\" hostname=\"HOSTNAME\"");
        assertThat(message.getTimestamp()).isEqualTo(new DateTime(2017, 3, 6, 12, 53, 10, DateTimeZone.UTC));
        assertThat(message.getField("source")).isEqualTo("DEVICENAME");
        assertThat(message.getField("level")).isEqualTo(5);
        assertThat(message.getField("facility")).isEqualTo("syslogd");
        assertThat(message.getField("logid")).isEqualTo("0000000013");
        assertThat(message.getField("app")).isEqualTo("SSL_TLSv1.2");
        assertThat(message.getField("facility_num")).isEqualTo(5);
    }

    private RawMessage buildRawMessage(String message) {
        return new RawMessage(message.getBytes(StandardCharsets.UTF_8), new InetSocketAddress("127.0.0.1", 5140));
    }
}
