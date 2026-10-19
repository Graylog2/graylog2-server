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
package org.graylog.collectors.rest;

import jakarta.ws.rs.ClientErrorException;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.FleetService;
import org.graylog.collectors.SourceService;
import org.graylog.collectors.db.FleetDTO;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.plugin.database.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FleetResourceDeleteTest {

    private static final Instant NOW = Instant.parse("2026-08-27T00:00:00Z");
    private static final String FLEET_ID = "fleet-1";

    @Mock
    private FleetService fleetService;
    @Mock
    private CollectorInstanceService instanceService;
    @Mock
    private SourceService sourceService;
    @Mock
    private CollectorsConfigService collectorsConfigService;
    @Mock
    private AuditEventSender auditEventSender;

    private FleetResource resource;

    @BeforeEach
    void setUp() {
        resource = new FleetResource(fleetService, instanceService, sourceService, collectorsConfigService, auditEventSender) {
            @Override
            protected boolean isPermitted(String permission, String instanceId) {
                return true;
            }

            @Override
            protected User getCurrentUser() {
                final User user = mock(User.class);
                when(user.getName()).thenReturn("admin");
                return user;
            }
        };
        when(fleetService.get(FLEET_ID)).thenReturn(Optional.of(FleetDTO.builder()
                .id(FLEET_ID)
                .name("web")
                .createdAt(NOW)
                .updatedAt(NOW)
                .build()));
    }

    @Test
    void refusesToDeleteAFleetWithAssignedInstances() {
        when(fleetService.countAssignedInstances(FLEET_ID)).thenReturn(2L);

        assertThatThrownBy(() -> resource.delete(FLEET_ID))
                .isInstanceOf(ClientErrorException.class)
                .hasMessageContaining("2 assigned instance(s)")
                .satisfies(e -> assertThat(((ClientErrorException) e).getResponse().getStatus()).isEqualTo(409));

        verify(fleetService, never()).delete(anyString());
        verify(auditEventSender, never()).success(any(), anyString(), any());
    }

    @Test
    void deletesAFleetWithoutAssignedInstances() {
        when(fleetService.countAssignedInstances(FLEET_ID)).thenReturn(0L);
        when(fleetService.delete(FLEET_ID)).thenReturn(true);

        resource.delete(FLEET_ID);

        verify(fleetService).delete(FLEET_ID);
        verify(auditEventSender).success(any(), eq(AuditEventTypes.COLLECTOR_FLEET_DELETE), any());
    }
}
