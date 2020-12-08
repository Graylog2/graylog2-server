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
package org.graylog2.utilities;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Helper to time arbitrary blocks of Java code, especially useful for coarse one-off performance testing.
 * This is not intended to be a silver bullet, but certainly more convenient than adding stopwatches all over the place.
 *
 * Use it like this:
 * <pre>
 *     try (TimedBlock ignored = TimedBlock.timed("myBlock").aboveMillis(100).start()) {
 *         // code to time, will only print timing info if it took 100ms or more
 *     }
 * </pre>
 *
 * By default it will use its own logger to write timing information after the try-with-resources block exits.
 */
@SuppressWarnings("unused")
public class TimedBlock implements AutoCloseable {
    private static final Logger ownLogger = LoggerFactory.getLogger(TimedBlock.class);

    private final Stopwatch stopwatch = Stopwatch.createUnstarted();
    private final Logger log;
    private final String name;
    private final long threshold;
    private final TimeUnit thresholdUnit;
    private final TimeUnit reportUnit;

    private TimedBlock(Logger log,
                      String name,
                      long threshold,
                      TimeUnit thresholdUnit, TimeUnit reportUnit) {
        this.log = log;
        this.name = name;
        this.threshold = threshold;
        this.thresholdUnit = thresholdUnit;
        this.reportUnit = reportUnit;
    }

    public static Builder timed(String name) {
        return new Builder(name);
    }

    public TimedBlock start() {
        stopwatch.start();
        return this;
    }

    @Override
    public void close() {
        stopwatch.stop();
        if (stopwatch.elapsed(thresholdUnit) >= threshold) {
            log.info("[{}] execution took {} {}", new Object[] {name, stopwatch.elapsed(reportUnit), niceName(reportUnit)});
        }
    }

    private String niceName(TimeUnit reportUnit) {
        switch (reportUnit) {
            case NANOSECONDS:
                return "ns";
            case MICROSECONDS:
                return "µs";
            case MILLISECONDS:
                return "ms";
            case SECONDS:
                return "s";
            case MINUTES:
                return "m";
            case HOURS:
                return "h";
            case DAYS:
                return "d";
        }
        return "";
    }

    public static class Builder {
        private Logger log = ownLogger;
        private String name = "block";
        private long threshold = 0L;
        private TimeUnit thresholdUnit = TimeUnit.MILLISECONDS;
        private TimeUnit reportUnit = TimeUnit.MILLISECONDS;

        public Builder(String name) {
            name(name);
        }

        public Builder log(Logger log) {
            this.log = log;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder aboveMillis(long time) {
            this.threshold = time;
            return this;
        }

        public Builder above(long time, TimeUnit unit) {
            this.threshold = time;
            this.thresholdUnit = unit;
            return this;
        }

        public Builder millis() {
            reportTimeUnit(TimeUnit.MILLISECONDS);
            return this;
        }

        public Builder micros() {
            reportTimeUnit(TimeUnit.MICROSECONDS);
            return this;
        }

        public Builder seconds() {
            reportTimeUnit(TimeUnit.SECONDS);
            return this;
        }

        public Builder reportTimeUnit(TimeUnit reportUnit) {
            this.reportUnit = reportUnit;
            return this;
        }

        public TimedBlock start() {
            return new TimedBlock(log, name, threshold, thresholdUnit, reportUnit).start();
        }
    }

}
