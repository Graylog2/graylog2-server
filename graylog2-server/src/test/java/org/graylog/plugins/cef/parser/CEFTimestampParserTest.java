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
package org.graylog.plugins.cef.parser;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class CEFTimestampParserTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"invalid", null, DateTimeZone.UTC, Locale.ROOT},
                {"1505741140123", new DateTime(2017, 9, 18, 13, 25, 40, 123, DateTimeZone.UTC), DateTimeZone.UTC, Locale.ROOT},
                {"2017-09-18T13:25:40.123+01:00", new DateTime(2017, 9, 18, 13, 25, 40, 123, DateTimeZone.forOffsetHours(1)), DateTimeZone.forOffsetHours(1), Locale.ROOT},
                {"2017-09-18T13:25:40+01:00", new DateTime(2017, 9, 18, 13, 25, 40, 0, DateTimeZone.forOffsetHours(1)), DateTimeZone.forOffsetHours(1), Locale.ROOT},
                {"20170918T132540.123+01:00", new DateTime(2017, 9, 18, 13, 25, 40, 123, DateTimeZone.forOffsetHours(1)), DateTimeZone.forOffsetHours(1), Locale.ROOT},
                {"20170918T132540+01:00", new DateTime(2017, 9, 18, 13, 25, 40, 0, DateTimeZone.forOffsetHours(1)), DateTimeZone.forOffsetHours(1), Locale.ROOT},
                {"Sep 18 13:25:40.123 CET", new DateTime(2000, 9, 18, 13, 25, 40, 123, DateTimeZone.forID("CET")), DateTimeZone.forID("CET"), Locale.ROOT},
                {"Sep 18 13:25:40.123", new DateTime(2000, 9, 18, 13, 25, 40, 123, DateTimeZone.forID("CET")), DateTimeZone.forID("CET"), Locale.ROOT},
                {"Sep 18 13:25:40 CET", new DateTime(2000, 9, 18, 13, 25, 40, 0, DateTimeZone.forID("CET")), DateTimeZone.forID("CET"), Locale.ROOT},
                {"Sep 18 13:25:40", new DateTime(2000, 9, 18, 13, 25, 40, 0, DateTimeZone.forID("CET")), DateTimeZone.forID("CET"), Locale.ROOT},
                {"Sep 18 2017 13:25:40.123 CET", new DateTime(2017, 9, 18, 13, 25, 40, 123, DateTimeZone.forID("CET")), DateTimeZone.forID("CET"), Locale.ROOT},
                {"Sep 18 2017 13:25:40.123", new DateTime(2017, 9, 18, 13, 25, 40, 123, DateTimeZone.forID("CET")), DateTimeZone.forID("CET"), Locale.ROOT},
                {"Sep 18 2017 13:25:40 CET", new DateTime(2017, 9, 18, 13, 25, 40, 0, DateTimeZone.forID("CET")), DateTimeZone.forID("CET"), Locale.ROOT},
                {"Sep 18 2017 13:25:40", new DateTime(2017, 9, 18, 13, 25, 40, 0, DateTimeZone.forID("CET")), DateTimeZone.forID("CET"), Locale.ROOT},
                {"Mai 18 2017 13:25:40.123 CET", new DateTime(2017, 5, 18, 13, 25, 40, 123, DateTimeZone.forID("CET")), DateTimeZone.forID("CET"), Locale.GERMAN},
        });
    }

    private final String testString;
    private final DateTime expectedDateTime;
    private final DateTimeZone timeZone;
    private final Locale locale;

    public CEFTimestampParserTest(String testString, DateTime expectedDateTime, DateTimeZone timeZone, Locale locale) {
        this.testString = testString;
        this.expectedDateTime = expectedDateTime;
        this.timeZone = timeZone;
        this.locale = locale;
    }

    @Test
    public void parseWithTimeZoneAndLocale() throws Exception {
        assertEquals(testString, expectedDateTime, CEFTimestampParser.parse(testString, timeZone, locale));
    }
}