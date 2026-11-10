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
package org.graylog2.plugin.utilities.date;

import org.graylog2.shared.SuppressForbidden;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateTimeConverterTest {

    @Test
    void convertFromDateTime() {
        final DateTime input = new DateTime(2021, 8, 19, 12, 0, DateTimeZone.UTC);

        final DateTime output = DateTimeConverter.convertToDateTime(input);

        assertThat(output).isEqualTo(input);
    }

    @Test
    void convertFromDate() {
        final long currentTimeMillis = System.currentTimeMillis();
        final Date input = new Date(currentTimeMillis);

        final DateTime output = DateTimeConverter.convertToDateTime(input);

        final DateTime expectedOutput = new DateTime(currentTimeMillis, DateTimeZone.UTC);
        assertThat(output).isEqualTo(expectedOutput);
    }

    @Test
    void convertFromZonedDateTimeNewYork() {
        final ZoneId newYork = ZoneId.of("America/New_York");
        final ZonedDateTime input = ZonedDateTime.of(2020, 10, 24, 10, 59, 0, 0, newYork);

        final DateTime output = DateTimeConverter.convertToDateTime(input);

        final DateTime expectedOutput = new DateTime(2020, 10, 24, 10, 59, DateTimeZone.forID("America/New_York"));
        assertThat(output).isEqualTo(expectedOutput);
    }

    @Test
    void convertFromZonedDateTimeUTC() {
        final ZonedDateTime input = ZonedDateTime.of(2020, 10, 24, 10, 59, 0, 0, ZoneOffset.UTC);

        final DateTime output = DateTimeConverter.convertToDateTime(input);

        final DateTime expectedOutput = new DateTime(2020, 10, 24, 10, 59, DateTimeZone.UTC);
        assertThat(output).isEqualTo(expectedOutput);
    }

    @Test
    void convertFromOffsetDateTime() {
        final OffsetDateTime input = OffsetDateTime.of(2021, 11, 20, 14, 50, 10, 0, ZoneOffset.UTC);

        final DateTime output = DateTimeConverter.convertToDateTime(input);

        final DateTime expectedOutput = new DateTime(2021, 11, 20, 14, 50, 10, DateTimeZone.UTC);
        assertThat(output).isEqualTo(expectedOutput);
    }

    @Test
    void convertFromOffsetDateTimeWithNonUTCOffset() {
        final OffsetDateTime input = OffsetDateTime.of(2021, 11, 20, 14, 50, 10, 0, ZoneOffset.of("+1"));

        final DateTime output = DateTimeConverter.convertToDateTime(input);

        final DateTime expectedOutput = new DateTime(2021, 11, 20, 13, 50, 10, DateTimeZone.UTC);
        assertThat(output).isEqualTo(expectedOutput);
    }

    @Test
    @SuppressForbidden("Comparing twice with default timezone is okay in tests")
    void convertFromLocalDateTime() {
        final LocalDateTime input = LocalDateTime.of(2021, Month.AUGUST, 19, 12, 0);

        final DateTime output = DateTimeConverter.convertToDateTime(input);

        // both input and output are represented with local timezone.
        final DateTime expectedOutput = new DateTime(2021, 8, 19, 12, 0);
        assertThat(output).isEqualTo(expectedOutput);
    }

    @Test
    @SuppressForbidden("Comparing twice with default timezone is okay in tests")
    void convertFromLocalDate() {
        final LocalDate input = LocalDate.of(2021, Month.SEPTEMBER, 2);

        final DateTime output = DateTimeConverter.convertToDateTime(input);

        final DateTime expectedOutput = new DateTime(2021, 9, 2, 0, 0);
        assertThat(output).isEqualTo(expectedOutput);
    }

    @Test
    void convertFromInstant() {
        final long currentTimeMillis = System.currentTimeMillis();
        final Instant input = Instant.ofEpochMilli(currentTimeMillis);

        final DateTime output = DateTimeConverter.convertToDateTime(input);

        final DateTime expectedOutput = new DateTime(currentTimeMillis, DateTimeZone.UTC);
        assertThat(output).isEqualTo(expectedOutput);
    }

    @Test
    void convertFromESDateString() {
        //ES_DATE_FORMAT_JODA = "yyyy-MM-dd HH:mm:ss.SSS";
        final String input = "2021-09-04 13:14:15.666";

        final DateTime output = DateTimeConverter.convertToDateTime(input);
        final DateTime expectedOutput = new DateTime(2021, 9, 4, 13, 14, 15, 666, DateTimeZone.UTC);
        assertThat(output).isEqualTo(expectedOutput);
    }

    @Test
    void convertFromESDateStringWithoutMillis() {
        //ES_DATE_FORMAT_NO_MS = "yyyy-MM-dd HH:mm:ss";
        final String input = "2026-05-18 08:57:55";

        final DateTime output = DateTimeConverter.convertToDateTime(input);
        final DateTime expectedOutput = new DateTime(2026, 5, 18, 8, 57, 55, DateTimeZone.UTC);
        assertThat(output).isEqualTo(expectedOutput);
    }

    @Test
    void convertFromIsoStringWithOffset() {
        // yyyy-MM-dd'T'HH:mm:ssZZ - +0200 means the instant is 06:57:55 UTC
        final String input = "2026-05-18T08:57:55+0200";

        final DateTime output = DateTimeConverter.convertToDateTime(input);
        final DateTime expectedOutput = new DateTime(2026, 5, 18, 6, 57, 55, DateTimeZone.UTC);
        assertThat(output).isEqualTo(expectedOutput);
    }

    @Test
    void convertFromIsoStringInUtcWithMillis() {
        // yyyy-MM-dd'T'HH:mm:ss.SSSZZ
        final String input = "2026-05-18T08:57:55.123Z";

        final DateTime output = DateTimeConverter.convertToDateTime(input);
        final DateTime expectedOutput = new DateTime(2026, 5, 18, 8, 57, 55, 123, DateTimeZone.UTC);
        assertThat(output).isEqualTo(expectedOutput);
    }

    @Test
    void convertFromIsoStringWithoutOffsetIsRejected() {
        // A value without zone/offset is ambiguous and must be rejected.
        assertThatThrownBy(() -> DateTimeConverter.convertToDateTime("2026-05-18T08:57:55"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void convertFromIsoStringWithMillisWithoutOffsetIsRejected() {
        // The millis variant routes through a different formatter than the no-ms case above, but a value
        // without zone/offset is equally ambiguous and must be rejected.
        assertThatThrownBy(() -> DateTimeConverter.convertToDateTime("2026-05-18T08:57:55.123"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void convertFromUnsupportedStringFormat() {
        assertThatThrownBy(() -> DateTimeConverter.convertToDateTime("not-a-timestamp"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported timestamp string format");
    }

    @Test
    void convertFromInvalidType() {
        assertThatThrownBy(() -> DateTimeConverter.convertToDateTime(1234))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Value of invalid type <Integer> provided");
    }

    @Test
    void convertFromInvalidDateString() {
        assertThatThrownBy(() -> DateTimeConverter.convertToDateTime("2031-14-14 13:14:10.123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Cannot parse \"2031-14-14");
    }
}
