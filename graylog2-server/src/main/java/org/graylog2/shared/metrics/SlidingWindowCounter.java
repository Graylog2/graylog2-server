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
package org.graylog2.shared.metrics;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;

import java.time.Duration;
import java.util.ArrayDeque;

public class SlidingWindowCounter extends Counter {
    private static final long MINUTE_MILLIS = Duration.ofMinutes(1).toMillis();

    private final long windowMillis;
    private final Clock clock;
    private final ArrayDeque<MinuteBucket> buckets = new ArrayDeque<>(2);
    private long totalCount = 0L;

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
        final long nowMinute = floorToMinute(now);
        synchronized (buckets) {
            purgeExpired(now);
            final MinuteBucket lastBucket = buckets.peekLast();
            if (lastBucket != null && lastBucket.minuteStart == nowMinute) {
                lastBucket.count += n;
            } else {
                buckets.addLast(new MinuteBucket(nowMinute, n));
            }
            totalCount += n;
        }
    }

    @Override
    public void dec() {
        throw new UnsupportedOperationException("Decrement without argument is not supported.");
    }

    @Override
    public void dec(long n) {
        throw new UnsupportedOperationException("Decrement is not supported.");
    }

    @Override
    public long getCount() {
        final long now = clock.getTime();
        synchronized (buckets) {
            purgeExpired(now);
            return totalCount;
        }
    }

    private void purgeExpired(long now) {
        final long cutoffMinute = floorToMinute(now - windowMillis);
        while (!buckets.isEmpty() && buckets.peekFirst().minuteStart < cutoffMinute) {
            totalCount -= buckets.removeFirst().count;
        }
    }

    private static long floorToMinute(long timeMillis) {
        return (timeMillis / MINUTE_MILLIS) * MINUTE_MILLIS;
    }

    private static final class MinuteBucket {
        private final long minuteStart;
        private long count;

        private MinuteBucket(long minuteStart, long count) {
            this.minuteStart = minuteStart;
            this.count = count;
        }
    }
}
