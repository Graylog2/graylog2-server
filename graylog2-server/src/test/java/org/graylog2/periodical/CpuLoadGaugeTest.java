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

import org.junit.jupiter.api.Test;
import oshi.hardware.CentralProcessor;
import oshi.software.os.CgroupInfo;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CpuLoadGaugeTest {
    @Test
    void reportsCpuLoadAfterTwoSamples() {
        // Stub the processor so the test exercises the gauge's own seed-then-compute logic and stays
        // deterministic and independent of native OSHI/JNA availability (which is absent on 'noexec' hosts).
        final CentralProcessor processor = mock(CentralProcessor.class);
        when(processor.getSystemCpuLoadTicks()).thenReturn(new long[]{1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L});
        when(processor.getSystemCpuLoadBetweenTicks(any(), any())).thenReturn(0.42d);

        final CgroupInfo cgroup = mock(CgroupInfo.class);
        when(cgroup.isContainerized()).thenReturn(false);

        final CpuLoadGauge gauge = new CpuLoadGauge() {
            @Override
            protected CentralProcessor processor() {
                return processor;
            }

            @Override
            protected CgroupInfo cgroupInfo() {
                return cgroup;
            }
        };

        // No sample taken yet.
        assertThat(gauge.getValue()).isNull();

        // The first run only seeds the baseline ticks, so there is nothing to compare against yet.
        gauge.update();
        assertThat(gauge.getValue()).isNull();

        // The second run has a previous sample to compute the load between: 0.42 * 100.
        gauge.update();
        assertThat(gauge.getValue()).isEqualTo(42.0d);
    }

    @Test
    void reportsContainerCpuLoadAfterTwoSamples() {
        final CgroupInfo cgroup = mock(CgroupInfo.class);
        when(cgroup.isContainerized()).thenReturn(true);
        // Return 1s of CPU usage on first sample, then 2s of CPU usage on second sample
        when(cgroup.getCpuUsage()).thenReturn(1_000_000_000L, 2_000_000_000L);
        // 2 effective cores
        when(cgroup.getEffectiveCpus()).thenReturn(2.0d);

        final CpuLoadGauge gauge = new CpuLoadGauge() {
            @Override
            protected CgroupInfo cgroupInfo() {
                return cgroup;
            }
        };

        assertThat(gauge.getValue()).isNull();

        // First run seeds the container usage baseline
        gauge.update();
        assertThat(gauge.getValue()).isNull();

        // Second run computes CPU percentage from delta
        gauge.update();
        assertThat(gauge.getValue()).isNotNull();
        assertThat(gauge.getValue()).isBetween(0.0d, 100.0d);
    }

    @Test
    void reportsContainerCpuLoadWithUnlimitedQuota() {
        final CentralProcessor processor = mock(CentralProcessor.class);
        when(processor.getLogicalProcessorCount()).thenReturn(4);

        final CgroupInfo cgroup = mock(CgroupInfo.class);
        when(cgroup.isContainerized()).thenReturn(true);
        when(cgroup.getCpuUsage()).thenReturn(500_000_000L, 1_000_000_000L);
        when(cgroup.getEffectiveCpus()).thenReturn(CgroupInfo.UNLIMITED_CPUS);

        final CpuLoadGauge gauge = new CpuLoadGauge() {
            @Override
            protected CentralProcessor processor() {
                return processor;
            }

            @Override
            protected CgroupInfo cgroupInfo() {
                return cgroup;
            }
        };

        gauge.update();
        assertThat(gauge.getValue()).isNull();

        gauge.update();
        assertThat(gauge.getValue()).isNotNull();
        assertThat(gauge.getValue()).isBetween(0.0d, 100.0d);
    }

    @Test
    void degradesGracefullyWhenNativeAccessFails() {
        // Simulate a host where OSHI/JNA cannot load its native library (e.g. a 'noexec' data dir),
        // which surfaces as a LinkageError (NoClassDefFoundError / UnsatisfiedLinkError), not an Exception.
        final AtomicInteger nativeCalls = new AtomicInteger();
        final CpuLoadGauge gauge = new CpuLoadGauge() {
            @Override
            protected CentralProcessor processor() {
                nativeCalls.incrementAndGet();
                throw new NoClassDefFoundError("Could not initialize class oshi.software.os.linux.LinuxOperatingSystemJNA");
            }

            @Override
            protected CgroupInfo cgroupInfo() {
                return null;
            }
        };

        // update() must swallow the error rather than propagate it.
        gauge.update();
        assertThat(gauge.getValue()).isNull();

        // Once disabled, subsequent runs stay quiet: the native path is never touched again.
        gauge.update();
        assertThat(gauge.getValue()).isNull();
        assertThat(nativeCalls.get()).isEqualTo(1);
    }
}
