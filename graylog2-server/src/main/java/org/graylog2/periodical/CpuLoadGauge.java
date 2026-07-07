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
package org.graylog2.periodical;

import com.codahale.metrics.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

public class CpuLoadGauge implements Gauge<Double> {

    private static final Logger LOG = LoggerFactory.getLogger(CpuLoadGauge.class);

    private long[] lastTicks;
    private Double cpuLoad;
    private boolean disabled = false;

    @Override
    public Double getValue() {
        return cpuLoad;
    }

    public void update() {
        if (disabled) {
            return;
        }
        try {
            final CentralProcessor processor = processor();
            final long[] newTicks = processor.getSystemCpuLoadTicks();
            if (lastTicks == null) {
                // First run: there is no previous sample to compare against yet, so just seed the baseline.
                lastTicks = newTicks;
                return;
            }
            cpuLoad = processor.getSystemCpuLoadBetweenTicks(lastTicks, newTicks) * 100.0d;
            lastTicks = newTicks;
        } catch (Throwable t) {
            // NoClassDefFoundError / UnsatisfiedLinkError are Errors (not Exceptions), so we must catch Throwable.
            // This typically happens when the Graylog data directory - which holds the unpacked JNA native library -
            // is on a 'noexec' mounted filesystem and OSHI/JNA cannot map its native library. The CPU-load metric is
            // a nice-to-have and must never prevent the node from running, so we disable it and carry on.
            disabled = true;
            cpuLoad = null;
            LOG.warn("Disabling the system CPU-load metric: unable to read CPU statistics via the OSHI native library. " +
                    "This usually means the Graylog data directory (which holds the unpacked JNA native library) is on " +
                    "a 'noexec' mounted filesystem. To enable the metric, point 'jna.tmpdir' at a writable, " +
                    "exec-capable directory via the JVM options.", t);
        }
    }

    protected CentralProcessor processor() {
        final SystemInfo si = new SystemInfo();
        return si.getHardware().getProcessor();
    }
}
