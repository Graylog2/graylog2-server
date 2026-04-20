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

import org.graylog2.inputs.InputService;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InputsMetricsSupplierTest {
    @Mock
    private InputService inputService;

    @InjectMocks
    private InputsMetricsSupplier supplier;

    @Test
    public void shouldReturnCountsByType() {
        final Map<String, Long> counts = Map.of(
                "org.graylog.plugins.beats.Beats2Input", 2L,
                "org.graylog2.inputs.gelf.tcp.GELFTCPInput", 3L,
                "org.graylog2.inputs.tcp.DuplicateInput", 1L,
                "org.graylog2.inputs.udp.DuplicateInput", 1L
        );
        when(inputService.totalCountByType()).thenReturn(counts);

        Optional<TelemetryEvent> event = supplier.get();

        final Map<String, Long> expectedCounts = Map.of(
                "beats_2_input", 2L,
                "gelftcp_input", 3L,
                "duplicate_input", 2L
        );
        assertTrue(event.isPresent());
        assertEquals(expectedCounts, event.get().metrics());
    }

    @Test
    public void shouldReturnEmptyMetricsWhenNoInputs() {
        when(inputService.totalCountByType()).thenReturn(Collections.emptyMap());

        Optional<TelemetryEvent> event = supplier.get();

        assertTrue(event.isPresent());
        assertTrue(event.get().metrics().isEmpty());
    }
}
