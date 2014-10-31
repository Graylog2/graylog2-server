package org.graylog2.benchmarks.utils;

import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

public class BusySleeper {

    public static void consumeCpuFor(long value, TimeUnit unit) {
        long requestedSleepNs = TimeUnit.NANOSECONDS.convert(value, unit);
        final long start = System.nanoTime();
        final long stopTime = start + requestedSleepNs;
        while (System.nanoTime() < stopTime) {
            Blackhole.consumeCPU(256);
        }
    }
}
