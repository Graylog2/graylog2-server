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
package org.graylog.events.event;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class EventReplayInfoTest {

    @Test
    public void testCreateNoFilters() {
        // Backwards-compatibility test for events that previously did not have filters.
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final EventReplayInfo info = EventReplayInfo.builder()
                .query("*")
                .streams(Collections.singleton("stream"))
                .timerangeStart(now.minusMinutes(1))
                .timerangeEnd(now).build();
        Assertions.assertNotNull(info.filters());
        Assertions.assertTrue(info.filters().isEmpty());
    }
}
