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
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.syslog4j.server.impl.event.structured.StructuredSyslogServerEvent;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class StructuredSyslogTest {
    // http://tools.ietf.org/rfc/rfc5424.txt
    private static final String ValidStructuredMessage = "<165>1 2012-12-25T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"] BOMAn application event log entry";
    private static final String ValidStructuredMultiMessage = "<165>1 2012-12-25T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"][meta sequenceId=\"1\"] BOMAn application event log entry";
    private static final String ValidStructuredMultiMessageSameKey = "<165>1 2012-12-25T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventID=\"1011\"][meta iut=\"10\"] BOMAn application event log entry";
    private static final String ValidStructuredNoStructValues = "<165>1 2012-12-25T22:14:15.003Z mymachine.example.com evntslog - ID47 - BOMAn application event log entry";
    private static final String ValidNonStructuredMessage = "<86>Dec 24 17:05:01 nb-lkoopmann CRON[10049]: pam_unix(cron:session): session closed for user root";
    private static final String MessageLookingLikeStructured = "<133>NOMA101FW01A: NetScreen device_id=NOMA101FW01A [Root]system-notification-00257(traffic): start_time=\"2011-12-23 17:33:43\" duration=0 reason=Creation";

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final Configuration configuration = new Configuration(ImmutableMap.<String, Object>of(
            SyslogCodec.CK_FORCE_RDNS, false,
            SyslogCodec.CK_ALLOW_OVERRIDE_DATE, false,
            SyslogCodec.CK_EXPAND_STRUCTURED_DATA, true,
            SyslogCodec.CK_STORE_FULL_MESSAGE, true
    ));
    private SyslogCodec syslogCodec;
    @Mock
    private MetricRegistry metricRegistry;
    @Mock
    private Timer mockedTimer;

    @Before
    public void setUp() {
        when(metricRegistry.timer(any(String.class))).thenReturn(mockedTimer);
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
        assertEquals(expected, result);
    }

    @Test
    public void testExtractMoreFields() {
        Map<String, Object> expected = new HashMap<>();
        expected.put("eventSource", "Application");
        expected.put("eventID", "1011");
        expected.put("iut", "3");
        expected.put("sequenceId", "1");

        Map<String, Object> result = syslogCodec.extractFields(newEvent(ValidStructuredMultiMessage), false);
        assertEquals(expected, result);
    }

    @Test
    public void testExtractFieldsWithoutExpansion() {
        // Order is not guaranteed in the current syslog4j implementation!
        Map<String, Object> result = syslogCodec.extractFields(newEvent(ValidStructuredMultiMessageSameKey), false);
        assertTrue("iut value is not 3 or 10!", Pattern.compile("3|10").matcher((String) result.get("iut")).matches());
    }

    @Test
    public void testExtractFieldsWithExpansion() {
        Map<String, Object> result = syslogCodec.extractFields(newEvent(ValidStructuredMultiMessageSameKey), true);
        assertEquals("3", result.get("exampleSDID@32473_iut"));
        assertEquals("10", result.get("meta_iut"));
    }

    @Test
    public void testExtractNoFields() {
        Map<String, Object> result = syslogCodec.extractFields(newEvent(ValidStructuredNoStructValues), false);
        assertEquals(Collections.<String, Object>emptyMap(), result);
    }

    @Test
    public void testExtractFieldsOfNonStructuredMessage() {
        Map<String, Object> result = syslogCodec.extractFields(newEvent(ValidNonStructuredMessage), false);
        assertEquals(0, result.size());
    }

    @Test
    public void testExtractFieldsOfAMessageThatOnlyLooksStructured() {
        Map<String, Object> result = syslogCodec.extractFields(newEvent(MessageLookingLikeStructured), false);
        assertEquals(0, result.size());
    }
}
