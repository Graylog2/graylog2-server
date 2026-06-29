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
package org.graylog.plugins.pipelineprocessor.functions.syslog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SyslogUtilsTest {

    @ParameterizedTest(name = "level {0} -> \"{1}\"")
    @CsvSource({
            "0, Emergency",
            "1, Alert",
            "2, Critical",
            "3, Error",
            "4, Warning",
            "5, Notice",
            "6, Informational",
            "7, Debug",
    })
    void levelToString_mapsRfc5424Severities(int level, String expected) {
        assertThat(SyslogUtils.levelToString(level)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "out-of-range level {0} -> \"Unknown\"")
    @ValueSource(ints = {-1, 8, 9, 100, Integer.MAX_VALUE, Integer.MIN_VALUE})
    void levelToString_returnsUnknownForOutOfRange(int level) {
        assertThat(SyslogUtils.levelToString(level)).isEqualTo("Unknown");
    }

    @ParameterizedTest(name = "facility {0} -> \"{1}\"")
    @CsvSource({
            "0, kern",
            "1, user",
            "2, mail",
            "3, daemon",
            "4, auth",
            "5, syslog",
            "6, lpr",
            "7, news",
            "8, uucp",
            "9, clock",
            "10, authpriv",
            "11, ftp",
            "12, ntp",
            "13, log audit",
            "14, log alert",
            "15, cron",
            "16, local0",
            "17, local1",
            "18, local2",
            "19, local3",
            "20, local4",
            "21, local5",
            "22, local6",
            "23, local7",
    })
    void facilityToString_mapsRfc5424Facilities(int facility, String expected) {
        assertThat(SyslogUtils.facilityToString(facility)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "out-of-range facility {0} -> \"Unknown\"")
    @ValueSource(ints = {-1, 24, 25, 100, Integer.MAX_VALUE, Integer.MIN_VALUE})
    void facilityToString_returnsUnknownForOutOfRange(int facility) {
        assertThat(SyslogUtils.facilityToString(facility)).isEqualTo("Unknown");
    }

    @ParameterizedTest(name = "priority {0} -> facility {1}, level {2}")
    @CsvSource({
            // priority, expectedFacility, expectedLevel  (RFC 5424: PRI = facility * 8 + severity)
            "0,   0,  0",   // kern / Emergency
            "7,   0,  7",   // kern / Debug
            "8,   1,  0",   // user / Emergency
            "34,  4,  2",   // auth / Critical   (RFC 5424 worked example)
            "165, 20, 5",   // local4 / Notice   (RFC 5424 worked example)
            "191, 23, 7",   // local7 / Debug    (maximum standard PRI)
    })
    void facilityAndLevelFromPriority_decomposePri(int priority, int expectedFacility, int expectedLevel) {
        assertThat(SyslogUtils.facilityFromPriority(priority)).isEqualTo(expectedFacility);
        assertThat(SyslogUtils.levelFromPriority(priority)).isEqualTo(expectedLevel);
    }

    @Test
    void facilityAndLevelFromPriority_recomposeToOriginalPriority() {
        // For every valid PRI value, facility * 8 + level must reconstruct the original priority.
        for (int priority = 0; priority <= 191; priority++) {
            final int facility = SyslogUtils.facilityFromPriority(priority);
            final int level = SyslogUtils.levelFromPriority(priority);
            assertThat((facility << 3) + level)
                    .as("priority %d should round-trip through facility/level", priority)
                    .isEqualTo(priority);
        }
    }
}
