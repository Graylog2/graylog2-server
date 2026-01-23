/*
 * Copyright (C) 2026 Graylog, Inc.
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
package org.graylog2.shared.metrics;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;

import java.time.Duration;
import java.util.ArrayDeque;

public class SlidingWindowCounter extends Counter {
    private final long windowMillis;
    private final Clock clock;
    private final ArrayDeque<Long> timestamps = new ArrayDeque<>();

    public SlidingWindowCounter(Duration window) {
        this(window, Clock.defaultClock());
    }

    SlidingWindowCounter(Duration window, Clock clock) {
        this.windowMillis = window.toMillis();
        this.clock = clock;
    }

    @Override
    public void inc() {
        inc(1);
    }

    @Override
    public void inc(long n) {
        if (n <= 0) {
            return;
        }
        final long now = clock.getTime();
        synchronized (timestamps) {
            purgeExpired(now);
            for (long i = 0; i < n; i++) {
                timestamps.addLast(now);
            }
        }
    }

    @Override
    public void dec() {
        dec(1);
    }

    @Override
    public void dec(long n) {
        if (n <= 0) {
            return;
        }
        final long now = clock.getTime();
        synchronized (timestamps) {
            purgeExpired(now);
            for (long i = 0; i < n && !timestamps.isEmpty(); i++) {
                timestamps.removeFirst();
            }
        }
    }

    @Override
    public long getCount() {
        final long now = clock.getTime();
        synchronized (timestamps) {
            purgeExpired(now);
            return timestamps.size();
        }
    }

    private void purgeExpired(long now) {
        final long cutoff = now - windowMillis;
        while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
            timestamps.removeFirst();
        }
    }
}
