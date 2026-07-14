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
    private volatile Double cpuLoad;
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
        } catch (LinkageError | RuntimeException e) {
            // The CPU-load metric is a nice-to-have and must never crash the node. Native OSHI/JNA load
            // failures surface as LinkageErrors (typically a 'noexec' data dir), and OSHI can also throw
            // RuntimeExceptions reading CPU stats - catch both, but not Throwable (keep OutOfMemoryError etc.
            // propagating). Disable the metric and carry on. Remedy: point 'jna.tmpdir' at an exec-capable dir.
            disabled = true;
            cpuLoad = null;
            LOG.warn("Disabling the system CPU-load metric: unable to read CPU statistics via the OSHI native library. " +
                    "This usually means the Graylog data directory (which holds the unpacked JNA native library) is on " +
                    "a 'noexec' mounted filesystem. To enable the metric, point 'jna.tmpdir' at a writable, " +
                    "exec-capable directory via the JVM options.", e);
        }
    }

    protected CentralProcessor processor() {
        final SystemInfo si = new SystemInfo();
        return si.getHardware().getProcessor();
    }
}
