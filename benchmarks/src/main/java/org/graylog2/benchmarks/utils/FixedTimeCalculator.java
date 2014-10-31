package org.graylog2.benchmarks.utils;

import java.util.concurrent.TimeUnit;

public class FixedTimeCalculator implements TimeCalculator{
    private long durationNs;
    public FixedTimeCalculator(int duration, TimeUnit unit) {
        durationNs = TimeUnit.NANOSECONDS.convert(duration, unit);
    }

    @Override
    public long sleepTimeNsForThread(int ordinal) {
        return durationNs;
    }
}
