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
package org.graylog.plugins.cef.codec;

import org.graylog.plugins.cef.parser.MappedMessage;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.failure.InputProcessingException;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CEFCodecTest {
    private CEFCodec codec;
    private final MessageFactory messageFactory = new TestMessageFactory();

    @BeforeEach
    public void setUp() {
        codec = new CEFCodec(Configuration.EMPTY_CONFIGURATION, messageFactory);
    }

    @Test
    public void buildMessageSummary() throws Exception {
        final MappedMessage cefMessage = mock(MappedMessage.class);
        when(cefMessage.deviceProduct()).thenReturn("product");
        when(cefMessage.deviceEventClassId()).thenReturn("event-class-id");
        when(cefMessage.name()).thenReturn("name");
        when(cefMessage.severity()).thenReturn("High");
        assertEquals("product: [event-class-id, High] name", codec.buildMessageSummary(cefMessage));
    }

    @Test
    public void decideSourceWithoutDeviceAddressReturnsRawMessageRemoteAddress() throws Exception {
        final MappedMessage cefMessage = mock(MappedMessage.class);
        when(cefMessage.mappedExtensions()).thenReturn(Collections.emptyMap());

        final RawMessage rawMessage = new RawMessage(new byte[0], new InetSocketAddress("128.66.23.42", 12345));

        // The hostname is unresolved, so we have to add the leading slash. Oh, Java...
        assertEquals("/128.66.23.42", codec.decideSource(cefMessage, rawMessage));
    }

    @Test
    public void decideSourceWithoutDeviceAddressReturnsCEFHostname() throws Exception {
        final MappedMessage cefMessage = mock(MappedMessage.class);
        when(cefMessage.host()).thenReturn("128.66.23.42");
        when(cefMessage.mappedExtensions()).thenReturn(Collections.emptyMap());

        final RawMessage rawMessage = new RawMessage(new byte[0], new InetSocketAddress("example.com", 12345));

        assertEquals("128.66.23.42", codec.decideSource(cefMessage, rawMessage));
    }

    @Test
    public void decideSourceWithFullDeviceAddressReturnsExtensionValue() throws Exception {
        final MappedMessage cefMessage = mock(MappedMessage.class);
        when(cefMessage.mappedExtensions()).thenReturn(Collections.singletonMap("deviceAddress", "128.66.23.42"));

        final RawMessage rawMessage = new RawMessage(new byte[0], new InetSocketAddress("example.com", 12345));

        assertEquals("128.66.23.42", codec.decideSource(cefMessage, rawMessage));
    }

    @Test
    public void decideSourceWithShortDeviceAddressReturnsExtensionValue() throws Exception {
        final MappedMessage cefMessage = mock(MappedMessage.class);
        when(cefMessage.mappedExtensions()).thenReturn(Collections.singletonMap("dvc", "128.66.23.42"));

        final RawMessage rawMessage = new RawMessage(new byte[0], new InetSocketAddress("example.com", 12345));

        assertEquals("128.66.23.42", codec.decideSource(cefMessage, rawMessage));
    }

    @Test
    public void testIssue8844() {
        // https://github.com/Graylog2/graylog-plugin-enterprise/issues/8844
        final RawMessage rawMessage = buildRawMessage("<134>1 2024-09-13T12:23:43.288000+02:00 netscaler-waf appname 1234 msgid - CEF:0|Vendor|Product|Version|EventID|Name|Severity|...\n");
        final MappedMessage cefMessage = mock(MappedMessage.class);
        when(cefMessage.mappedExtensions()).thenReturn(Collections.singletonMap("dvc", "128.66.23.42"));
        final Message message = codec.decodeSafe(rawMessage).get();
        verifyFields(message);
    }

    @Test
    public void testIssue8844WithoutPriority() {
        // https://github.com/Graylog2/graylog-plugin-enterprise/issues/8844
        final RawMessage rawMessage = buildRawMessage("<134>2024-09-13T12:23:43.288000+02:00 netscaler-waf appname 1234 msgid - CEF:0|Vendor|Product|Version|EventID|Name|Severity|...\n");
        final MappedMessage cefMessage = mock(MappedMessage.class);
        when(cefMessage.mappedExtensions()).thenReturn(Collections.singletonMap("dvc", "128.66.23.42"));
        final Message message = codec.decodeSafe(rawMessage).get();
        verifyFields(message);
    }

    private static void verifyFields(Message message) {
        assertTrue(message.getMessage().contains("Product: [EventID, Severity] Name"));
        assertEquals(6, message.getField("level"));
        assertEquals("Vendor", message.getField("device_vendor"));
        assertEquals("local0", message.getField("facility"));
        // 12:23:43.288000+02:00. The six digit fraction is microseconds, so it must not add 288 seconds.
        assertEquals(new DateTime(2024, 9, 13, 10, 23, 43, 288, DateTimeZone.UTC), message.getTimestamp());
    }

    @Test
    public void specCompliantDoubleEscapedBackslashIsUnescapedOnce() {
        // CEF spec: a literal backslash in an extension value is written as "\\".
        final RawMessage rawMessage = buildRawMessage(
                "CEF:0|Vendor|Product|1.0|100|AntiMalware|9|filePath=C:\\\\Windows\\\\System32\\\\reg.exe fname=C:\\\\temp\\\\notes.txt");
        final Message message = codec.decodeSafe(rawMessage).get();

        assertEquals("C:\\Windows\\System32\\reg.exe", message.getField("filePath"));
        assertEquals("C:\\temp\\notes.txt", message.getField("fname"));
    }

    private static final String RFC5424_HEADER = "<12>1 2026-09-03T16:35:24.921661+00:00 collector Vendor 692316 - ";

    @Test
    public void syslogHeaderPrecededByANumberIsStripped() {
        final RawMessage rawMessage = buildRawMessage("240 " + RFC5424_HEADER + "CEF:0|Vendor|Product|1.0|100|Alert|5|msg=leading-number");
        final Message message = codec.decodeSafe(rawMessage).get();

        assertEquals("Vendor", message.getField("device_vendor"));
        assertEquals("Alert", message.getField("name"));
    }

    @Test
    public void syslogHeaderWithEmbeddedCarriageReturnIsStripped() {
        final RawMessage rawMessage = buildRawMessage(RFC5424_HEADER + "CEF:0|Vendor|Product|1.0|100|Alert|5|msg=line1\rline2");
        final Message message = codec.decodeSafe(rawMessage).get();

        assertEquals("Vendor", message.getField("device_vendor"));
        assertEquals("Alert", message.getField("name"));
    }

    @Test
    public void rfc5424TimestampAndHostAreParsedFromSyslogHeader() {
        final RawMessage rawMessage = buildRawMessage(RFC5424_HEADER + "CEF:0|Vendor|Product|1.0|100|Alert|5|msg=control");
        final Message message = codec.decodeSafe(rawMessage).get();

        // Microseconds must not be read as milliseconds, and the host is the syslog hostname, not the "-" structured-data marker.
        assertEquals(new DateTime(2026, 9, 3, 16, 35, 24, 921, DateTimeZone.UTC), message.getTimestamp());
        assertEquals("collector", message.getSource());
    }

    // A BSD timestamp contains spaces, so it cannot be found by stopping at the first one.
    @Test
    public void bsdTimestampAndHostAreParsedFromSyslogHeader() {
        final CEFCodec utcCodec = new CEFCodec(new Configuration(Map.of("timezone", "UTC")), messageFactory);
        final RawMessage rawMessage = buildRawMessage("<12>Sep 03 16:35:24 collector CEF:0|Vendor|Product|1.0|100|Name|5|msg=bsd");
        final Message message = utcCodec.decodeSafe(rawMessage).get();

        // A BSD timestamp carries no year, so it takes the current one.
        final int currentYear = DateTime.now(DateTimeZone.UTC).getYear();
        assertEquals(new DateTime(currentYear, 9, 3, 16, 35, 24, DateTimeZone.UTC), message.getTimestamp());
        assertEquals("collector", message.getSource());
    }

    // An epoch timestamp followed by RFC 5424 fields used to be swallowed whole and fail to parse.
    @Test
    public void epochTimestampIsParsedWhenTheHeaderCarriesTrailingFields() {
        final RawMessage rawMessage = buildRawMessage(
                "<12>1 1725381324921 collector Vendor 692316 - - CEF:0|Vendor|Product|1.0|100|Name|5|msg=epoch");
        final Message message = codec.decodeSafe(rawMessage).get();

        assertEquals(new DateTime(1725381324921L, DateTimeZone.UTC), message.getTimestamp());
        assertEquals("collector", message.getSource());
    }

    // A zone name is a trailing token, so looking for the shortest parseable timestamp would leave it in the hostname.
    @Test
    public void bsdTimestampWithZoneNameKeepsTheZoneOutOfTheHostname() {
        final CEFCodec utcCodec = new CEFCodec(new Configuration(Map.of("timezone", "UTC")), messageFactory);
        final RawMessage rawMessage = buildRawMessage("<12>Sep 03 16:35:24 CEST collector CEF:0|Vendor|Product|1.0|100|Name|5|msg=zone");
        final Message message = utcCodec.decodeSafe(rawMessage).get();

        final int currentYear = DateTime.now(DateTimeZone.UTC).getYear();
        assertEquals(new DateTime(currentYear, 9, 3, 14, 35, 24, DateTimeZone.UTC), message.getTimestamp());
        assertEquals("collector", message.getSource());
    }

    // Header fields are not trimmed, so a delimiter left on the payload would end up inside the last one.
    @Test
    public void trailingLineTerminatorIsNotPartOfTheLastHeaderField() {
        final RawMessage rawMessage = buildRawMessage("<12>CEF:0|Vendor|Product|1.0|100|Name|5\r\n");
        final Message message = codec.decodeSafe(rawMessage).get();

        assertEquals("5", message.getField("severity"));
    }

    // Several appliances emit a timestamp with no zone, or with a space instead of the RFC 3339 "T".
    @Test
    public void zoneLessAndSpaceSeparatedIsoTimestampsAreParsed() {
        final CEFCodec utcCodec = new CEFCodec(new Configuration(Map.of("timezone", "UTC")), messageFactory);
        final DateTime expected = new DateTime(2026, 9, 3, 16, 35, 24, DateTimeZone.UTC);

        for (String timestamp : new String[]{"2026-09-03T16:35:24", "2026-09-03 16:35:24"}) {
            final Message message = utcCodec.decodeSafe(buildRawMessage(
                    "<12>" + timestamp + " collector CEF:0|Vendor|Product|1.0|100|Name|5|msg=x")).get();

            assertEquals(expected, message.getTimestamp(), timestamp);
            assertEquals("collector", message.getSource(), timestamp);
        }
    }

    // A zoned timestamp still wins over the zone-less formats, which are only reached as a fallback.
    @Test
    public void zonedTimestampIsPreferredOverTheZoneLessFallback() {
        final CEFCodec utcCodec = new CEFCodec(new Configuration(Map.of("timezone", "UTC")), messageFactory);
        final Message message = utcCodec.decodeSafe(buildRawMessage(
                "<12>2026-09-03T16:35:24 +0200 collector CEF:0|Vendor|Product|1.0|100|Name|5|msg=x")).get();

        assertEquals(new DateTime(2026, 9, 3, 14, 35, 24, DateTimeZone.UTC), message.getTimestamp());
        assertEquals("collector", message.getSource());
    }

    @Test
    public void payloadWithoutACefHeaderReportsWhatIsWrong() {
        final RawMessage rawMessage = buildRawMessage("<12>1 2026-09-03T16:35:24Z collector this is not a CEF message");

        final InputProcessingException e =
                assertThrows(InputProcessingException.class, () -> codec.decodeSafe(rawMessage));
        assertTrue(e.getCause().getMessage().contains("No CEF header found"), e.getCause().getMessage());
    }

    @Test
    public void getAggregator() throws Exception {
        assertNull(codec.getAggregator());
    }

    private RawMessage buildRawMessage(String message) {
        return new RawMessage(message.getBytes(StandardCharsets.UTF_8), new InetSocketAddress("127.0.0.1", 5140));
    }
}
