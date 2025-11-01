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

import org.graylog.events.notifications.DBNotificationService;
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
public class EventNotificationsMetricsSupplierTest {
    @Mock
    private DBNotificationService dbNotificationService;

    @InjectMocks
    private EventNotificationsMetricsSupplier eventNotificationsMetricsSupplier;

    @Test
    public void shouldReturnNotificationMetrics() {
        Map<String, Long> counts = Map.of(
                "http-notification-v1", 1L,
                "email-notification-v1", 3L,
                "slack-notification-v1", 2L
        );

        when(dbNotificationService.countByType()).thenReturn(counts);

        Optional<TelemetryEvent> event = eventNotificationsMetricsSupplier.get();

        assertTrue(event.isPresent());
        assertEquals(counts, event.get().metrics());
    }
}
