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

        final DateTime expectedOutput = new DateTime(currentTimeMillis);
        assertThat(output).isEqualTo(expectedOutput);
    }

    @Test
    void convertFromZonedDateTime() {
        final ZoneId europeBerlin = ZoneId.of("America/New_York");
        final ZonedDateTime input = ZonedDateTime.of(2020, 10, 24, 10, 59, 0, 0, europeBerlin);

        final DateTime output = DateTimeConverter.convertToDateTime(input);

        final DateTime expectedOutput = new DateTime(2020, 10, 24, 10, 59, DateTimeZone.forID("America/New_York"));
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
    void convertFromLocalDateTime() {
        final LocalDateTime input = LocalDateTime.of(2021, Month.AUGUST, 19, 12, 0);

        final DateTime output = DateTimeConverter.convertToDateTime(input);

        // represented in local timezone, but then converted to UTC
        //final DateTime expectedOutput = new DateTime(2021, 8, 19, 12, 0).withZone(DateTimeZone.UTC);
        final DateTime expectedOutput = new DateTime(2021, 8, 19, 12, 0);
        assertThat(output).isEqualTo(expectedOutput);
    }

    @Test
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

        final DateTime expectedOutput = new DateTime(currentTimeMillis);
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
