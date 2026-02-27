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

import org.graylog.plugins.sidecar.services.SidecarService;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SidecarsVersionSupplierTest {
    @Mock
    private SidecarService sidecarService;

    @InjectMocks
    private SidecarsVersionSupplier supplier;

    @Test
    public void shouldReturnSidecarsVersion() {
        final Map<String, Long> counts = Map.of(
                "1.5.1", 3L,
                "1.4.0", 2L
        );

        when(sidecarService.countByVersion()).thenReturn(counts);

        Optional<TelemetryEvent> event = supplier.get();

        assertThat(event).isPresent();
        assertThat(event.get().metrics())
                .isEqualTo(Map.of(
                        "1.5.1", 3L,
                        "1.4.0", 2L
                ));
    }
}
