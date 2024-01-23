package org.graylog.events.event;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class EventReplayInfoTest {

    @Test
    public void testCreateNoFilters() {
        // Backwards-compatibility test for events that previously did not have filters.
        final EventReplayInfo info = EventReplayInfo.builder()
                .query("*")
                .streams(Collections.singleton("stream"))
                .timerangeStart(DateTime.now().minusMinutes(1))
                .timerangeEnd(DateTime.now()).build();
        Assertions.assertNotNull(info.filters());
        Assertions.assertTrue(info.filters().isEmpty());
    }
}
