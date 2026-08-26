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
package org.graylog.testing;

import org.threeten.extra.MutableClock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Provides {@link Clock} instances for testing.
 */
public final class TestClocks {
    private TestClocks() {
    }

    /**
     * Returns an immutable fixed clock for the epoch in UTC. (1970-01-01T00:00:00.000Z)
     *
     * @return the clock
     */
    public static Clock fixedEpoch() {
        return Clock.fixed(Instant.ofEpochMilli(0), ZoneOffset.UTC);
    }

    /**
     * Returns a mutable fixed clock for the epoch in UTC. (1970-01-01T00:00:00.000Z)
     *
     * @return the clock
     */
    public static MutableClock mutableFixedEpoch() {
        return MutableClock.epochUTC();
    }
}
