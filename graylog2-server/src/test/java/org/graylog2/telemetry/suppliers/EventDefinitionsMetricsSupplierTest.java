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

import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventDefinitionsMetricsSupplierTest {
    @Mock
    private DBEventDefinitionService dbEventDefinitionService;

    @InjectMocks
    private EventDefinitionsMetricsSupplier eventDefinitionsMetricsSupplier;

    @Test
    public void shouldReturnEventDefinitionsMetrics() {
        Map<String, Long> counts = Map.of(
                "illuminate_event_definitions", 12L,
                "user_event_definitions", 2L
        );

        when(dbEventDefinitionService.countByType()).thenReturn(counts);

        Optional<TelemetryEvent> event = eventDefinitionsMetricsSupplier.get();

        assertTrue(event.isPresent());
        assertEquals(counts, event.get().metrics());
    }
}
