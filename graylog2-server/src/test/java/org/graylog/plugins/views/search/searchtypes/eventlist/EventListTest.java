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
package org.graylog.plugins.views.search.searchtypes.eventlist;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.searchtypes.events.EventList;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

import static org.graylog2.plugin.streams.Stream.DEFAULT_EVENTS_STREAM_ID;
import static org.graylog2.plugin.streams.Stream.DEFAULT_SYSTEM_EVENTS_STREAM_ID;

public class EventListTest {

    @Test
    public void testEffectiveStream() {
        final EventList eventList = EventList.builder()
                .streams(ImmutableSet.of("dead-beef", "1337-beef"))
                .build();
        assertThat(eventList.effectiveStreams()).isEqualTo(
                ImmutableSet.of(DEFAULT_EVENTS_STREAM_ID, DEFAULT_SYSTEM_EVENTS_STREAM_ID)
        );
        assertThat(eventList.streams()).isEqualTo(ImmutableSet.of("dead-beef", "1337-beef"));
    }
}
