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
import org.joda.time.format.DateTimeFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.jodatime.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NaturalDateParserTest {
    private NaturalDateParser naturalDateParser;
    private NaturalDateParser naturalDateParserBerlin;
    private NaturalDateParser naturalDateParserUtc;

    @BeforeEach
    public void setUp() {
        naturalDateParser = new NaturalDateParser();
        naturalDateParserUtc = new NaturalDateParser("Etc/UTC");
        naturalDateParserBerlin = new NaturalDateParser("Europe/Berlin");
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
    public void testEuropeBerlinTZ() throws Exception {
        NaturalDateParser.Result today = naturalDateParserBerlin.parse("today");
        assertThat(today.getFrom()).as("From should not be null").isNotNull();
        assertThat(today.getTo()).as("To should not be null").isNotNull();
        assertThat(today.getDateTimeZone().getID()).as("should have the Europe/Berlin as Timezone").isEqualTo("Europe/Berlin");
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

    // Ausserdem Zeiträume usw. bei Sommer/Winterzeitumstellung usw
    // Warum startet Natty die Woche an einem Samstag?

    /*
    Natty/Naturaldateparser related issues from github:

https://github.com/Graylog2/graylog2-server/issues/10004
https://github.com/Graylog2/graylog2-server/issues/8556
https://github.com/Graylog2/graylog2-server/issues/8263
https://github.com/Graylog2/graylog2-server/issues/6857


       relevant Natty issues:
       https://github.com/joestelmach/natty/issues
     */

    @Test
    public void testNatty53() throws Exception {
        DateTime reference = DateTime.now();
        NaturalDateParser.Result last4 = naturalDateParser.parse("Tue Jan 12 00:00:00 UTC 2016", reference.toDate());
        assertThat(last4.getFrom()).as("from should be exactly 4 hours in the past").isEqualTo(reference.minusHours(4));
        assertThat(last4.getTo()).as("to should be the reference date").isEqualTo(reference);
    }

    @Test
    public void testLast4hours() throws Exception {
        DateTime reference = DateTime.now();
        NaturalDateParser.Result last4 = naturalDateParser.parse("last 4 hours", reference.toDate());
        assertThat(last4.getFrom()).as("from should be exactly 4 hours in the past").isEqualTo(reference.minusHours(4));
        assertThat(last4.getTo()).as("to should be the reference date").isEqualTo(reference);
    }

    @Test
    public void testLast4hoursTZBerlin() throws Exception {
        DateTime reference = DateTime.now();
        NaturalDateParser.Result last4 = naturalDateParserBerlin.parse("last 4 hours", reference.toDate());
        assertThat(last4.getFrom()).as("from should be exactly 4 hours in the past").isEqualTo(reference.minusHours(4));
        assertThat(last4.getTo()).as("to should be the reference date").isEqualTo(reference);
    }

    @Test
    public void testLast4hoursArtificialReference() throws Exception {
        DateTime reference = DateTime.now().minusHours(7);
        NaturalDateParser.Result last4 = naturalDateParserBerlin.parse("last 4 hours", reference.toDate());
        assertThat(last4.getFrom()).as("from should be exactly 4 hours in the past").isEqualTo(reference.minusHours(4));
        assertThat(last4.getTo()).as("to should be the reference date").isEqualTo(reference);
    }

    // TODO: these tests will have to change in the near future when the alignment to start/end of day will be included

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

        DateTime lastMonday = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("07.06.2021 09:45:23");

        // TODO: in the future, this should compare to "07.06.2021 00:00:00"
        assertThat(result.getFrom()).as("should be equal to").isEqualTo(lastMonday);
        // TODO: in the future, this should compare to "08.06.2021 00:00:00"
        assertThat(result.getTo()).as("should differ from").isNotEqualTo(lastMonday);
    }

    @Test
    public void testParseLastWeek() throws Exception {
        DateTime reference = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("12.06.2021 09:45:23");
        NaturalDateParser.Result result = naturalDateParser.parse("last week", reference.toDate());

        DateTime lastMonday = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("07.06.2021 09:45:23");

        // TODO: in the future, this should compare to "07.06.2021 00:00:00"
        assertThat(result.getFrom()).as("should be equal to").isEqualTo(lastMonday);
        // TODO: in the future, this should compare to "08.06.2021 00:00:00"
        assertThat(result.getTo()).as("should differ from").isNotEqualTo(lastMonday);
    }

    @Test
    public void testParseMondayToFriday() throws Exception {
        DateTime reference = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("12.06.2021 09:45:23");
        NaturalDateParser.Result result = naturalDateParser.parse("monday to friday", reference.toDate());

        DateTime monday = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("14.06.2021 09:45:23");
        DateTime friday = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").withZoneUTC().parseDateTime("18.06.2021 09:45:23");

        // TODO: in the future, this should compare to "14.06.2021 00:00:00"
        assertThat(result.getFrom()).as("should be equal to").isEqualTo(monday);
        // TODO: in the future, this should compare to "19.06.2021 00:00:00"
        assertThat(result.getTo()).as("should be equal to").isEqualTo(friday);
    }

    // TODO: end of: these tests will have to change in the near future when the alignment to start/end of day will be included

    /**
     * Test to validate, that Natty's reference date/timezone (usually taken from the VM's env) and
     * the user's requested timezone does not accidentially roll over during parsing of e.g. "today" (full day parsing).
     * The day of the point of time where the parsing occurs differs in relation to the timezone.
     *
     * Here: VM is almost midnight at Apr, 9 UTC, User requests "today" at April, 10 in "Europe/Berlin"
     */
    @Test
    public void testNattyReferenceDateDiffersFromUsersTZButDoesNotRollToADifferentDayEndOfDayVMRunsUTC() throws Exception {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date reference = isoFormat.parse("2021-04-09T23:59:00");

        NaturalDateParser.Result today = naturalDateParserBerlin.parse("today", reference);
        assertThat(today.getFrom()).as("From should not be null").isNotNull();
        assertThat(today.getTo()).as("To should not be null").isNotNull();
        assertThat(today.getDateTimeZone().getID()).as("should have the Europe/Berlin as Timezone").isEqualTo("Europe/Berlin");
        assertThat(today.getFrom().dayOfMonth().get()).as("should be April, 10").isEqualTo(10);
    }

    /**
     * Test to validate, that Natty's reference date/timezone (usually taken from the VM's env) and
     * the user's requested timezone does not accidentially roll over during parsing of e.g. "today" (full day parsing).
     * The day of the point of time where the parsing occurs differs in relation to the timezone.
     *
     * Here: VM is as bit after midnight at Apr, 9 UTC, User requests "today" at April, 9 in "Europe/Berlin"
     */
    @Test
    public void testNattyReferenceDateDiffersFromUsersTZButDoesNotRollToADifferentDayStartOfDayVMRunsUTC() throws Exception {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date reference = isoFormat.parse("2021-04-09T00:01:00");


        NaturalDateParser.Result today = naturalDateParserBerlin.parse("today", reference);
        assertThat(today.getFrom()).as("From should not be null").isNotNull();
        assertThat(today.getTo()).as("To should not be null").isNotNull();
        assertThat(today.getDateTimeZone().getID()).as("should have the Europe/Berlin as Timezone").isEqualTo("Europe/Berlin");
        assertThat(today.getFrom().dayOfMonth().get()).as("should be April, 9").isEqualTo(9);
    }

    /**
     * Test to validate, that Natty's reference date/timezone (usually taken from the VM's env) and
     * the user's requested timezone does not accidentially roll over during parsing of e.g. "today" (full day parsing).
     * The day of the point of time where the parsing occurs differs in relation to the timezone.
     *
     * Here: VM is almost midnight at Apr, 9 Europe/Berlin, User requests "today" at April, 9 in "UTC"
     */
    @Test
    public void testNattyReferenceDateDiffersFromUsersTZButDoesNotRollToADifferentDayEndOfDayUserIsUTC() throws Exception {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        isoFormat.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
        Date reference = isoFormat.parse("2021-04-09T23:59:00");

        NaturalDateParser.Result today = naturalDateParserBerlin.parse("today", reference);
        assertThat(today.getFrom()).as("From should not be null").isNotNull();
        assertThat(today.getTo()).as("To should not be null").isNotNull();
        assertThat(today.getDateTimeZone().getID()).as("should have the Europe/Berlin as Timezone").isEqualTo("Europe/Berlin");
        assertThat(today.getFrom().dayOfMonth().get()).as("should be April, 9").isEqualTo(9);
    }

    /**
     * Test to validate, that Natty's reference date/timezone (usually taken from the VM's env) and
     * the user's requested timezone does not accidentially roll over during parsing of e.g. "today" (full day parsing).
     * The day of the point of time where the parsing occurs differs in relation to the timezone.
     *
     * Here: VM is almost midnight at Apr, 9 Europe/Berlin, User requests "today" at April, 9 in "UTC"
     */
    @Test
    public void testNattyReferenceDateDiffersFromUsersTZButDoesNotRollToADifferentDayStartOfDayUserIsUTC() throws Exception {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        isoFormat.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
        Date reference = isoFormat.parse("2021-04-09T00:01:00");

        NaturalDateParser.Result today = naturalDateParserBerlin.parse("today", reference);
        assertThat(today.getFrom()).as("From should not be null").isNotNull();
        assertThat(today.getTo()).as("To should not be null").isNotNull();
        assertThat(today.getDateTimeZone().getID()).as("should have the Europe/Berlin as Timezone").isEqualTo("Europe/Berlin");
        assertThat(today.getFrom().dayOfMonth().get()).as("should be April, 9").isEqualTo(9);
    }
}
