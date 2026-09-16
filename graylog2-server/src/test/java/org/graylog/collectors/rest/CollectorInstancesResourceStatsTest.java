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
import org.graylog.collectors.CollectorInstanceService.InstanceCount;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.CollectorsPermissions;
import org.graylog.collectors.FleetService;
import org.graylog.collectors.SourceService;
import org.graylog.collectors.db.FleetDTO;
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
class CollectorInstancesResourceStatsTest {

    private static final Instant NOW = Instant.parse("2026-08-27T00:00:00Z");

    @Mock
    private FleetService fleetService;
    @Mock
    private CollectorInstanceService collectorInstanceService;
    @Mock
    private SourceService sourceService;
    @Mock
    private CollectorsConfigService collectorsConfigService;

    private TestResource resource;

    /** Grants FLEET_READ only for the fleet ids in {@code readable}. */
    private class TestResource extends CollectorInstancesResource {
        private final Set<String> readable;

        TestResource(Set<String> readable) {
            super(collectorInstanceService, fleetService, sourceService, null, null, collectorsConfigService, null, null);
            this.readable = readable;
        }

        @Override
        protected boolean isPermitted(String permission, String instanceId) {
            return CollectorsPermissions.FLEET_READ.equals(permission) && readable.contains(instanceId);
        }
    }

    @BeforeEach
    void setUp() {
        resource = new TestResource(Set.of("fleet-visible"));
        when(collectorsConfigService.getOrDefault()).thenReturn(CollectorsConfig.createDefault("localhost"));
    }

    private static FleetDTO fleet(String id, String name) {
        return FleetDTO.builder()
                .id(id)
                .name(name)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }

    @Test
    void statsCountsOnlyGrantedFleets() {
        when(fleetService.getAllFleets()).thenReturn(List.of(
                fleet("fleet-visible", "Visible"),
                fleet("fleet-hidden", "Hidden")));
        when(collectorInstanceService.countByFleetGrouped(any())).thenReturn(Map.of(
                "fleet-visible", new InstanceCount(3L, 2L),
                "fleet-hidden", new InstanceCount(7L, 5L)));
        when(sourceService.countByFleetGrouped()).thenReturn(Map.of(
                "fleet-visible", 4L,
                "fleet-hidden", 9L));

        final var response = resource.stats();

        assertThat(response.totalInstances()).isEqualTo(3L);
        assertThat(response.onlineInstances()).isEqualTo(2L);
        assertThat(response.offlineInstances()).isEqualTo(1L);
        assertThat(response.totalFleets()).isEqualTo(1L);
        assertThat(response.totalSources()).isEqualTo(4L);
    }
}
