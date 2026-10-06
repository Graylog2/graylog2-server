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

import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.CollectorsPermissions;
import org.graylog.collectors.FleetService;
import org.graylog.collectors.SourceService;
import org.graylog.collectors.db.FleetDTO;
import org.graylog2.audit.AuditEventSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FleetResourceStatsTest {

    private static final Instant NOW = Instant.parse("2026-08-27T00:00:00Z");

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

    private TestFleetResource resource;

    /** Grants FLEET_READ only for the fleet ids in {@code readable}. */
    private class TestFleetResource extends FleetResource {
        private final Set<String> readable;

        TestFleetResource(Set<String> readable) {
            super(fleetService, instanceService, sourceService, collectorsConfigService, auditEventSender);
            this.readable = readable;
        }

        @Override
        protected boolean isPermitted(String permission, String instanceId) {
            return CollectorsPermissions.FLEET_READ.equals(permission) && readable.contains(instanceId);
        }
    }

    @BeforeEach
    void setUp() {
        resource = new TestFleetResource(Set.of("fleet-visible"));
        when(collectorsConfigService.getOrDefault()).thenReturn(CollectorsConfig.createDefault("localhost"));
    }

    @Test
    void bulkStatsOmitsFleetsTheSubjectCannotRead() {
        when(fleetService.getAllFleets()).thenReturn(List.of(
                fleet("fleet-visible", "Visible"),
                fleet("fleet-hidden", "Hidden")));
        when(instanceService.countByFleetGrouped(any())).thenReturn(Map.of());
        when(sourceService.countByFleetGrouped()).thenReturn(Map.of());

        final var response = resource.bulkStats();

        assertThat(response.fleets()).extracting("fleetId").containsExactly("fleet-visible");
        assertThat(response.fleets()).extracting("fleetName").doesNotContain("Hidden");
    }

    private static FleetDTO fleet(String id, String name) {
        return FleetDTO.builder()
                .id(id)
                .name(name)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }
}
