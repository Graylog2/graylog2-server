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
package org.graylog2.plugin.configuration.fields;

import org.graylog2.shared.SuppressForbidden;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Test;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DropdownFieldValueTemplatesTest {
    private enum TestEnum {
        ONE, TWO
    }

    @Test
    public void testBuildEnumMap() throws Exception {
        final Map<String, String> enumMap = DropdownField.ValueTemplates.valueMapFromEnum(TestEnum.class, (t) -> t.name().toLowerCase(Locale.ENGLISH));
        assertThat(enumMap)
                .containsEntry("ONE", "one")
                .containsEntry("TWO", "two");
    }

    /**
     * Test that the timezone map for DropdownFields is ordered first by offset and then alphabetically
     */
    @Test
    @SuppressForbidden("Intentionally use system default timezone")
    public void testBuildTimeZoneMap() {
        Map<String, String> timezones = DropdownField.ValueTemplates.timeZones();
        DateTimeZone curDTZ = null;
        int curOffset = 0;
        Instant now = new DateTime().withZone(DateTimeZone.getDefault()).toInstant();
        for (String tz : timezones.keySet()) {
            DateTimeZone nextDTZ = DateTimeZone.forID(tz);
            int nextOffset = nextDTZ.getOffset(now);
            if (curDTZ != null) {
                // current offset should always be less than or equal to the next
                assertThat(curOffset).isLessThanOrEqualTo(nextOffset);
                // if the timezones have the same offset, current should come before next alphabetically
                if (curOffset == nextOffset) {
                    assertThat(curDTZ.getID().toLowerCase().compareTo(nextDTZ.getID().toLowerCase())).isLessThan(0);
                }
            }
            curDTZ = nextDTZ;
            curOffset = nextOffset;
        }
    }
}
