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

import com.google.common.collect.ImmutableList;
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

    public TestEvent(DateTime timestamp, String key) {
        super(ULID.nextULID(), timestamp, "test", "1", "Test Event", "test", 1, true);
        this.setKeyTuple(ImmutableList.of(key));
    }
}
