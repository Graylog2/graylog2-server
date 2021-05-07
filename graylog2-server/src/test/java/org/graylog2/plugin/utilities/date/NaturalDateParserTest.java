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
import org.joda.time.format.DateTimeFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.jodatime.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the NaturalDate parser. Currently uses Natty, which has some known flaws.
 *
 * Natty/Naturaldateparser related issues from github:
 *
 * https://github.com/Graylog2/graylog2-server/issues/10004
 * https://github.com/Graylog2/graylog2-server/issues/8556
 * https://github.com/Graylog2/graylog2-server/issues/8263
 * https://github.com/Graylog2/graylog2-server/issues/6857
 *
 * relevant Natty issues:
 * https://github.com/joestelmach/natty/issues
 *
 * Things to discuss:
 * - why does Natty start the week on saturday?
 * https://www.timeanddate.com/calendar/days/#:~:text=According%20to%20international%20standard%20ISO,last%20day%20of%20the%20week.
 * - should we be able to set this to sun/mon depending on the tz/country?
 *
 *
 * TODO: when we align to day start/end in the future, all timestamps have to be updated
 */
public class NaturalDateParserTest {
    private NaturalDateParser naturalDateParser;
    private NaturalDateParser naturalDateParserAntarctica;
    private NaturalDateParser naturalDateParserUtc;

    @BeforeEach
    public void setUp() {
        naturalDateParser = new NaturalDateParser();
        naturalDateParserUtc = new NaturalDateParser("Etc/UTC");
        naturalDateParserAntarctica = new NaturalDateParser("Antarctica/Palmer");
    }

    @Test
    public void testParse() throws Exception {
        NaturalDateParser.Result today = naturalDateParser.parse("today");
        assertNotNull(today.getFrom());
        assertNotNull(today.getTo());

        naturalDateParser.parse("today");
        naturalDateParser.parse("last week to today");
    }

    @Test
    public void testParseFailsOnUnparsableDate() throws Exception {
        assertThrows(NaturalDateParser.DateNotParsableException.class, () -> {
            naturalDateParser.parse("LOLWUT");
        });
    }

    @Test
    public void testParseFailsOnEmptyDate() throws Exception {
        assertThrows(NaturalDateParser.DateNotParsableException.class, () -> {
            naturalDateParser.parse("");
        });
    }

    @Test
    public void testDefaultTZ() throws Exception {
        NaturalDateParser.Result today = naturalDateParser.parse("today");
        assertThat(today.getFrom()).as("From should not be null").isNotNull();
        assertThat(today.getTo()).as("To should not be null").isNotNull();
        assertThat(today.getDateTimeZone().getID()).as("should have the Etc/UTC as Timezone").isEqualTo("Etc/UTC");
    }

    @Test
    public void testUTCTZ() throws Exception {
        NaturalDateParser.Result today = naturalDateParserUtc.parse("today");
        assertThat(today.getFrom()).as("From should not be null").isNotNull();
        assertThat(today.getTo()).as("To should not be null").isNotNull();
        assertThat(today.getDateTimeZone().getID()).as("should have the Etc/UTC as Timezone").isEqualTo("Etc/UTC");
    }

    @Test
    public void testAntarcticaTZ() throws Exception {
        NaturalDateParser.Result today = naturalDateParserAntarctica.parse("today");
        assertThat(today.getFrom()).as("From should not be null").isNotNull();
        assertThat(today.getTo()).as("To should not be null").isNotNull();
        assertThat(today.getDateTimeZone().getID()).as("should have the Antarctica/Palmer as Timezone").isEqualTo("Antarctica/Palmer");
    }

    @Test
    public void testInvalidTZ() {
        assertThrows(IllegalArgumentException.class, () -> {
            new NaturalDateParser("LOLWut");
        });
    }

    @Test
    public void testTemporalOrder() throws Exception {
        NaturalDateParser.Result result1 = naturalDateParserUtc.parse("last hour");
        assertThat(result1.getFrom()).as("from should be before to in").isBefore(result1.getTo());

        NaturalDateParser.Result result2 = naturalDateParserUtc.parse("last one hour");
        assertThat(result2.getFrom()).as("from should be before to in").isBefore(result2.getTo());
    }

    // https://github.com/Graylog2/graylog2-server/issues/1226
    @Test
    public void issue1226() throws Exception {
        NaturalDateParser.Result result99days = naturalDateParser.parse("last 99 days");
        assertThat(result99days.getFrom()).isEqualToIgnoringMillis(result99days.getTo().minusDays(99));

        NaturalDateParser.Result result100days = naturalDateParser.parse("last 100 days");
        assertThat(result100days.getFrom()).isEqualToIgnoringMillis(result100days.getTo().minusDays(100));

        NaturalDateParser.Result result101days = naturalDateParser.parse("last 101 days");
        assertThat(result101days.getFrom()).isEqualToIgnoringMillis(result101days.getTo().minusDays(101));
    }

    @Test
    public void testThisMonth() throws Exception {
        DateTime reference = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("12.06.2021 09:45:23");
        NaturalDateParser.Result result = naturalDateParser.parse("this month", reference.toDate());
        assertThat(result.getFrom()).as("is the same as the reference date").isEqualTo(reference);

//        DateTime first = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("01.06.2021 09:45:23");
//        DateTime firstNextMonth = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("01.07.2021 09:45:23");
//        assertThat(result.getFrom()).as("should start at the beginning of the month").isEqualTo(first);
//        assertThat(result.getTo()).as("should be the 1st day of the following month").isEqualTo(firstNextMonth);
    }

    @Test
    public void testMondayToFriday() throws Exception {
        DateTime reference = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("12.06.2021 09:45:23");
        DateTime monday = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("14.06.2021 09:45:23");
        DateTime friday = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("18.06.2021 09:45:23");

        NaturalDateParser.Result result = naturalDateParser.parse("monday to friday", reference.toDate());
        assertThat(result.getFrom()).as("should be Monday, 14.").isEqualTo(monday);
        assertThat(result.getTo()).as("should be Friday, 18.").isEqualTo(friday);

        /* fails with current Natty implementation
        DateTime lastMonday = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("06.06.2021 09:45:23");
        DateTime lastFriday = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("11.06.2021 09:45:23");

        result = naturalDateParser.parse("last monday to friday", reference.toDate());
        assertThat(result.getFrom()).as("should be Monday, 06.").isEqualTo(lastMonday);
        assertThat(result.getTo()).as("should be Friday, 11.").isEqualTo(lastFriday);
        */

        result = naturalDateParser.parse("next monday to friday", reference.toDate());
        assertThat(result.getFrom()).as("should be Monday, 14.").isEqualTo(monday);
        assertThat(result.getTo()).as("should be Friday, 18.").isEqualTo(friday);

        reference = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("08.06.2021 09:45:23");
        result = naturalDateParser.parse("next monday to friday", reference.toDate());
        assertThat(result.getFrom()).as("should be Monday, 14.").isEqualTo(monday);
        assertThat(result.getTo()).as("should be Friday, 18.").isEqualTo(friday);

        /* fails with current Natty implementaiton
        reference = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("14.06.2021 09:45:23");
        result = naturalDateParser.parse("monday to friday", reference.toDate());
        assertThat(result.getFrom()).as("should be Monday, 14.").isEqualTo(monday);
        assertThat(result.getTo()).as("should be Friday, 18.").isEqualTo(friday);
         */
    }

    /**
     *  Bug from https://github.com/joestelmach/natty/issues
     *  Test is ignored because it fails, test can be used in the future to check if it's solved
     */
    @Test
    @Disabled
    public void testNatty53() throws Exception {
        DateTime reference = DateTime.now(DateTimeZone.UTC);
        NaturalDateParser.Result natty53 = naturalDateParser.parse("Tue Jan 12 00:00:00 UTC 2016", reference.toDate());
        assertThat(natty53.getFrom().getYear()).as("from should be 2016").isEqualTo(2016);
        assertThat(natty53.getTo()).as("to should be the reference date").isEqualTo(reference);
    }

    @Test
    public void testLast4hours() throws Exception {
        DateTime reference = DateTime.now(DateTimeZone.UTC);
        NaturalDateParser.Result last4 = naturalDateParser.parse("last 4 hours", reference.toDate());
        assertThat(last4.getFrom()).as("from should be exactly 4 hours in the past").isEqualTo(reference.minusHours(4));
        assertThat(last4.getTo()).as("to should be the reference date").isEqualTo(reference);
    }

    @Test
    public void testLast4hoursTZAntarctica() throws Exception {
        DateTime reference = DateTime.now(DateTimeZone.UTC);
        NaturalDateParser.Result last4 = naturalDateParserAntarctica.parse("last 4 hours", reference.toDate());
        assertThat(last4.getFrom()).as("from should be exactly 4 hours in the past").isEqualTo(reference.minusHours(4));
        assertThat(last4.getTo()).as("to should be the reference date").isEqualTo(reference);
    }

    @Test
    public void testLast4hoursArtificialReference() throws Exception {
        DateTime reference = DateTime.now(DateTimeZone.UTC).minusHours(7);
        NaturalDateParser.Result last4 = naturalDateParserAntarctica.parse("last 4 hours", reference.toDate());
        assertThat(last4.getFrom()).as("from should be exactly 4 hours in the past").isEqualTo(reference.minusHours(4));
        assertThat(last4.getTo()).as("to should be the reference date").isEqualTo(reference);

        reference = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("12.06.2021 09:45:23");
        DateTime fourHoursAgo = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("12.06.2021 05:45:23");
        last4 = naturalDateParserAntarctica.parse("last 4 hours", reference.toDate());
        assertThat(last4.getFrom()).as("from should be exactly 4 hours in the past").isEqualTo(fourHoursAgo);
        assertThat(last4.getTo()).as("to should be the reference date").isEqualTo(reference);
    }

    @Test
    public void testLast4hoursArtificialReferenceDSTChange() throws Exception {
        DateTime reference = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZone(DateTimeZone.forID("Antarctica/Palmer")).parseDateTime("28.03.2021 03:45:23");
        NaturalDateParser.Result last4 = naturalDateParserAntarctica.parse("last 4 hours", reference.toDate());
        assertThat(last4.getFrom()).as("from should be exactly 4 hours in the past").isEqualTo(reference.minusHours(4));
        assertThat(last4.getTo()).as("to should be the reference date").isEqualTo(reference);

        reference = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZone(DateTimeZone.forID("Antarctica/Palmer")).parseDateTime("31.10.2021 03:45:23");
        last4 = naturalDateParserAntarctica.parse("last 4 hours", reference.toDate());
        assertThat(last4.getFrom()).as("from should be exactly 4 hours in the past").isEqualTo(reference.minusHours(4));
        assertThat(last4.getTo()).as("to should be the reference date").isEqualTo(reference);
    }

    @Test
    public void testParseToday() throws Exception {
        DateTime reference = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("12.06.2021 09:45:23");
        NaturalDateParser.Result result = naturalDateParser.parse("today", reference.toDate());

        // TODO: in the future, this should compare to "12.06.2021 00:00:00"
        assertThat(result.getFrom()).as("should be equal to").isEqualTo(reference);
        // TODO: in the future, this should compare to "13.06.2021 00:00:00"
        assertThat(result.getTo()).as("should differ from").isNotEqualTo(reference);
     }

    @Test
    public void testParseLastMonday() throws Exception {
        DateTime reference = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("12.06.2021 09:45:23");
        NaturalDateParser.Result result = naturalDateParser.parse("last monday", reference.toDate());

        DateTime lastMonday = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("31.05.2021 09:45:23");

        assertThat(result.getFrom()).as("should be equal to").isEqualTo(lastMonday);
        assertThat(result.getTo()).as("should differ from").isNotEqualTo(lastMonday);
    }

    @Test
    public void testParseLastWeek() throws Exception {
        DateTime reference = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("12.06.2021 09:45:23");
        NaturalDateParser.Result result = naturalDateParser.parse("last week", reference.toDate());

        DateTime lastMonday = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("05.06.2021 09:45:23");

        assertThat(result.getFrom()).as("should be equal to").isEqualTo(lastMonday);
        assertThat(result.getTo()).as("should differ from").isNotEqualTo(lastMonday);
    }

    @Test
    public void testParseMondayToFriday() throws Exception {
        DateTime reference = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("12.06.2021 09:45:23");
        NaturalDateParser.Result result = naturalDateParser.parse("monday to friday", reference.toDate());

        DateTime monday = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("14.06.2021 09:45:23");
        DateTime friday = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("18.06.2021 09:45:23");

        assertThat(result.getFrom()).as("should be equal to").isEqualTo(monday);
        assertThat(result.getTo()).as("should be equal to").isEqualTo(friday);
    }
}
