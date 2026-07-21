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

        final CpuLoadGauge gauge = new CpuLoadGauge() {
            @Override
            protected CentralProcessor processor() {
                return processor;
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
