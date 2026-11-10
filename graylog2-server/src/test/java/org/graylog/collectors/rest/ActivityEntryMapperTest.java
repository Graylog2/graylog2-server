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
import org.graylog.collectors.CollectorsPermissions;
import org.graylog.collectors.FleetService;
import org.graylog.collectors.db.Attribute;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.collectors.db.FleetDTO;
import org.graylog.collectors.db.FleetReassignedPayload;
import org.graylog.collectors.db.MarkerPayload;
import org.graylog.collectors.db.MarkerType;
import org.graylog.collectors.db.TransactionMarker;
import org.graylog.collectors.rest.RecentActivityResponse.FleetReassignedDetails;
import org.graylog.security.HasPermissions;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityEntryMapperTest {

    private static final HasPermissions ALLOW_ALL = (permission, id) -> true;

    @Mock
    private FleetService fleetService;
    @Mock
    private CollectorInstanceService instanceService;
    @Mock
    private UserService userService;

    private ActivityEntryMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ActivityEntryMapper(fleetService, instanceService, userService);
        lenient().when(fleetService.getAllFleets()).thenReturn(List.of(
                fleet("fleet-1", "Production"),
                fleet("fleet-2", "Staging")));
    }

    @Test
    void resolvesFleetTargetAndActor() {
        final var alice = mock(User.class);
        when(alice.getId()).thenReturn("alice-id");
        when(alice.getFullName()).thenReturn("Alice Admin");
        when(userService.load("alice")).thenReturn(alice);

        final var entries = mapper.toEntries(
                List.of(marker(1L, TransactionMarker.TARGET_FLEET, Set.of("fleet-1"), MarkerType.CONFIG_CHANGED, null, "alice")),
                ALLOW_ALL);

        assertThat(entries).hasSize(1);
        final var entry = entries.getFirst();
        assertThat(entry.seq()).isEqualTo(1L);
        assertThat(entry.type()).isEqualTo("CONFIG_CHANGED");
        assertThat(entry.actor().username()).isEqualTo("alice");
        assertThat(entry.actor().fullName()).isEqualTo("Alice Admin");
        assertThat(entry.targets()).hasSize(1);
        assertThat(entry.targets().getFirst().id()).isEqualTo("fleet-1");
        assertThat(entry.targets().getFirst().name()).isEqualTo("Production");
        assertThat(entry.targets().getFirst().type()).isEqualTo(TransactionMarker.TARGET_FLEET);
        assertThat(entry.details()).isNull();
    }

    @Test
    void resolvesCollectorTargetHostname() {
        when(instanceService.findByInstanceUids(Set.of("uid-1"))).thenReturn(Map.of(
                "uid-1", instance("uid-1", "fleet-1", "web-01")));

        final var entries = mapper.toEntries(
                List.of(marker(2L, TransactionMarker.TARGET_COLLECTOR, Set.of("uid-1"), MarkerType.RESTART, null, null)),
                ALLOW_ALL);

        assertThat(entries.getFirst().targets()).hasSize(1);
        assertThat(entries.getFirst().targets().getFirst().name()).isEqualTo("web-01");
        assertThat(entries.getFirst().actor()).isNull();
    }

    @Test
    void reassignmentCarriesResolvedDestinationFleet() {
        when(instanceService.findByInstanceUids(Set.of("uid-1"))).thenReturn(Map.of(
                "uid-1", instance("uid-1", "fleet-1", "web-01")));

        final var entries = mapper.toEntries(
                List.of(marker(3L, TransactionMarker.TARGET_COLLECTOR, Set.of("uid-1"), MarkerType.FLEET_REASSIGNED,
                        new FleetReassignedPayload("fleet-2"), null)),
                ALLOW_ALL);

        assertThat(entries.getFirst().details()).isInstanceOf(FleetReassignedDetails.class);
        final var details = (FleetReassignedDetails) entries.getFirst().details();
        assertThat(details.destinationFleet().id()).isEqualTo("fleet-2");
        assertThat(details.destinationFleet().name()).isEqualTo("Staging");
    }

    @Test
    void unknownFleetTargetRendersAsDeleted() {
        final var entries = mapper.toEntries(
                List.of(marker(4L, TransactionMarker.TARGET_FLEET, Set.of("fleet-gone"), MarkerType.CONFIG_CHANGED, null, null)),
                ALLOW_ALL);

        assertThat(entries.getFirst().targets()).hasSize(1);
        assertThat(entries.getFirst().targets().getFirst().id()).isNull();
        assertThat(entries.getFirst().targets().getFirst().name()).isEqualTo("[deleted]");
    }

    @Test
    void skipsTargetsAndDetailsTheUserMayNotSee() {
        final HasPermissions denyFleet2 = (permission, id) ->
                !(CollectorsPermissions.FLEET_READ.equals(permission) && "fleet-2".equals(id));

        final var entries = mapper.toEntries(
                List.of(marker(5L, TransactionMarker.TARGET_FLEET, Set.of("fleet-2"), MarkerType.FLEET_REASSIGNED,
                        new FleetReassignedPayload("fleet-2"), null)),
                denyFleet2);

        assertThat(entries.getFirst().targets()).isEmpty();
        assertThat(entries.getFirst().details()).isNull();
    }

    @Test
    void hidesActorFullNameWithoutUsersReadPermission() {
        final var alice = mock(User.class);
        when(alice.getId()).thenReturn("alice-id");
        when(userService.load("alice")).thenReturn(alice);

        final HasPermissions denyUsersRead = (permission, id) -> !permission.startsWith("users:");

        final var entries = mapper.toEntries(
                List.of(marker(6L, TransactionMarker.TARGET_FLEET, Set.of("fleet-1"), MarkerType.CONFIG_CHANGED, null, "alice")),
                denyUsersRead);

        assertThat(entries.getFirst().actor().fullName()).isEqualTo("Unknown");
    }

    @Test
    void emptyInputYieldsEmptyOutput() {
        assertThat(mapper.toEntries(List.of(), ALLOW_ALL)).isEmpty();
    }

    private static FleetDTO fleet(String id, String name) {
        return FleetDTO.builder()
                .id(id)
                .name(name)
                .createdAt(Instant.EPOCH)
                .updatedAt(Instant.EPOCH)
                .build();
    }

    private static CollectorInstanceDTO instance(String uid, String fleetId, String hostname) {
        return CollectorInstanceDTO.builder()
                .instanceUid(uid)
                .fleetId(fleetId)
                .nonIdentifyingAttributes(List.of(Attribute.of("host.name", hostname)))
                .lastSeen(Instant.EPOCH)
                .enrolledAt(Instant.EPOCH)
                .messageSeqNum(0L)
                .capabilities(0L)
                .activeCertificateFingerprint("fp-" + uid)
                .activeCertificatePem("pem")
                .activeCertificateExpiresAt(Instant.EPOCH)
                .issuingCaId("ca-1")
                .enrollmentTokenId("token-1")
                .build();
    }

    private static TransactionMarker marker(long seq, String target, Set<String> targetIds, MarkerType type,
                                            MarkerPayload payload, String createdByUser) {
        return new TransactionMarker(seq, target, targetIds, type, payload, Instant.now(), "node-1", createdByUser);
    }
}
