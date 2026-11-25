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

import org.graylog2.indexer.IndexFailureCounter;
import org.graylog2.plugin.Tools;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexingFailuresMetricsSupplierTest {
    @Mock
    private IndexFailureCounter indexFailureCounter;

    @InjectMocks
    private IndexingFailuresMetricsSupplier supplier;

    @Test
    void sumsTheFailuresOfAllIndicesOverTheLastDay() {
        when(indexFailureCounter.countByIndexSince(any())).thenReturn(Map.of("graylog_1", 2L, "graylog_2", 3L));

        final DateTime before = Tools.nowUTC();
        final Optional<TelemetryEvent> event = supplier.get();
        final DateTime after = Tools.nowUTC();

        assertThat(event).map(TelemetryEvent::metrics).contains(Map.of("indexing_failures_24h", 5L));
        final ArgumentCaptor<DateTime> since = ArgumentCaptor.forClass(DateTime.class);
        verify(indexFailureCounter).countByIndexSince(since.capture());
        assertThat(since.getValue()).isBetween(before.minusHours(24), after.minusHours(24));
    }

    @Test
    void reportsZeroWithoutFailures() {
        when(indexFailureCounter.countByIndexSince(any())).thenReturn(Map.of());

        assertThat(supplier.get()).map(TelemetryEvent::metrics).contains(Map.of("indexing_failures_24h", 0L));
    }
}
