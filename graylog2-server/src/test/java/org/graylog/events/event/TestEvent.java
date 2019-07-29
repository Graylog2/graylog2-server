package org.graylog.events.event;

import de.huxhorn.sulky.ulid.ULID;
import org.joda.time.DateTime;

import static org.joda.time.DateTimeZone.UTC;

public class TestEvent extends EventImpl {
    private static final de.huxhorn.sulky.ulid.ULID ULID = new ULID();

    public TestEvent() {
        super(ULID.nextULID(), DateTime.now(UTC), "test", "1", "Test Event", "test", 1, true);
    }

    public TestEvent(DateTime timestamp) {
        super(ULID.nextULID(), timestamp, "test", "1", "Test Event", "test", 1, true);
    }
}
