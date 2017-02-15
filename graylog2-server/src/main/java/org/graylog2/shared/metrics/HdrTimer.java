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
