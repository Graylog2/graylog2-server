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

import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.jodatime.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

public class NaturalDateParserTest {
    private NaturalDateParser naturalDateParser;

    @Before
    public void setUp() {
        naturalDateParser = new NaturalDateParser();
    }

    @Test
    public void testParse() throws Exception {
        NaturalDateParser.Result today = naturalDateParser.parse("today");
        assertNotNull(today.getFrom());
        assertNotNull(today.getTo());

        // It's enough if this does not throw exceptions because we are not testing the underlying library.
        naturalDateParser.parse("today");
        naturalDateParser.parse("last week to today");
    }

    @Test(expected = NaturalDateParser.DateNotParsableException.class)
    public void testParseFailsOnUnparsableDate() throws Exception {
        naturalDateParser.parse("LOLWUT");
    }

    @Test(expected = NaturalDateParser.DateNotParsableException.class)
    public void testParseFailsOnEmptyDate() throws Exception {
        naturalDateParser.parse("");
    }

    @Test
    public void testDefaultTZ() throws Exception {
        NaturalDateParser p = new NaturalDateParser();

        NaturalDateParser.Result today = p.parse("today");
        assertThat(today.getFrom()).as("From should not be null").isNotNull();
        assertThat(today.getTo()).as("To should not be null").isNotNull();
        assertThat(today.getDateTimeZone().getID()).as("should have the Etc/UTC as Timezone").isEqualTo("Etc/UTC");
    }

    @Test
    public void testUTCTZ() throws Exception {
        NaturalDateParser p = new NaturalDateParser("Etc/UTC");

        NaturalDateParser.Result today = p.parse("today");
        assertThat(today.getFrom()).as("From should not be null").isNotNull();
        assertThat(today.getTo()).as("To should not be null").isNotNull();
        assertThat(today.getDateTimeZone().getID()).as("should have the Etc/UTC as Timezone").isEqualTo("Etc/UTC");
    }

    @Test
    public void testEuropeBerlinTZ() throws Exception {
        NaturalDateParser p = new NaturalDateParser("Europe/Berlin");

        NaturalDateParser.Result today = p.parse("today");
        assertThat(today.getFrom()).as("From should not be null").isNotNull();
        assertThat(today.getTo()).as("To should not be null").isNotNull();
        assertThat(today.getDateTimeZone().getID()).as("should have the Europe/Berlin as Timezone").isEqualTo("Europe/Berlin");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidTZ() {
        new NaturalDateParser("LOLWut");
    }

    @Test
    public void testTemporalOrder() throws Exception {
        NaturalDateParser p = new NaturalDateParser("Etc/UTC");

        NaturalDateParser.Result result1 = p.parse("last hour");
        assertThat(result1.getFrom()).as("from should be before to in").isBefore(result1.getTo());

        NaturalDateParser.Result result2 = p.parse("last one hour");
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

        NaturalDateParser p = new NaturalDateParser("Europe/Berlin");

        NaturalDateParser.Result today = p.parse("today", reference);
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

        NaturalDateParser p = new NaturalDateParser("Europe/Berlin");

        NaturalDateParser.Result today = p.parse("today", reference);
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

        NaturalDateParser p = new NaturalDateParser("Europe/Berlin");

        NaturalDateParser.Result today = p.parse("today", reference);
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

        NaturalDateParser p = new NaturalDateParser("Europe/Berlin");

        NaturalDateParser.Result today = p.parse("today", reference);
        assertThat(today.getFrom()).as("From should not be null").isNotNull();
        assertThat(today.getTo()).as("To should not be null").isNotNull();
        assertThat(today.getDateTimeZone().getID()).as("should have the Europe/Berlin as Timezone").isEqualTo("Europe/Berlin");
        assertThat(today.getFrom().dayOfMonth().get()).as("should be April, 9").isEqualTo(9);
    }
}
