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

import org.graylog2.lookup.db.DBLookupTableService;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LookupTablesSupplierTest {
    @Mock
    private DBLookupTableService dbLookupTableService;

    @InjectMocks
    private LookupTablesSupplier lookupTablesSupplier;

    @Test
    public void shouldReturnLookupTablesMetrics() {
        long expectedCount = 5L;
        when(dbLookupTableService.count()).thenReturn(expectedCount);

        Optional<TelemetryEvent> metrics = lookupTablesSupplier.get();

        assertTrue(metrics.isPresent());
        assertEquals(expectedCount, metrics.get().metrics().get("lookup_table_count"));
    }
}
