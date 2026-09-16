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
package org.graylog.integrations.inputs.paloalto;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Unit tests for {@link PaloAltoParser}.
 */
public class PaloAltoParserTest {

    // Valid Panorama-format messages
    private static final String PANORAMA_TRAFFIC_MESSAGE =
            "<14>1 2018-09-19T11:50:32-05:00 Panorama--2 - - - - 1,2018/09/19 11:50:32,453524335,TRAFFIC,end,2049," +
                    "2018/09/19 11:50:32,10.20.30.40,10.20.30.40,10.20.30.40,10.20.30.40,HTTPS-strict,,,incomplete,vsys1," +
                    "Public,Public,ethernet1/1,ethernet1/1,ALK Logging,2018/09/19 11:50:32,205742,1,64575,443,41304,443," +
                    "0x400070,tcp,allow,412,272,140,6,2018/09/19 11:50:15,0,any,0,54196730,0x8000000000000000," +
                    "10.20.30.40-10.20.30.40,10.20.30.40-10.20.30.40,0,4,2,tcp-fin,13,16,0,0,,Prod--2,from-policy,,,0,,0,,N/A,0,0,0,0";

    private static final String PANORAMA_SYSTEM_MESSAGE =
            "<14>1 2018-09-19T11:50:35-05:00 Panorama-1 - - - - 1,2018/09/19 11:50:35,000710000506,SYSTEM,general,0," +
                    "2018/09/19 11:50:35,,general,,0,0,general,informational," +
                    "\"Deviating device: Prod--2, Serial: 453524335, Object: N/A, Metric: mp-cpu, Value: 34\"," +
                    "1163103,0x0,0,0,0,0,,Panorama-1";

    // Panorama message whose timestamp carries no timezone offset — timezone comes from the input config.
    // 2018-09-19T11:50:33 in America/Chicago (CDT, UTC-5) => 2018-09-19T16:50:33Z
    private static final String PANORAMA_NO_TZ_MESSAGE =
            "<14>1 2018-09-19T11:50:33 Panorama--1 - - - - 1,2018/09/19 11:50:33,007255000045716,THREAT,spyware,2049," +
                    "2018/09/19 11:50:33,10.20.30.40,10.20.30.40,10.20.30.40,10.20.30.40,HTTPS-strict,,,ssl,vsys1,Public,Public," +
                    "ethernet1/1,ethernet1/1,ALK Logging,2018/09/19 11:50:33,201360,1,21131,443,56756,443,0x80403000,tcp,alert," +
                    "\"test.com/\",Suspicious TLS Evasion Found(14978),online_test.com,informational,client-to-server," +
                    "1007133,0xa000000000000000,10.20.30.40-10.20.30.40,10.20.30.40-10.20.30.40,0,,1204440535977427988,,,0,,,,,," +
                    ",,0,13,16,0,0,,Prod--1,,,,,0,,0,,N/A,spyware,AppThreat-8065-5006,0x0,0,4294967295";

    // Valid standard-syslog-format messages (STANDARD_SYSLOG_PARSER path)
    private static final String STANDARD_SYSLOG_THREAT_MESSAGE =
            "<14>Aug 22 11:21:04 hq-lx-net-7.dart.org 1,2018/08/22 11:21:04,013201001141,THREAT,vulnerability,0," +
            "2018/08/22 11:21:02,10.0.190.116,10.0.2.225,0.0.0.0,0.0.0.0,DMZ-to-LAN,,,msrpc,vsys1,DMZ,LAN," +
            "ethernet1/3,ethernet1/6,Panorama,2018/08/22 11:21:02,398906,1,26475,135,0,0,0x2000,tcp,alert,\"\"," +
            "Microsoft RPC Endpoint Mapper Detection(30845),any,informational,client-to-server," +
            "6585310726021616818,0x8000000000000000,10.0.0.0-10.255.255.255,10.0.0.0-10.255.255.255,0,,0,,,0,,,,,,,,0," +
            "346,12,0,0,,pa5220-hq-mdf-1,,,,,0,,0,,N/A,info-leak,AppThreat-8054-4933,0x0";

    // Same message with a double-space before the single-digit day ("Aug  2" vs "Aug 22").
    // The parser strips the extra space before date parsing.
    private static final String STANDARD_SYSLOG_DOUBLE_SPACE_DATE_MESSAGE =
            "<14>Aug  2 11:21:04 hq-lx-net-7.dart.org 1,2018/08/02 11:21:04,013201001141,THREAT,vulnerability,0," +
            "2018/08/02 11:21:02,10.0.190.116,10.0.2.225,0.0.0.0,0.0.0.0,DMZ-to-LAN,,,msrpc,vsys1,DMZ,LAN," +
            "ethernet1/3,ethernet1/6,Panorama,2018/08/02 11:21:02,398906,1,26475,135,0,0,0x2000,tcp,alert,\"\"," +
            "Microsoft RPC Endpoint Mapper Detection(30845),any,informational,client-to-server," +
            "6585310726021616818,0x8000000000000000,10.0.0.0-10.255.255.255,10.0.0.0-10.255.255.255,0,,0,,,0,,,,,,,,0," +
            "346,12,0,0,,pa5220-hq-mdf-1,,,,,0,,0,,N/A,info-leak,AppThreat-8054-4933,0x0";

    private PaloAltoParser parser;

    @BeforeEach
    public void setUp() {
        parser = new PaloAltoParser();
    }

    // -------------------------------------------------------------------------
    // Valid Panorama messages — ensure fix does not break existing behaviour
    // -------------------------------------------------------------------------

    @Test
    public void parse_validPanoramaTrafficMessage_returnsCorrectSourceAndTimestamp() {
        final PaloAltoMessageBase result = parser.parse(PANORAMA_TRAFFIC_MESSAGE, DateTimeZone.UTC);

        assertThat(result).isNotNull();
        assertThat(result.source()).isEqualTo("Panorama--2");
        // 2018-09-19T11:50:32-05:00 => 2018-09-19T16:50:32Z
        final DateTime expected = new DateTime("2018-09-19T16:50:32.000Z", DateTimeZone.UTC);
        assertThat(result.timestamp().withZone(DateTimeZone.UTC)).isEqualTo(expected);
    }

    @Test
    public void parse_validPanoramaSystemMessage_returnsCorrectSource() {
        final PaloAltoMessageBase result = parser.parse(PANORAMA_SYSTEM_MESSAGE, DateTimeZone.UTC);

        assertThat(result).isNotNull();
        assertThat(result.source()).isEqualTo("Panorama-1");
    }

    @Test
    public void parse_panoramaMessageWithNoTimezoneOffset_parsesWithProvidedZone() {
        // Timezone has no offset in the message; the configured zone (America/Chicago = CDT = UTC-5
        // in September) must be applied. Expected UTC result: 2018-09-19T16:50:33Z.
        final PaloAltoMessageBase result = parser.parse(PANORAMA_NO_TZ_MESSAGE, DateTimeZone.forID("America/Chicago"));

        assertThat(result).isNotNull();
        assertThat(result.source()).isEqualTo("Panorama--1");
        final DateTime expected = new DateTime("2018-09-19T16:50:33.000Z", DateTimeZone.UTC);
        assertThat(result.timestamp().withZone(DateTimeZone.UTC)).isEqualTo(expected);
    }

    // -------------------------------------------------------------------------
    // Valid standard-syslog messages — ensure STANDARD_SYSLOG_PARSER fix
    // does not break existing behaviour
    // -------------------------------------------------------------------------

    @Test
    public void parse_validStandardSyslogThreatMessage_returnsCorrectSource() {
        final PaloAltoMessageBase result = parser.parse(STANDARD_SYSLOG_THREAT_MESSAGE, DateTimeZone.UTC);

        assertThat(result).isNotNull();
        assertThat(result.source()).isEqualTo("hq-lx-net-7.dart.org");
    }

    @Test
    public void parse_validStandardSyslogThreatMessage_parsesMultipleFieldsCorrectly() {
        final PaloAltoMessageBase result = parser.parse(STANDARD_SYSLOG_THREAT_MESSAGE, DateTimeZone.UTC);

        assertThat(result).isNotNull();

        // Syslog header fields
        assertThat(result.source()).isEqualTo("hq-lx-net-7.dart.org");
        assertThat(result.panType()).isEqualTo("THREAT");

        // The syslog timestamp carries no year; the parser appends the current year.
        // Assert time-of-day components only so the test stays green across years.
        final DateTime ts = result.timestamp().withZone(DateTimeZone.UTC);
        assertThat(ts.getMonthOfYear()).isEqualTo(8);    // August
        assertThat(ts.getDayOfMonth()).isEqualTo(22);
        assertThat(ts.getHourOfDay()).isEqualTo(11);
        assertThat(ts.getMinuteOfHour()).isEqualTo(21);
        assertThat(ts.getSecondOfMinute()).isEqualTo(4);

        // CSV payload: the raw string forwarded to type-specific parsers
        assertThat(result.payload()).startsWith("1,2018/08/22 11:21:04,013201001141,THREAT");

        // Key CSV field values (0-indexed)
        assertThat(result.fields().get(2)).isEqualTo("013201001141");   // serial_number
        assertThat(result.fields().get(3)).isEqualTo("THREAT");          // type  (== panType)
        assertThat(result.fields().get(4)).isEqualTo("vulnerability");   // subtype
        assertThat(result.fields().get(7)).isEqualTo("10.0.190.116");    // src_addr
        assertThat(result.fields().get(8)).isEqualTo("10.0.2.225");      // dst_addr
        assertThat(result.fields().get(11)).isEqualTo("DMZ-to-LAN");     // rule_name
        assertThat(result.fields().get(14)).isEqualTo("msrpc");          // application
        assertThat(result.fields().get(15)).isEqualTo("vsys1");          // virtual_system
    }

    @Test
    public void parse_validStandardSyslogMessage_doubleSpaceDate_parsesCorrectly() {
        // "Aug  2" — double space before a single-digit day number.
        // The parser normalises "Aug  2" → "Aug 2" before the date formatter runs.
        final PaloAltoMessageBase result = parser.parse(STANDARD_SYSLOG_DOUBLE_SPACE_DATE_MESSAGE, DateTimeZone.UTC);

        assertThat(result).isNotNull();
        assertThat(result.source()).isEqualTo("hq-lx-net-7.dart.org");
        assertThat(result.panType()).isEqualTo("THREAT");

        final DateTime ts = result.timestamp().withZone(DateTimeZone.UTC);
        assertThat(ts.getMonthOfYear()).isEqualTo(8);
        assertThat(ts.getDayOfMonth()).isEqualTo(2);     // single-digit day
        assertThat(ts.getHourOfDay()).isEqualTo(11);
        assertThat(ts.getMinuteOfHour()).isEqualTo(21);
        assertThat(ts.getSecondOfMinute()).isEqualTo(4);

        assertThat(result.fields().get(3)).isEqualTo("THREAT");
        assertThat(result.fields().get(4)).isEqualTo("vulnerability");
        assertThat(result.fields().get(7)).isEqualTo("10.0.190.116");   // src_addr
    }

    // -- PANORAMA_SYSLOG_PARSER behaviour tests --

    @Test
    public void parse_panorama_craftedPayload_singleToken_rejectedImmediately() {
        final String payload = "<14>1 2024-01-01T00:00:00Z";

        assertThatThrownBy(() -> parser.parse(payload, DateTimeZone.UTC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void parse_panorama_craftedPayload_twoTokens_noDelimiter_rejected() {
        final String payload = "<14>1 2024-01-01T00:00:00Z hostname";

        assertThatThrownBy(() -> parser.parse(payload, DateTimeZone.UTC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void parse_panorama_craftedPayload_manyTokens_noDelimiter_rejected() {
        // 500 space-separated tokens, no " - - - - " delimiter
        final String payload = buildCraftedPanoramaPayload(500);

        assertThatThrownBy(() -> parser.parse(payload, DateTimeZone.UTC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void parse_panorama_craftedPayload_largeMessage_completesWithinTimeout() {
        // 32768 tokens with the old (.+?) groups this took ~20 s; with \S+ it completes in <100 ms.
        // assertTimeoutPreemptively interrupts the test thread at the deadline instead
        // of waiting for the regex to finish before checking the elapsed time.
        final String payload = buildCraftedPanoramaPayload(32768);

        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThatThrownBy(() -> parser.parse(payload, DateTimeZone.UTC))
                        .isInstanceOf(IllegalArgumentException.class));
    }

    // -- STANDARD_SYSLOG_PARSER behaviour tests --
    //
    // Note on timing: STANDARD_SYSLOG_PARSER has only one lazy group (.+?) for the
    // hostname, which produces O(n) — not O(n²) — backtracking with the old pattern.
    // That is fast enough on modern hardware that a timing assertion is not a reliable
    // regression guard. The \S+ fix is still correct and eliminates all backtracking;
    // these behavioural assertions verify correctness regardless of speed.

    @Test
    public void parse_standardSyslog_craftedPayload_noCSV_rejected() {
        // Valid syslog date prefix and hostname, but no digit-starting CSV field.
        final String payload = "<14>Aug 22 11:21:04 hostname";

        assertThatThrownBy(() -> parser.parse(payload, DateTimeZone.UTC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void parse_standardSyslog_craftedPayload_manyTokens_noCSV_rejected() {
        // Many space-separated tokens after the date — no digit-starting CSV.
        final String payload = buildCraftedSyslogPayload(500);

        assertThatThrownBy(() -> parser.parse(payload, DateTimeZone.UTC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Crafted Panorama-prefix payload: starts with {@code <14>1 } (valid Panorama leader)
     * followed by {@code tokenCount} space-separated single-character tokens,
     * deliberately omitting the {@code - - - -} delimiter.
     */
    private static String buildCraftedPanoramaPayload(int tokenCount) {
        final StringBuilder sb = new StringBuilder("<14>1 ");
        for (int i = 0; i < tokenCount; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append('x');
        }
        return sb.toString();
    }

    /**
     * Crafted standard-syslog-prefix payload: starts with a valid syslog date header
     * followed by {@code tokenCount} space-separated single-character tokens,
     * deliberately omitting the digit-starting CSV field.
     */
    private static String buildCraftedSyslogPayload(int tokenCount) {
        final StringBuilder sb = new StringBuilder("<14>Aug 22 11:21:04 ");
        for (int i = 0; i < tokenCount; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append('x');
        }
        return sb.toString();
    }
}
