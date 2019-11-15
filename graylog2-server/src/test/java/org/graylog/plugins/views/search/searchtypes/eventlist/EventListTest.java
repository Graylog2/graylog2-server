/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
