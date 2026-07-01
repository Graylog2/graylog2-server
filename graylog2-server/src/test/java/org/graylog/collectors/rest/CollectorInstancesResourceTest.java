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

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.FleetService;
import org.graylog.collectors.FleetTransactionLogService;
import org.graylog.collectors.SourceService;
import org.graylog.collectors.db.Attribute;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.database.filtering.ComputedFieldRegistry;
import org.graylog2.security.WithAuthorization;
import org.graylog2.security.WithAuthorizationExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(WithAuthorizationExtension.class)
@WithAuthorization(username = "admin", permissions = "*")
class CollectorInstancesResourceTest {

    @Mock
    private CollectorInstanceService collectorInstanceService;
    @Mock
    private FleetService fleetService;
    @Mock
    private SourceService sourceService;
    @Mock
    private ComputedFieldRegistry computedFieldRegistry;
    @Mock
    private FleetTransactionLogService txnLogService;
    @Mock
    private CollectorsConfigService collectorsConfigService;
    @Mock
    private AuditEventSender auditEventSender;

    private CollectorInstancesResource resource;

    @BeforeEach
    void setUp() {
        resource = new CollectorInstancesResource(
                collectorInstanceService,
                fleetService,
                sourceService,
                computedFieldRegistry,
                txnLogService,
                collectorsConfigService,
                auditEventSender);
    }

    @Test
    void getInstanceReturnsMappedInstanceWhenFoundAndPermitted() {
        stubOfflineThreshold();
        when(collectorInstanceService.findByInstanceUid("uid-1"))
                .thenReturn(Optional.of(instance("uid-1", "fleet-1", Instant.now())));

        final var result = resource.getInstance("uid-1");

        assertThat(result.instanceUid()).isEqualTo("uid-1");
        assertThat(result.fleetId()).isEqualTo("fleet-1");
        assertThat(result.status()).isEqualTo("online");
        assertThat(result.identifyingAttributes()).containsEntry("host.name", "host-1");
        assertThat(result.nonIdentifyingAttributes()).containsEntry("os.type", "linux");
    }

    @Test
    void getInstanceMarksInstanceOfflineWhenLastSeenOlderThanThreshold() {
        stubOfflineThreshold();
        // Default offline threshold is 5 minutes; a 10-minute-old heartbeat is offline.
        final var staleLastSeen = Instant.now().minus(Duration.ofMinutes(10));
        when(collectorInstanceService.findByInstanceUid("uid-1"))
                .thenReturn(Optional.of(instance("uid-1", "fleet-1", staleLastSeen)));

        final var result = resource.getInstance("uid-1");

        assertThat(result.status()).isEqualTo("offline");
    }

    @Test
    void getInstanceThrowsNotFoundWhenInstanceMissing() {
        when(collectorInstanceService.findByInstanceUid("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resource.getInstance("missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    @WithAuthorization(username = "reader", permissions = "collector_fleets:read:fleet-1")
    void getInstanceAllowsReaderWithFleetScopedReadPermission() {
        stubOfflineThreshold();
        when(collectorInstanceService.findByInstanceUid("uid-1"))
                .thenReturn(Optional.of(instance("uid-1", "fleet-1", Instant.now())));

        final var result = resource.getInstance("uid-1");

        assertThat(result.instanceUid()).isEqualTo("uid-1");
    }

    @Test
    @WithAuthorization(username = "reader", permissions = "collector_fleets:read:other-fleet")
    void getInstanceDeniedWhenReadPermissionScopedToDifferentFleet() {
        when(collectorInstanceService.findByInstanceUid("uid-1"))
                .thenReturn(Optional.of(instance("uid-1", "fleet-1", Instant.now())));

        assertThatThrownBy(() -> resource.getInstance("uid-1"))
                .isInstanceOf(ForbiddenException.class);
    }

    private void stubOfflineThreshold() {
        when(collectorsConfigService.getOrDefault()).thenReturn(CollectorsConfig.createDefault("graylog.example.com"));
    }

    private static CollectorInstanceDTO instance(String instanceUid, String fleetId, Instant lastSeen) {
        final Instant now = Instant.now();
        return CollectorInstanceDTO.builder()
                .instanceUid(instanceUid)
                .fleetId(fleetId)
                .capabilities(0L)
                .lastSeen(lastSeen)
                .enrolledAt(now)
                .activeCertificateFingerprint("sha256:active")
                .activeCertificatePem("active-pem")
                .activeCertificateExpiresAt(now.plus(Duration.ofDays(30)))
                .issuingCaId("ca-1")
                .enrollmentTokenId("token-1")
                .identifyingAttributes(List.of(Attribute.of("host.name", "host-1")))
                .nonIdentifyingAttributes(List.of(Attribute.of("os.type", "linux")))
                .build();
    }
}
