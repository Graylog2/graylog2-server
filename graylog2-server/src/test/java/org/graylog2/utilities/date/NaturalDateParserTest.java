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
package org.graylog2.utilities.date;

import org.graylog2.plugin.utilities.date.NaturalDateParser;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NaturalDateParserTest {
    private NaturalDateParser naturalDateParser;

    @Before
    public void setUp() {
        naturalDateParser = new NaturalDateParser("Etc/UTC");
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

    @Test
    public void testParseAlignToStartOfDay() throws Exception {
        final DateTimeFormatter df = DateTimeFormat.forPattern("HH:mm:ss");
        final String[] testsThatAlignToStartOfDay = { "yesterday", "the day before yesterday", "today",
                "monday", "monday to friday", "last week" };

        for(String test: testsThatAlignToStartOfDay) {
            NaturalDateParser.Result result = naturalDateParser.parse(test);
            assertNotNull(result.getFrom());
            assertNotNull(result.getTo());

            assertThat(df.print(result.getFrom())).as("time part of date should equal 00:00:00 in").isEqualTo("00:00:00");
            assertThat(df.print(result.getTo())).as("time part of date should equal 00:00:00 in").isEqualTo("00:00:00");
        }
    }

    @Test
    public void testParseAlignToStartOfDayEuropeBerlin() throws Exception {
        final NaturalDateParser naturalDateParser = new NaturalDateParser("Europe/Berlin");
        final DateTimeFormatter df = DateTimeFormat.forPattern("HH:mm:ss");
        final String[] testsThatAlignToStartOfDay = { "yesterday", "the day before yesterday", "today",
                "monday", "monday to friday", "last week" };

        for(String test: testsThatAlignToStartOfDay) {
            NaturalDateParser.Result result = naturalDateParser.parse(test);
            assertNotNull(result.getFrom());
            assertNotNull(result.getTo());

            assertThat(df.print(result.getFrom())).as("time part of date should equal 00:00:00 in").isEqualTo("00:00:00");
            assertThat(df.print(result.getTo())).as("time part of date should equal 00:00:00 in").isEqualTo("00:00:00");
        }
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
    @Ignore
    public void testTemporalOrder() throws Exception {
        NaturalDateParser p = new NaturalDateParser("Etc/UTC");

        NaturalDateParser.Result result1 = p.parse("last hour");
        assertTrue(result1.getFrom().compareTo(result1.getTo()) < 0);

        NaturalDateParser.Result result2 = p.parse("last one hour");
        assertTrue(result2.getFrom().compareTo(result2.getTo()) < 0);
    }

    // https://github.com/Graylog2/graylog2-server/issues/1226
    // proposed change of how it works, is that the to-part is actually the first moment of the next day so that comparisons work
    @Test
    public void issue1226() throws Exception {
        NaturalDateParser.Result result99days = naturalDateParser.parse("last 99 days");
        org.assertj.jodatime.api.Assertions.assertThat(result99days.getFrom()).isEqualToIgnoringMillis(result99days.getTo().minusDays(100));

        NaturalDateParser.Result result100days = naturalDateParser.parse("last 100 days");
        org.assertj.jodatime.api.Assertions.assertThat(result100days.getFrom()).isEqualToIgnoringMillis(result100days.getTo().minusDays(101));

        NaturalDateParser.Result result101days = naturalDateParser.parse("last 101 days");
        org.assertj.jodatime.api.Assertions.assertThat(result101days.getFrom()).isEqualToIgnoringMillis(result101days.getTo().minusDays(102));
    }
}
