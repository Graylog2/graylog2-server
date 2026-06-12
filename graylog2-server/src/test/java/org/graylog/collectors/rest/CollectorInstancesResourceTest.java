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
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.FleetService;
import org.graylog.collectors.FleetTransactionLogService;
import org.graylog.collectors.SourceService;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.collectors.db.FleetReassignedPayload;
import org.graylog.collectors.db.MarkerType;
import org.graylog.collectors.db.TransactionMarker;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.database.filtering.ComputedFieldRegistry;
import org.graylog2.security.WithAuthorization;
import org.graylog2.security.WithAuthorizationExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    @Mock
    private ActivityEntryMapper activityEntryMapper;

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
                auditEventSender,
                activityEntryMapper
        );
    }

    @Test
    void pendingChangesReturnsCoalescedActionsAndFiltersUnknownMarkers() {
        final var instance = instance("uid-1", "fleet-1", 5L);
        when(collectorInstanceService.findByInstanceUid("uid-1")).thenReturn(Optional.of(instance));

        final var reassign = new TransactionMarker(6L, TransactionMarker.TARGET_COLLECTOR, Set.of("uid-1"),
                MarkerType.FLEET_REASSIGNED, new FleetReassignedPayload("fleet-X"), Instant.now(), "node-1", "alice");
        final var unknown = new TransactionMarker(7L, TransactionMarker.TARGET_FLEET, Set.of("fleet-1"),
                MarkerType.UNKNOWN, null, Instant.now(), "node-1", null);
        when(txnLogService.getUnprocessedMarkers("fleet-1", "uid-1", 5L)).thenReturn(List.of(reassign, unknown));
        // coalesce() is stateless and delegates to the static doCoalesce — run the real logic
        when(txnLogService.coalesce(anyList())).thenCallRealMethod();

        final var mappedEntries = List.of(new RecentActivityResponse.ActivityEntry(
                6L, Instant.now(), "FLEET_REASSIGNED", null, List.of(), null));
        when(activityEntryMapper.toEntries(anyList(), any())).thenReturn(mappedEntries);

        final var response = resource.instancePendingChanges("uid-1");

        assertThat(response.coalesced().reassignTargetFleetId()).isEqualTo("fleet-X");
        assertThat(response.coalesced().recomputeConfig()).isTrue();
        assertThat(response.coalesced().restart()).isFalse();
        assertThat(response.activities()).isEqualTo(mappedEntries);

        // The mapper must only see known marker types — UNKNOWN is filtered out
        @SuppressWarnings("unchecked") final ArgumentCaptor<List<TransactionMarker>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(activityEntryMapper).toEntries(captor.capture(), any());
        assertThat(captor.getValue()).containsExactly(reassign);
    }

    @Test
    void pendingChangesAreEmptyWhenInstanceIsCaughtUp() {
        when(collectorInstanceService.findByInstanceUid("uid-1"))
                .thenReturn(Optional.of(instance("uid-1", "fleet-1", 9L)));
        when(txnLogService.getUnprocessedMarkers("fleet-1", "uid-1", 9L)).thenReturn(List.of());
        when(txnLogService.coalesce(anyList())).thenCallRealMethod();
        when(activityEntryMapper.toEntries(anyList(), any())).thenReturn(List.of());

        final var response = resource.instancePendingChanges("uid-1");

        assertThat(response.activities()).isEmpty();
        assertThat(response.coalesced().recomputeConfig()).isFalse();
        assertThat(response.coalesced().recomputeIngestConfig()).isFalse();
        assertThat(response.coalesced().reassignTargetFleetId()).isNull();
        assertThat(response.coalesced().restart()).isFalse();
        assertThat(response.coalesced().runDiscovery()).isFalse();
    }

    @Test
    void pendingChangesForUnknownInstanceIsNotFound() {
        when(collectorInstanceService.findByInstanceUid("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resource.instancePendingChanges("missing"))
                .isInstanceOf(NotFoundException.class);

        verify(txnLogService, never()).getUnprocessedMarkers(any(), any(), anyLong());
    }

    @Test
    @WithAuthorization(username = "bob", permissions = "collectors:activitiesread")
    void pendingChangesRequireFleetReadPermission() {
        when(collectorInstanceService.findByInstanceUid("uid-1"))
                .thenReturn(Optional.of(instance("uid-1", "fleet-1", 0L)));

        assertThatThrownBy(() -> resource.instancePendingChanges("uid-1"))
                .isInstanceOf(ForbiddenException.class);

        verify(txnLogService, never()).getUnprocessedMarkers(any(), any(), anyLong());
    }

    private static CollectorInstanceDTO instance(String uid, String fleetId, long lastProcessedTxnSeq) {
        return CollectorInstanceDTO.builder()
                .instanceUid(uid)
                .fleetId(fleetId)
                .lastProcessedTxnSeq(lastProcessedTxnSeq)
                .lastSeen(Instant.now())
                .enrolledAt(Instant.EPOCH)
                .messageSeqNum(0L)
                .capabilities(0L)
                .activeCertificateFingerprint("fp-" + uid)
                .activeCertificatePem("pem")
                .activeCertificateExpiresAt(Instant.now())
                .issuingCaId("ca-1")
                .enrollmentTokenId("token-1")
                .build();
    }
}
