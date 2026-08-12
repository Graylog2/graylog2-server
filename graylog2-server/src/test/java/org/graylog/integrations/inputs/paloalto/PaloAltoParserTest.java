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
import static org.junit.jupiter.api.Assertions.assertTimeout;

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

    // Panorama message with timestamp that has no timezone offset
    private static final String PANORAMA_NO_TZ_MESSAGE =
            "<14>1 2018-09-19T11:50:33 Panorama--1 - - - - 1,2018/09/19 11:50:33,007255000045716,THREAT,spyware,2049," +
            "2018/09/19 11:50:33,10.20.30.40,10.20.30.40,10.20.30.40,10.20.30.40,HTTPS-strict,,,ssl,vsys1,Public,Public," +
            "ethernet1/1,ethernet1/1,ALK Logging,2018/09/19 11:50:33,201360,1,21131,443,56756,443,0x80403000,tcp,alert," +
            "\"test.com/\",Suspicious TLS Evasion Found(14978),online_test.com,informational,client-to-server," +
            "1007133,0xa000000000000000,10.20.30.40-10.20.30.40,10.20.30.40-10.20.30.40,0,,1204440535977427988,,,0,,,,,," +
            ",,0,13,16,0,0,,Prod--1,,,,,0,,0,,N/A,spyware,AppThreat-8065-5006,0x0,0,4294967295";

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
        // Timestamp: 2018-09-19T11:50:32-05:00 => 2018-09-19T16:50:32Z
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
        final PaloAltoMessageBase result = parser.parse(PANORAMA_NO_TZ_MESSAGE, DateTimeZone.forID("America/Chicago"));

        assertThat(result).isNotNull();
        assertThat(result.source()).isEqualTo("Panorama--1");
    }

    // -------------------------------------------------------------------------
    // Regex regression tests
    // -------------------------------------------------------------------------

    @Test
    public void parse_craftedPanoramaPrefix_missingDelimiter_completesQuickly_small() {
        // 200 space-separated tokens — enough to be slow with old O(n²) regex
        final String payload = buildCraftedPayload(200);

        assertTimeout(Duration.ofSeconds(2), () ->
                assertThatThrownBy(() -> parser.parse(payload, DateTimeZone.UTC))
                        .isInstanceOf(IllegalArgumentException.class));
    }

    @Test
    public void parse_craftedPanoramaPrefix_missingDelimiter_completesQuickly_medium() {
        // 1000 tokens — would take multiple seconds with old regex
        final String payload = buildCraftedPayload(1000);

        assertTimeout(Duration.ofSeconds(2), () ->
                assertThatThrownBy(() -> parser.parse(payload, DateTimeZone.UTC))
                        .isInstanceOf(IllegalArgumentException.class));
    }

    @Test
    public void parse_craftedPanoramaPrefix_missingDelimiter_completesQuickly_large() {
        // 4096 tokens — representative of a ~65 kB message; would hang for
        // ~20 seconds with the old pattern. Must complete in under 2 seconds with fix.
        final String payload = buildCraftedPayload(4096);

        assertTimeout(Duration.ofSeconds(2), () ->
                assertThatThrownBy(() -> parser.parse(payload, DateTimeZone.UTC))
                        .isInstanceOf(IllegalArgumentException.class));
    }

    @Test
    public void parse_craftedPanoramaPrefix_singleToken_rejectedQuickly() {
        // Edge case: only a single token after the syslog version byte — no spaces at all
        final String payload = "<14>1 2024-01-01T00:00:00Z";

        assertThatThrownBy(() -> parser.parse(payload, DateTimeZone.UTC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void parse_craftedPanoramaPrefix_twoTokensNoDelimiter_rejectedQuickly() {
        // Two tokens but no - - - - delimiter
        final String payload = "<14>1 2024-01-01T00:00:00Z hostname";

        assertThatThrownBy(() -> parser.parse(payload, DateTimeZone.UTC))
                .isInstanceOf(IllegalArgumentException.class);
    }


    /**
     * Builds a crafted syslog message that starts with a valid Panorama-compatible
     * prefix ({@code <14>1 }) but deliberately omits the {@code - - - -} delimiter.
     * The body consists of {@code tokenCount} space-separated single-character tokens.
     */
    private static String buildCraftedPayload(int tokenCount) {
        final StringBuilder sb = new StringBuilder("<14>1 ");
        for (int i = 0; i < tokenCount; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append('x');
        }
        return sb.toString();
    }
}
