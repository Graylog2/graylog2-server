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
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

import java.util.concurrent.TimeUnit;

public class HdrTimer extends Timer {

    private final HdrHistogram hdrHistogram;

    public HdrTimer(final long highestTrackableValue, final TimeUnit unit, final int numberOfSignificantValueDigits) {
        this(highestTrackableValue, unit, numberOfSignificantValueDigits, new ExponentiallyDecayingReservoir());
    }

    public HdrTimer(long highestTrackableValue, TimeUnit unit, int numberOfSignificantValueDigits, Reservoir reservoir) {
        this(highestTrackableValue, unit, numberOfSignificantValueDigits, reservoir, Clock.defaultClock());
    }


    public HdrTimer(long highestTrackableValue,
                    TimeUnit unit,
                    int numberOfSignificantValueDigits,
                    Reservoir reservoir,
                    Clock clock) {
        super(reservoir, clock);
        hdrHistogram = new HdrHistogram(unit.toNanos(highestTrackableValue), numberOfSignificantValueDigits);
    }

    @Override
    public long getCount() {
        return hdrHistogram.getCount();
    }

    @Override
    public Snapshot getSnapshot() {
        return hdrHistogram.getSnapshot();
    }

    @Override
    public void update(long duration, TimeUnit unit) {
        super.update(duration, unit);
        if (duration >= 0) {
            hdrHistogram.update(unit.toNanos(duration));
        }
    }
}
