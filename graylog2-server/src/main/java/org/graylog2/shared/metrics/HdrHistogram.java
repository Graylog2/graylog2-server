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

import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Snapshot;
import org.HdrHistogram.AtomicHistogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

public class HdrHistogram extends com.codahale.metrics.Histogram {
    private static final Logger log = LoggerFactory.getLogger(HdrHistogram.class);
    private final AtomicHistogram hdrHistogram;

    public HdrHistogram(AtomicHistogram hdrHistogram) {
        super(new ExponentiallyDecayingReservoir());
        this.hdrHistogram = hdrHistogram;
    }

    public HdrHistogram(final long highestTrackableValue, final int numberOfSignificantValueDigits) {
        this(new AtomicHistogram(highestTrackableValue, numberOfSignificantValueDigits));
    }

    @Override
    public long getCount() {
        return hdrHistogram.getTotalCount();
    }

    @Override
    public Snapshot getSnapshot() {
        final AtomicHistogram copy = hdrHistogram.copy();
        return new Snapshot() {
            @Override
            public double getValue(double quantile) {
                return copy.getValueAtPercentile(quantile * 100);
            }

            @Override
            public long[] getValues() {
                return new long[0];
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public long getMax() {
                return copy.getMaxValue();
            }

            @Override
            public double getMean() {
                final double mean = copy.getMean();
                return Double.isNaN(mean) ? 0 : mean;
            }

            @Override
            public long getMin() {
                final long minValue = copy.getMinValue();
                return minValue == Long.MAX_VALUE ? 0 : minValue;
            }

            @Override
            public double getStdDev() {
                final double stdDeviation = copy.getStdDeviation();
                return Double.isNaN(stdDeviation) ? 0 : stdDeviation;
            }

            @Override
            public void dump(OutputStream output) {
                final PrintStream printStream;
                try {
                    printStream = new PrintStream(output, false, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                copy.outputPercentileDistribution(printStream, 1d);
            }
        };
    }


    @Override
    public void update(int value) {
        update((long)value);
    }

    @Override
    public void update(long value) {
        try {
            hdrHistogram.recordValue(value);
        } catch (ArrayIndexOutOfBoundsException e) {
            log.debug("Ignoring value {} for HdrHistogram, it exceeds the highest trackable value {}", value, hdrHistogram.getHighestTrackableValue());
        }
    }
}
