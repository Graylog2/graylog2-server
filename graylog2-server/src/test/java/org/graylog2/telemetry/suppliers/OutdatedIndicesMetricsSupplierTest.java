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

import org.graylog2.indexer.indices.OutdatedIndex;
import org.graylog2.indexer.indices.OutdatedIndexService;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutdatedIndicesMetricsSupplierTest {
    @Mock
    private OutdatedIndexService outdatedIndexService;

    @InjectMocks
    private OutdatedIndicesMetricsSupplier supplier;

    @Test
    void shouldCountOutdatedIndicesByCategory() {
        final OutdatedIndex managed = new OutdatedIndex("graylog_0", "7", false, true, null);
        final OutdatedIndex managedActiveWrite = new OutdatedIndex("graylog_1", "7", false, true, "graylog_deflector");
        final OutdatedIndex managedWarm = new OutdatedIndex("graylog_warm", "7", true, true, null);
        final OutdatedIndex system = new OutdatedIndex(".kibana", "7", false);
        final OutdatedIndex foreign = new OutdatedIndex("customer_data", "7", false);

        when(outdatedIndexService.getOutdatedIndices())
                .thenReturn(List.of(managed, managedActiveWrite, managedWarm, system, foreign));

        final Optional<TelemetryEvent> event = supplier.get();

        assertThat(event).isPresent();
        assertThat(event.get().metrics()).isEqualTo(Map.<String, Object>of(
                "managed", 3L,
                "warm", 1L,
                "active_write_index", 1L,
                "system", 1L,
                "foreign", 1L
        ));
    }

    @Test
    void shouldReturnZeroCountsWhenNoOutdatedIndices() {
        when(outdatedIndexService.getOutdatedIndices()).thenReturn(List.of());

        final Optional<TelemetryEvent> event = supplier.get();

        assertThat(event).isPresent();
        assertThat(event.get().metrics()).isEqualTo(Map.<String, Object>of(
                "managed", 0L,
                "warm", 0L,
                "active_write_index", 0L,
                "system", 0L,
                "foreign", 0L
        ));
    }
}
