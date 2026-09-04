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

import org.graylog2.shared.users.UserService;
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
public class UsersMetricsSupplierTest {
    @Mock
    private UserService userService;

    @InjectMocks
    private UsersMetricsSupplier usersMetricsSupplier;

    @Test
    public void shouldReturnUsersMetrics() {
        Map<String, Long> counts = Map.of(
                "admin_users", 2L,
                "non_admin_users", 4L
        );

        when(userService.countByPrivilege()).thenReturn(counts);

        Optional<TelemetryEvent> event = usersMetricsSupplier.get();

        assertTrue(event.isPresent());
        assertEquals(counts, event.get().metrics());
    }
}
