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
package org.graylog2.inputs.converters;

import com.google.common.collect.ImmutableMap;
import org.assertj.jodatime.api.Assertions;
import org.graylog2.plugin.inputs.Converter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FlexibleDateConverterTest {
    @Test
    public void testConvert() throws Exception {
        Converter c = new FlexibleDateConverter(Collections.<String, Object>emptyMap());

        assertNull(c.convert(null));
        assertEquals(null, c.convert(""));
        assertEquals(null, c.convert("foo"));

        // Using startsWith here to avoid time zone problems in tests.
        assertTrue(c.convert("2014-3-12").toString().startsWith("2014-03-12T"));
        assertTrue(c.convert("2014-3-12 12:27").toString().startsWith("2014-03-12T12:27:00.000"));
        assertTrue(c.convert("Mar 12").toString().contains("-03-12T"));
        assertTrue(c.convert("Mar 12 2pm").toString().contains("-03-12T14:00:00.000"));
        assertTrue(c.convert("Mar 12 14:45:38").toString().contains("-03-12T14:45:38.000"));
        assertTrue(c.convert("Mar 2 13:48:18").toString().contains("-03-02T13:48:18.000"));
    }

    @Test
    public void convertObeysTimeZone() throws Exception {
        Converter c = new FlexibleDateConverter(ImmutableMap.<String, Object>of("time_zone", "+12:00"));

        final DateTime dateOnly = (DateTime) c.convert("2014-3-12");
        assertThat(dateOnly.getZone()).isEqualTo(DateTimeZone.forOffsetHours(12));
        Assertions.assertThat(dateOnly)
                .isAfterOrEqualTo(new DateTime(2014, 3, 12, 0, 0, DateTimeZone.forOffsetHours(12)))
                .isBefore(new DateTime(2014, 3, 13, 0, 0, DateTimeZone.forOffsetHours(12)));

        final DateTime dateTime = (DateTime) c.convert("2014-3-12 12:34");
        assertThat(dateTime.getZone()).isEqualTo(DateTimeZone.forOffsetHours(12));
        Assertions.assertThat(dateTime)
                .isEqualTo(new DateTime(2014, 3, 12, 12, 34, DateTimeZone.forOffsetHours(12)));

        final DateTime textualDateTime = (DateTime) c.convert("Mar 12, 2014 2pm");
        assertThat(textualDateTime.getZone()).isEqualTo(DateTimeZone.forOffsetHours(12));
        Assertions.assertThat(textualDateTime)
                .isEqualTo(new DateTime(2014, 3, 12, 14, 0, DateTimeZone.forOffsetHours(12)));
    }

    @Test
    public void convertUsesEtcUTCIfTimeZoneSettingIsEmpty() throws Exception {
        Converter c = new FlexibleDateConverter(ImmutableMap.<String, Object>of("time_zone", ""));

        final DateTime dateOnly = (DateTime) c.convert("2014-3-12");
        assertThat(dateOnly.getZone()).isEqualTo(DateTimeZone.forID("Etc/UTC"));
    }

    @Test
    public void convertUsesEtcUTCIfTimeZoneSettingIsBlank() throws Exception {
        Converter c = new FlexibleDateConverter(ImmutableMap.<String, Object>of("time_zone", " "));

        final DateTime dateOnly = (DateTime) c.convert("2014-3-12");
        assertThat(dateOnly.getZone()).isEqualTo(DateTimeZone.forID("Etc/UTC"));
    }

    @Test
    public void convertUsesEtcUTCIfTimeZoneSettingIsInvalid() throws Exception {
        Converter c = new FlexibleDateConverter(ImmutableMap.<String, Object>of("time_zone", "TEST"));

        final DateTime dateOnly = (DateTime) c.convert("2014-3-12");
        assertThat(dateOnly.getZone()).isEqualTo(DateTimeZone.forID("Etc/UTC"));
    }

    @Test
    public void convertUsesEtcUTCIfTimeZoneSettingIsNotAString() throws Exception {
        Converter c = new FlexibleDateConverter(ImmutableMap.<String, Object>of("time_zone", 42));

        final DateTime dateOnly = (DateTime) c.convert("2014-3-12");
        assertThat(dateOnly.getZone()).isEqualTo(DateTimeZone.forID("Etc/UTC"));
    }
}
