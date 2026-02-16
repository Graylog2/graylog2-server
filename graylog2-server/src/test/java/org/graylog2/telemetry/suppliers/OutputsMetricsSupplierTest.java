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

import org.graylog2.streams.OutputService;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutputsMetricsSupplierTest {

    @Mock
    private OutputService outputService;

    @InjectMocks
    private OutputsMetricsSupplier testee;

    @Test
    void emptyServiceResponseReturnsEmptyOptional() {
        when(outputService.countByType()).thenReturn(Collections.emptyMap());

        assertThat(testee.get()).isNotEmpty()
                .contains(TelemetryEvent.of(Map.of()));

        verify(outputService).countByType();
        verifyNoMoreInteractions(outputService);
    }

    @Test
    void outputTypesFrequencyGetsWrappedInTelemetryEvent() {
        final Map<String, Long> typeFrequency = Map.of("a", 1L, "b", 2L, "c", 3L);
        when(outputService.countByType()).thenReturn(typeFrequency);

        final Optional<TelemetryEvent> expected = Optional.of(TelemetryEvent.of(new HashMap<>(typeFrequency)));

        assertThat(testee.get()).isEqualTo(expected);
        verify(outputService).countByType();
        verifyNoMoreInteractions(outputService);
    }
}
