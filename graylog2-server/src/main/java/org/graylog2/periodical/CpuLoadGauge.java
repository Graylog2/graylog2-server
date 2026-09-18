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
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.software.os.CgroupInfo;

public class CpuLoadGauge implements Gauge<Double> {

    private static final Logger LOG = LoggerFactory.getLogger(CpuLoadGauge.class);

    private final Supplier<CentralProcessor> processorSupplier = Suppliers.memoize(this::processor);
    private final Supplier<CgroupInfo> containerCgroupSupplier = Suppliers.memoize(this::detectContainerCgroup);

    private long[] lastTicks;
    private long lastContainerUsage = -1L;
    private long lastContainerTime = -1L;
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
            final CgroupInfo cgroup = containerCgroupSupplier.get();
            if (cgroup != null) {
                final long newUsage = cgroup.getCpuUsage();
                final long newTime = System.nanoTime();
                if (newUsage > 0) {
                    if (lastContainerUsage >= 0 && lastContainerTime > 0) {
                        final long deltaUsage = newUsage - lastContainerUsage;
                        final long deltaTime = newTime - lastContainerTime;
                        if (deltaTime > 0 && deltaUsage >= 0) {
                            final double effectiveCpus = cgroup.getEffectiveCpus();
                            final CentralProcessor processor = processorSupplier.get();
                            final double cpus = effectiveCpus > 0 ? effectiveCpus : processor.getLogicalProcessorCount();
                            if (cpus > 0) {
                                cpuLoad = Math.min(100.0d, Math.max(0.0d, (double) deltaUsage / (deltaTime * cpus) * 100.0d));
                            }
                        }
                    }
                    lastContainerUsage = newUsage;
                    lastContainerTime = newTime;
                    return;
                }
            }

            final CentralProcessor processor = processorSupplier.get();
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

    private CgroupInfo detectContainerCgroup() {
        final CgroupInfo cgroup = cgroupInfo();
        return cgroup != null && cgroup.isContainerized() ? cgroup : null;
    }

    protected CentralProcessor processor() {
        final SystemInfo si = new SystemInfo();
        return si.getHardware().getProcessor();
    }

    protected CgroupInfo cgroupInfo() {
        final SystemInfo si = new SystemInfo();
        return si.getOperatingSystem().getCgroupInfo();
    }
}
