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
package org.graylog2.telemetry.suppliers;

import org.graylog2.shared.system.stats.StatsService;
import org.graylog2.shared.system.stats.SystemStats;
import org.graylog2.shared.system.stats.os.Memory;
import org.graylog2.shared.system.stats.os.OsStats;
import org.graylog2.shared.system.stats.os.Processor;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SystemMetricsSupplierTest {
    @Mock
    private StatsService statsService;

    @Mock
    private SystemStats systemStats;

    @Mock
    private OsStats osStats;

    @Mock
    private Processor processor;

    @Mock
    private Memory memory;

    @InjectMocks
    private SystemMetricsSupplier systemMetricsSupplier;

    @Test
    public void shouldReturnSystemMetrics() {
        String expectedOsName = System.getProperty("os.name");
        String expectedOsVersion = System.getProperty("os.version");

        when(statsService.systemStats()).thenReturn(systemStats);
        when(systemStats.osStats()).thenReturn(osStats);
        when(osStats.processor()).thenReturn(processor);
        when(osStats.memory()).thenReturn(memory);
        when(processor.totalCores()).thenReturn(14);
        when(memory.total()).thenReturn(48000000000L);

        Optional<TelemetryEvent> event = systemMetricsSupplier.get();

        assertTrue(event.isPresent());
        assertThat(event.get().metrics())
                .containsEntry("os_name", expectedOsName)
                .containsEntry("os_version", expectedOsVersion)
                .containsEntry("cpu_cores", 14)
                .containsEntry("total_memory", 48000000000L);
    }
}
